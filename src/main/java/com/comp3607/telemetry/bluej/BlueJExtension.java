package com.comp3607.telemetry.bluej;

import java.net.URL;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.comp3607.telemetry.editor.EditorActivityTracker;
import com.comp3607.telemetry.factory.*;
import com.comp3607.telemetry.observer.*;
import com.comp3607.telemetry.statistics.CompilationStatistics;

import bluej.extensions2.*;
import bluej.extensions2.editor.*;
import bluej.extensions2.event.*;

public class BlueJExtension extends Extension
        implements PackageListener, CompileListener, InvocationFinishedListener, ClassListener {

    private static final Logger LOGGER =Logger.getLogger(BlueJExtension.class.getName());
    private final ObserverSubject telemetrySubject = new ObserverSubject();
    private final ObserverManager observerManager = new ObserverManager();
    private final CompilationStatistics compilationStatistics = new CompilationStatistics();
    private BlueJ blueJ;
    private boolean listenersRegistered;
    private final Map<JavaEditor, EditorRegistration> editorTrackers =Collections.synchronizedMap(new IdentityHashMap<>());
    private final long idleThresholdMillis;
    private long compilationStartNanos = -1;
    private String lastCompilationError;

    public BlueJExtension() {
        this(30_000);
    }

    public BlueJExtension(long idleThresholdMillis) {
        if (idleThresholdMillis < 0) {
            throw new IllegalArgumentException("Idle threshold cannot be negative");
        }
        this.idleThresholdMillis = idleThresholdMillis;
        telemetrySubject.addObserver(observerManager);
        telemetrySubject.addObserver(compilationStatistics);
    }

    @Override
    public synchronized void startup(BlueJ bluej) {
        if (listenersRegistered) {
            return;
        }

        this.blueJ = bluej;
        bluej.addPackageListener(this);
        bluej.addCompileListener(this);
        bluej.addInvocationFinishedListener(this);
        bluej.addClassListener(this);
        listenersRegistered = true;
    }

    @Override
    public void terminate() {
        if (blueJ != null) {
            blueJ.removePackageListener(this);
            blueJ.removeCompileListener(this);
            blueJ.removeInvocationFinishedListener(this);
            blueJ.removeClassListener(this);
            detachAllEditors();
            blueJ = null;
            listenersRegistered = false;
        }
    }

    @Override
    public boolean isCompatible() {
        return getExtensionsAPIVersionMajor() >= 3;
    }

    @Override
    public String getName() {
        return "BlueJ Telemetry";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Collects BlueJ project, compilation, and invocation telemetry.";
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
        publish(new ProjectEventFactory("opened", projectName(event)));
        attachEditors(event);
    }

    @Override
    public void packageClosing(PackageEvent event) {
        publish(new ProjectEventFactory("closing", projectName(event)));
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
        boolean successful = event.getEventType()== InvocationFinishedEvent.EventType.NORMAL_EXIT;

        Object result = event.getResult();
        String exceptionMessage = null;
        if (result instanceof Throwable) {
            exceptionMessage = ((Throwable) result).getMessage();
        }
        String[] suppliedParameters = event.getParameters();
        List<String> parameters;
        if (suppliedParameters == null) {
            parameters = List.of();
        } else {
            parameters = Arrays.asList(suppliedParameters.clone());
        }
        publish(new ExecutionEventFactory(successful, (Long) null,
            event.getClassName(), event.getMethodName(), event.getObjectName(), parameters,
            event.getEventType().name(), exceptionMessage));
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

    @Override
    public void classNameChanged(ClassEvent event) {
        BClass bClass = event.getBClass();
        publish(new ClassEventFactory("renamed", bClass.getName(), event.getOldName(),event.isClassCompiled(), event.hasError()));
        attachEditor(bClass);
    }

    @Override
    public void classRemoved(ClassEvent event) {
        publish(new ClassEventFactory("removed", event.getBClass().getName(), null,
            event.isClassCompiled(), event.hasError()));
    }

    @Override
    public void classStateChanged(ClassEvent event) {
        publish(new ClassEventFactory("stateChanged", event.getBClass().getName(), null,
            event.isClassCompiled(), event.hasError()));
        attachEditor(event.getBClass());
    }

    private void attachEditors(PackageEvent event) {
        try {
            for (BClass bClass : event.getPackage().getClasses()) {
                attachEditor(bClass);
            }
        } catch (ExtensionException exception) {
            LOGGER.log(Level.FINE, "Unable to inspect package editors", exception);
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
                EditorActivityTracker tracker = new EditorActivityTracker(idleThresholdMillis);
                DocumentListener listener = (length, oldText, newText, start, end) ->
                    publish(tracker.recordEdit(java.time.Instant.now()));
                editorTrackers.put(editor, new EditorRegistration(listener));
                editor.addDocumentListener(listener);
            }
        } catch (ExtensionException exception) {
            LOGGER.log(Level.FINE, "Unable to attach editor telemetry", exception);
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
