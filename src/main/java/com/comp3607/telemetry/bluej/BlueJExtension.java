package com.comp3607.telemetry.bluej;

import java.net.URL;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import com.comp3607.telemetry.report.SessionReport;
import com.comp3607.telemetry.events.MethodEvent;
import com.comp3607.telemetry.events.EditorEvent;
import com.comp3607.telemetry.report.PdfReportGenerator;
import com.comp3607.telemetry.report.SessionJsonReportGenerator;

import com.comp3607.telemetry.strategy.CsvExportStrategy;
import com.comp3607.telemetry.strategy.EventExporter;
import com.comp3607.telemetry.strategy.ExportStrategy;
import com.comp3607.telemetry.editor.EditorActivityTracker;
import com.comp3607.telemetry.factory.*;
import com.comp3607.telemetry.observer.*;
import com.comp3607.telemetry.statistics.CompilationStatistics;

import bluej.extensions2.*;
import bluej.extensions2.editor.*;
import bluej.extensions2.event.*;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class BlueJExtension extends Extension
        implements PackageListener, CompileListener, InvocationFinishedListener, ClassListener {

    private static final Logger LOGGER =Logger.getLogger(BlueJExtension.class.getName());
    private final ObserverSubject telemetrySubject = new ObserverSubject();
    private final ObserverManager observerManager = new ObserverManager();
    private final EventExporter eventExporter = new EventExporter(new CsvExportStrategy());
    private final CompilationStatistics compilationStatistics = new CompilationStatistics();
    private BlueJ blueJ;
    private boolean listenersRegistered;
    private final Map<JavaEditor, EditorRegistration> editorTrackers =Collections.synchronizedMap(new IdentityHashMap<>());
    private final Map<String, Set<String>> methodSignaturesByClass = new HashMap<>();
    private final long idleThresholdMillis;
    private EditorActivityTracker editorActivityTracker;
    private ScheduledExecutorService editorScanExecutor;
    private ScheduledFuture<?> editorScanFuture;
    private volatile BPackage scannedProject;
    private long compilationStartNanos = -1;
    private String lastCompilationError;
    private LocalDateTime sessionStart;
    private String currentProjectName = "BlueJ Project";
    
    private final ExecutorService reportExecutor =
        Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "bluej-telemetry-report");
            thread.setDaemon(true);
            return thread;
        });

    public BlueJExtension() {
        this(30_000);
    }

    public BlueJExtension(long idleThresholdMillis) {
        if (idleThresholdMillis < 0) {
            throw new IllegalArgumentException("Idle threshold cannot be negative");
        }
        this.idleThresholdMillis = idleThresholdMillis;
        this.editorActivityTracker = new EditorActivityTracker(idleThresholdMillis);
        telemetrySubject.addObserver(observerManager);
        telemetrySubject.addObserver(compilationStatistics);
    }

        
    public void setExportStrategy(ExportStrategy strategy) {
        eventExporter.setStrategy(strategy);
    }

    
    public Path exportEvents(Path directory, String fileNameWithoutExtension) throws IOException {
        return eventExporter.exportToFile(observerManager.getEvents(), directory, fileNameWithoutExtension);
    }
    @Override
    public synchronized void startup(BlueJ bluej) {
        if (listenersRegistered) {
            return;
        }

        this.blueJ = bluej;
        this.sessionStart = LocalDateTime.now();
        this.editorActivityTracker = new EditorActivityTracker(idleThresholdMillis);
        bluej.setMenuGenerator(new TelemetryMenuGenerator());
        bluej.addPackageListener(this);
        bluej.addCompileListener(this);
        bluej.addInvocationFinishedListener(this);
        bluej.addClassListener(this);
        listenersRegistered = true;
    }

    @Override
    public void terminate() {
        EditorEvent finalEvent = editorActivityTracker.endSession(java.time.Instant.now());
        if (finalEvent != null) {
            publish(finalEvent);
        }
        stopEditorPolling();
        if (blueJ != null) {
            blueJ.setMenuGenerator(null);
            blueJ.removePackageListener(this);
            blueJ.removeCompileListener(this);
            blueJ.removeInvocationFinishedListener(this);
            blueJ.removeClassListener(this);
            detachAllEditors();
            blueJ = null;
            listenersRegistered = false;
        }
        exportSessionReport();
        reportExecutor.shutdown();
    }

    private final class TelemetryMenuGenerator extends MenuGenerator {
        @Override
        public MenuItem getToolsMenuItem(BPackage project) {
            Menu menu = new Menu("Telemetry Reports");

            MenuItem jsonItem = new MenuItem("Export JSON Report");
            jsonItem.setOnAction(event -> chooseAndExportJson());

            MenuItem pdfItem = new MenuItem("Export PDF Report");
            pdfItem.setOnAction(event -> chooseAndExportPdf());

            menu.getItems().setAll(jsonItem, pdfItem);
            return menu;
        }
    }

    private void chooseAndExportJson() {
        FileChooser chooser = createSaveChooser("JSON Report", "telemetry-report.json", "*.json");
        Window owner = blueJ == null ? null : blueJ.getCurrentWindow();
        File selected = chooser.showSaveDialog(owner);
        if (selected == null) {
            return;
        }
        Path destination = ensureJsonExtension(selected.toPath());
        SessionReport snapshot = buildSessionSnapshot();
        reportExecutor.execute(() -> {
            try {
                Path saved = new SessionJsonReportGenerator().generate(snapshot, destination);
                showInformation("JSON report saved", "Saved JSON report to:\n" + saved.toAbsolutePath());
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Unable to manually export JSON report", exception);
                showError("Unable to export JSON report",
                    "The JSON report could not be saved:\n" + exception.getMessage());
            }
        });
    }

    private void chooseAndExportPdf() {
        FileChooser chooser = createSaveChooser("PDF Report", "telemetry-report.pdf", "*.pdf");
        Window owner = blueJ == null ? null : blueJ.getCurrentWindow();
        File selected = chooser.showSaveDialog(owner);
        if (selected == null) {
            return;
        }
        Path destination = ensurePdfExtension(selected.toPath());
        SessionReport snapshot = buildSessionSnapshot();
        reportExecutor.execute(() -> {
            try {
                Path saved = new PdfReportGenerator().generate(snapshot, destination);
                showInformation("PDF report saved", "Saved PDF report to:\n" + saved.toAbsolutePath());
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Unable to manually export PDF report", exception);
                showError("Unable to export PDF report",
                    "The PDF report could not be saved:\n" + exception.getMessage());
            }
        });
    }

    private FileChooser createSaveChooser(String description, String initialName, String glob) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Telemetry Reports");
        chooser.setInitialFileName(initialName);
        Path outputDirectory = defaultOutputDirectory();
        try {
            java.nio.file.Files.createDirectories(outputDirectory);
            if (java.nio.file.Files.isDirectory(outputDirectory)) {
                chooser.setInitialDirectory(outputDirectory.toFile());
            }
        } catch (IOException exception) {
            LOGGER.log(Level.FINE, "Unable to prepare telemetry output directory", exception);
        }
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(description, glob));
        return chooser;
    }

    private void showInformation(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        });
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(title);
            alert.setContentText(message);
            alert.show();
        });
    }

    private SessionReport buildSessionSnapshot() {
        LocalDateTime start = sessionStart;
        if (start == null) {
            start = LocalDateTime.now();
        }
        return new SessionReport(currentProjectName, start, LocalDateTime.now(),
            observerManager.getEvents());
    }

    Path exportJsonReport(Path destination) throws IOException {
        if (destination == null) {
            return null;
        }
        return new SessionJsonReportGenerator().generate(buildSessionSnapshot(), ensureJsonExtension(destination));
    }

    Path exportPdfReport(Path destination) throws IOException {
        if (destination == null) {
            return null;
        }
        return new PdfReportGenerator().generate(buildSessionSnapshot(), ensurePdfExtension(destination));
    }

    private static Path ensureJsonExtension(Path destination) {
        String name = destination.getFileName().toString();
        return name.toLowerCase(java.util.Locale.ROOT).endsWith(".json")
            ? destination
            : destination.resolveSibling(name + ".json");
    }

    private static Path ensurePdfExtension(Path destination) {
        String name = destination.getFileName().toString();
        return name.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")
            ? destination
            : destination.resolveSibling(name + ".pdf");
    }

    private Path defaultOutputDirectory() {
        return Path.of(System.getProperty("bluej.telemetry.output",
            Path.of(System.getProperty("user.home"), "bluej-telemetry").toString()));
    }

    void exportSessionReport() {
        List<com.comp3607.telemetry.events.TelemetryEvent> events = observerManager.getEvents();
        if (events.isEmpty() || sessionStart == null) {
            return;
        }
        try {
            LocalDateTime end = LocalDateTime.now();
            SessionReport report = new SessionReport(currentProjectName, sessionStart, end, events);
            Path directory = Path.of(System.getProperty("bluej.telemetry.output",
                Path.of(System.getProperty("user.home"), "bluej-telemetry").toString()));
            String stamp = end.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
            String base = "telemetry-" + safeFilePart(currentProjectName) + "-" + stamp;
            new SessionJsonReportGenerator().generate(report, directory.resolve(base + ".json"));
            new PdfReportGenerator().generate(report, directory.resolve(base + ".pdf"));
            LOGGER.info("Telemetry reports saved to " + directory.toAbsolutePath());
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Unable to generate telemetry session reports", exception);
        } finally {
            sessionStart = null;
        }
    }

    private static String safeFilePart(String value) {
        String safe = value == null ? "project" : value.replaceAll("[^A-Za-z0-9_-]", "_");
        return safe.isBlank() ? "project" : safe;
    }

    @Override
    public boolean isCompatible() {
        return getExtensionsAPIVersionMajor() >= 3;
    }

    @Override
    public String getName() {
        return "Telemetry";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Collects BlueJ coding-session telemetry and exports reports.";
    }

    @Override
    public URL getURL() {
        try {
            return new URL("https://www.bluej.org/extensions/writingextensions2.html");
        } catch (java.net.MalformedURLException exception) {
            return null;
        }
    }

    @Override
    public void packageOpened(PackageEvent event) {
        currentProjectName = projectName(event);
        publish(new ProjectEventFactory("opened", currentProjectName));
        attachEditors(event);
        startEditorPolling(event.getPackage());
    }

    @Override
    public void packageClosing(PackageEvent event) {
        publish(new ProjectEventFactory("closing", projectName(event)));
        stopEditorPolling();
        detachEditors(event);
    }
 
    @Override
    public synchronized void compileStarted(CompileEvent event) {
        compilationStartNanos = System.nanoTime();
        lastCompilationError = null;

        LOGGER.info("Telemetry: Compilation started.");
    }

    @Override
    public synchronized void compileError(CompileEvent event) {
        lastCompilationError = event.getErrorMessage();
    }

    @Override
    public void compileWarning(CompileEvent event) {
        // Warnings do not represent a completed compilation attempt.
    }

    @Override
    public void compileSucceeded(CompileEvent event) {
        publishCompilation(true, event);
    }

    @Override
    public void compileFailed(CompileEvent event) {
        publishCompilation(false, event);
    }

    @Override
    public void invocationFinished(InvocationFinishedEvent event) {
        InvocationFinishedEvent.EventType eventType = event.getEventType();
        if (eventType == null) {
            eventType = InvocationFinishedEvent.EventType.UNKNOWN_EXIT;
        }
        boolean successful = eventType == InvocationFinishedEvent.EventType.NORMAL_EXIT;

        // getResult() is the invoked method's return value (or null for void) and is
        // therefore not a reliable source of exception details.
        String exceptionMessage = null;
        String[] suppliedParameters = event.getParameters();
        List<String> parameters;
        if (suppliedParameters == null) {
            parameters = List.of();
        } else {
            parameters = Arrays.asList(suppliedParameters.clone());
        }
        Class<?>[] suppliedSignature = event.getSignature();
        List<String> methodSignature;
        if (suppliedSignature == null) {
            methodSignature = List.of();
        } else {
            methodSignature = Arrays.stream(suppliedSignature)
                .map(type -> type == null ? "" : type.getTypeName())
                .toList();
        }
        publish(new ExecutionEventFactory(successful, (Long) null,
            event.getClassName(), event.getMethodName(), event.getObjectName(), parameters,
            methodSignature, eventType.name(), exceptionMessage));
    }

    private synchronized void publishCompilation(boolean successful, CompileEvent event) {
        Long compilationTime = null;
        if (compilationStartNanos >= 0) {
            compilationTime = (System.nanoTime() - compilationStartNanos) / 1_000_000;
        }

        String errorMessage = null;
        if (!successful) {
            errorMessage = event.getErrorMessage();
        }
        if (errorMessage == null) {
            errorMessage = lastCompilationError;
        }

        File[] sourceFiles = event.getFiles();
        List<String> files = new ArrayList<>();
        if (sourceFiles != null) {
            for (File file : sourceFiles) {
                if (file != null) {
                    files.add(file.getPath());
                }
            }
        }
        Integer line = null;
        if (event.getErrorLineNumber() >= 0) {
            line = event.getErrorLineNumber();
        }
        publish(new CompilationEventFactory(successful, compilationTime, errorMessage, line,event.getErrorPosition(), files, event.isUserGeneratedCompilation()));
        compilationStartNanos = -1;
        lastCompilationError = null;
    }

    private void publish(com.comp3607.telemetry.factory.TelemetryEventFactory factory) {
        telemetrySubject.notifyObservers(factory.createEvent());
    }

    private void publish(com.comp3607.telemetry.events.TelemetryEvent event) {
        telemetrySubject.notifyObservers(event);
    }

    LocalDateTime getSessionStartTime() {
        return sessionStart;
    }

    void setSessionStartTime(LocalDateTime startTime) {
        sessionStart = startTime;
    }

    List<com.comp3607.telemetry.events.TelemetryEvent> getSessionEventsSnapshot() {
        return observerManager.getEvents();
    }

    void publishSessionEvent(com.comp3607.telemetry.events.TelemetryEvent event) {
        publish(event);
    }

    @Override
    public void classNameChanged(ClassEvent event) {
        BClass bClass = event.getBClass();
        String newName = bClass.getName();
        synchronized (methodSignaturesByClass) {
            Set<String> previous = methodSignaturesByClass.remove(event.getOldName());
            if (previous != null) {
                methodSignaturesByClass.put(newName, previous);
            }
        }
        publish(new ClassEventFactory("renamed", newName, event.getOldName(),
            event.isClassCompiled(), event.hasError()));
        trackMethods(bClass);
        attachEditor(bClass);
    }

    @Override
    public void classRemoved(ClassEvent event) {
        String className = event.getBClass().getName();
        publish(new ClassEventFactory("removed", className, null,
            event.isClassCompiled(), event.hasError()));
        Set<String> previous;
        synchronized (methodSignaturesByClass) {
            previous = methodSignaturesByClass.remove(className);
        }
        if (previous != null) {
            for (String signature : previous) {
                publish(new MethodEvent("removed", className, signature));
            }
        }
    }

    @Override
    public void classStateChanged(ClassEvent event) {
        publish(new ClassEventFactory("stateChanged", event.getBClass().getName(), null,
            event.isClassCompiled(), event.hasError()));
        trackMethods(event.getBClass());
        attachEditor(event.getBClass());
    }

    private void attachEditors(PackageEvent event) {
        try {
            for (BClass bClass : event.getPackage().getClasses()) {
                publish(new ClassEventFactory("opened", bClass.getName(), null,
                    bClass.isCompiled(), false));
                trackMethods(bClass);
                attachEditor(bClass);
            }
        } catch (ExtensionException exception) {
            LOGGER.log(Level.FINE, "Unable to inspect package editors", exception);
        }
    }

    /**
     * Compares the class's declared method signatures with the previous snapshot.
     * This detects method additions and removals after a successful BlueJ class update.
     */
    private void trackMethods(BClass bClass) {
        try {
            String className = bClass.getName();
            Set<String> current = new LinkedHashSet<>();
            BMethod[] methods = bClass.getDeclaredMethods();
            if (methods != null) {
                for (BMethod method : methods) {
                    if (method != null) {
                        current.add(method.toString());
                    }
                }
            }

            Set<String> previous;
            synchronized (methodSignaturesByClass) {
                previous = methodSignaturesByClass.put(className, current);
            }
            if (previous == null) {
                for (String signature : current) {
                    publish(new MethodEvent("discovered", className, signature));
                }
                return;
            }
            for (String signature : current) {
                if (!previous.contains(signature)) {
                    publish(new MethodEvent("added", className, signature));
                }
            }
            for (String signature : previous) {
                if (!current.contains(signature)) {
                    publish(new MethodEvent("removed", className, signature));
                }
            }
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Unable to inspect declared methods for telemetry", exception);
        }
    }

    private void attachEditor(BClass bClass) {
        try {
            JavaEditor editor = bClass.getJavaEditorIfOpen();
            if (editor == null) {
                return;
            }
            synchronized (editorTrackers) {
                if (editorTrackers.containsKey(editor)) {
                    return;
                }
                DocumentListener listener = (length, oldText, newText, start, end) ->
                    publish(editorActivityTracker.recordEdit(java.time.Instant.now()));
                editor.addDocumentListener(listener);
                editorTrackers.put(editor, new EditorRegistration(listener));
            }
        } catch (ExtensionException | RuntimeException exception) {
            LOGGER.log(Level.FINE, "Unable to attach editor telemetry", exception);
        }
    }

    private synchronized void startEditorPolling(BPackage project) {
        stopEditorPolling();
        scannedProject = project;
        editorScanExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "bluej-telemetry-editor-scan");
            thread.setDaemon(true);
            return thread;
        });
        editorScanFuture = editorScanExecutor.scheduleWithFixedDelay(() -> {
            try {
                BPackage current = scannedProject;
                if (current == null) {
                    return;
                }
                for (BClass bClass : current.getClasses()) {
                    attachEditor(bClass);
                }
            } catch (ExtensionException exception) {
                LOGGER.log(Level.FINE, "Unable to scan for newly opened editors", exception);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private synchronized void stopEditorPolling() {
        scannedProject = null;
        if (editorScanFuture != null) {
            editorScanFuture.cancel(true);
            editorScanFuture = null;
        }
        if (editorScanExecutor != null) {
            editorScanExecutor.shutdownNow();
            editorScanExecutor = null;
        }
    }

    private void detachEditors(PackageEvent event) {
        try {
            for (BClass bClass : event.getPackage().getClasses()) {
                JavaEditor editor = bClass.getJavaEditorIfOpen();
                if (editor != null) {
                    detachEditor(editor);
                }
            }
        } catch (ExtensionException exception) {
            LOGGER.log(Level.FINE, "Unable to detach package editor telemetry", exception);
        }
    }

    private void detachAllEditors() {
        synchronized (editorTrackers) {
            for (Map.Entry<JavaEditor, EditorRegistration> entry : editorTrackers.entrySet()) {
                entry.getKey().removeDocumentListener(entry.getValue().listener());
            }
            editorTrackers.clear();
        }
    }

    private void detachEditor(JavaEditor editor) {
        synchronized (editorTrackers) {
            EditorRegistration registration = editorTrackers.remove(editor);
            if (registration != null) {
                editor.removeDocumentListener(registration.listener());
            }
        }
    }

    private static final class EditorRegistration {
        private final DocumentListener listener;

        private EditorRegistration(DocumentListener listener) {
            this.listener = listener;
        }

        private DocumentListener listener() {
            return listener;
        }
    }

    private String projectName(PackageEvent event) {
        try {
            return event.getPackage().getProject().getName();
        } catch (ExtensionException exception) {
            logProjectNameFailure(exception);
            return "unknown";
        }
    }

    private void logProjectNameFailure(ExtensionException exception) {
        try {
            LOGGER.log(Level.WARNING, "Unable to retrieve the BlueJ project name", exception);
        } catch (RuntimeException ignored) {
            // Logging must not interfere with BlueJ callbacks.
        }
    }
}
