package com.comp3607.telemetry.bluej;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.time.*;
import java.util.*;

import org.junit.jupiter.api.*;
import org.mockito.*;

import com.comp3607.telemetry.events.*;

import bluej.extensions2.*;
import bluej.extensions2.editor.*;
import bluej.extensions2.event.*;
import bluej.extensions2.event.ClassEvent;

import javafx.scene.control.*;

class BlueJExtensionTest {

    @Test
    void validatesIdleThresholdAndExtensionMetadata() {
        assertThrows(IllegalArgumentException.class, () -> new BlueJExtension(-1));
        BlueJExtension extension = new BlueJExtension(0);
        assertEquals("Telemetry", extension.getName());
        assertEquals("1.0.0", extension.getVersion());
        assertEquals("Collects BlueJ coding-session telemetry and exports reports.", extension.getDescription());
        assertEquals("https://www.bluej.org/extensions/writingextensions2.html",
            extension.getURL().toExternalForm());
        assertTrue(extension.isCompatible());
    }

    @Test
    void startupRegistersListenersOnceAndTerminateRemovesThem() {
        BlueJ bluej = mock(BlueJ.class);
        BlueJExtension extension = new BlueJExtension();

        extension.startup(bluej);
        extension.startup(bluej);

        verify(bluej, times(1)).setMenuGenerator(any(MenuGenerator.class));
        verify(bluej, times(1)).addPackageListener(extension);
        verify(bluej, times(1)).addCompileListener(extension);
        verify(bluej, times(1)).addInvocationFinishedListener(extension);
        verify(bluej, times(1)).addClassListener(extension);

        extension.terminate();
        verify(bluej).setMenuGenerator(null);
        verify(bluej).removePackageListener(extension);
        verify(bluej).removeCompileListener(extension);
        verify(bluej).removeInvocationFinishedListener(extension);
        verify(bluej).removeClassListener(extension);
    }

    @Test
    void terminateWithoutRegisteredListenersIsSafe() {
        BlueJExtension extension = new BlueJExtension();
        assertDoesNotThrow(extension::terminate);
    }

    @Test
    void reportsCompilationStartedWarningErrorSuccessAndFailure() {
        BlueJExtension extension = new BlueJExtension();
        CompileEvent started = mock(CompileEvent.class);
        extension.compileStarted(started);

        CompileEvent error = mock(CompileEvent.class);
        when(error.getErrorMessage()).thenReturn("Cannot find symbol");
        extension.compileError(error);
        extension.compileWarning(mock(CompileEvent.class));

        CompileEvent success = mock(CompileEvent.class);
        when(success.getFiles()).thenReturn(new File[] { null, new File("src/A.java") });
        when(success.getErrorLineNumber()).thenReturn(-1);
        when(success.getErrorPosition()).thenReturn(new int[] { 3, 8 });
        when(success.isUserGeneratedCompilation()).thenReturn(true);
        extension.compileSucceeded(success);

        CompileEvent failed = mock(CompileEvent.class);
        when(failed.getErrorMessage()).thenReturn("Syntax error");
        when(failed.getFiles()).thenReturn(new File[] { new File("src/B.java") });
        when(failed.getErrorLineNumber()).thenReturn(12);
        when(failed.getErrorPosition()).thenReturn(new int[] { 12, 1 });
        extension.compileFailed(failed);

        List<TelemetryEvent> events = extension.getSessionEventsSnapshot();
        assertEquals(2, events.size());
        CompilationEvent successful = (CompilationEvent) events.get(0);
        assertTrue(successful.getSuccessful());
        assertEquals(List.of("src\\A.java".replace('\\', File.separatorChar)), successful.getFiles());
        assertTrue(successful.isCompilationTimeAvailable());

        CompilationEvent unsuccessful = (CompilationEvent) events.get(1);
        assertFalse(unsuccessful.getSuccessful());
        assertEquals("Syntax error", unsuccessful.getErrorMessage());
        assertEquals(12, unsuccessful.getErrorLineNumber());
        assertEquals(List.of("src\\B.java".replace('\\', File.separatorChar)), unsuccessful.getFiles());
    }

    @Test
    void compilationFailureUsesPreviouslyRecordedErrorWhenEventMessageIsMissing() {
        BlueJExtension extension = new BlueJExtension();
        extension.compileStarted(mock(CompileEvent.class));
        CompileEvent error = mock(CompileEvent.class);
        when(error.getErrorMessage()).thenReturn("Recorded diagnostic");
        extension.compileError(error);

        CompileEvent failed = mock(CompileEvent.class);
        when(failed.getErrorMessage()).thenReturn(null);
        when(failed.getFiles()).thenReturn(null);
        when(failed.getErrorLineNumber()).thenReturn(-1);
        extension.compileFailed(failed);

        CompilationEvent event = (CompilationEvent) extension.getSessionEventsSnapshot().get(0);
        assertEquals("Recorded diagnostic", event.getErrorMessage());
        assertTrue(event.getFiles().isEmpty());
        assertNull(event.getErrorLineNumber());
    }

    @Test
    void recordsSuccessfulExecutionWithPopulatedMetadata() {
        BlueJExtension extension = new BlueJExtension();
        InvocationFinishedEvent invocation = mock(InvocationFinishedEvent.class);
        when(invocation.getEventType()).thenReturn(InvocationFinishedEvent.EventType.NORMAL_EXIT);
        when(invocation.getParameters()).thenReturn(new String[] {"7", "true"});
        when(invocation.getSignature()).thenReturn(new Class<?>[] {int.class, boolean.class});
        when(invocation.getClassName()).thenReturn("Calculator");
        when(invocation.getMethodName()).thenReturn("add");
        when(invocation.getObjectName()).thenReturn("calc1");

        extension.invocationFinished(invocation);

        ExecutionEvent event = (ExecutionEvent) extension.getSessionEventsSnapshot().get(0);
        assertTrue(event.getSuccessful());
        assertEquals("Calculator", event.getClassName());
        assertEquals("add", event.getMethodName());
        assertEquals("calc1", event.getObjectName());
        assertEquals(List.of("7", "true"), event.getParameters());
        assertEquals(List.of(int.class.getTypeName(), boolean.class.getTypeName()), event.getMethodSignature());
        assertEquals("NORMAL_EXIT", event.getTerminationType());
        assertNull(event.getExecutionTime());
    }

    @Test
    void recordsFailedUnknownAndNullExecutionData() {
        BlueJExtension extension = new BlueJExtension();

        InvocationFinishedEvent failed = mock(InvocationFinishedEvent.class);
        when(failed.getEventType()).thenReturn(InvocationFinishedEvent.EventType.EXCEPTION_EXIT);
        when(failed.getParameters()).thenReturn(null);
        when(failed.getSignature()).thenReturn(null);
        extension.invocationFinished(failed);

        InvocationFinishedEvent unknown = mock(InvocationFinishedEvent.class);
        when(unknown.getEventType()).thenReturn(null);
        extension.invocationFinished(unknown);

        ExecutionEvent failedEvent = (ExecutionEvent) extension.getSessionEventsSnapshot().get(0);
        assertFalse(failedEvent.getSuccessful());
        assertEquals("EXCEPTION_EXIT", failedEvent.getTerminationType());
        assertTrue(failedEvent.getParameters().isEmpty());
        assertTrue(failedEvent.getMethodSignature().isEmpty());

        ExecutionEvent unknownEvent = (ExecutionEvent) extension.getSessionEventsSnapshot().get(1);
        assertFalse(unknownEvent.getSuccessful());
        assertEquals("UNKNOWN_EXIT", unknownEvent.getTerminationType());
    }

    @Test
    void packageEventsDiscoverRenameRemoveAndStateChangeClasses() throws Exception {
        BlueJExtension extension = new BlueJExtension();
        BPackage project = mock(BPackage.class);
        BProject bluejProject = mock(BProject.class);
        when(project.getProject()).thenReturn(bluejProject);
        when(bluejProject.getName()).thenReturn("DemoProject");
        when(project.getClasses()).thenReturn(new BClass[0]);

        PackageEvent opened = mock(PackageEvent.class);
        when(opened.getPackage()).thenReturn(project);
        extension.packageOpened(opened);
        assertNotNull(extension.getSessionEventsSnapshot().get(0));

        BMethod first = mock(BMethod.class);
        when(first.toString()).thenReturn("void calculate()");
        BMethod second = mock(BMethod.class);
        when(second.toString()).thenReturn("int getValue()");
        BClass bClass = mock(BClass.class);
        when(bClass.getName()).thenReturn("Calculator");
        when(bClass.getDeclaredMethods()).thenReturn(new BMethod[] {first, second});
        when(bClass.isCompiled()).thenReturn(true);
        when(bClass.getJavaEditorIfOpen()).thenReturn(null);

        ClassEvent state = mock(ClassEvent.class);
        when(state.getBClass()).thenReturn(bClass);
        when(state.isClassCompiled()).thenReturn(true);
        extension.classStateChanged(state);

        when(bClass.getDeclaredMethods()).thenReturn(new BMethod[] {second});
        extension.classStateChanged(state);

        when(bClass.getName()).thenReturn("RenamedCalculator");
        ClassEvent renamed = mock(ClassEvent.class);
        when(renamed.getBClass()).thenReturn(bClass);
        when(renamed.getOldName()).thenReturn("Calculator");
        extension.classNameChanged(renamed);

        ClassEvent removed = mock(ClassEvent.class);
        when(removed.getBClass()).thenReturn(bClass);
        extension.classRemoved(removed);

        PackageEvent closing = mock(PackageEvent.class);
        when(closing.getPackage()).thenReturn(project);
        extension.packageClosing(closing);

        List<TelemetryEvent> events = extension.getSessionEventsSnapshot();
        assertTrue(events.stream().anyMatch(e -> e instanceof ProjectEvent));
        assertTrue(events.stream().anyMatch(e -> e instanceof com.comp3607.telemetry.events.ClassEvent));
        assertTrue(events.stream().anyMatch(e -> e instanceof MethodEvent));
    }

    @Test
    void attachesEditorsOnceDetachesEditorAndDetachesAll() throws Exception {
        BlueJExtension extension = new BlueJExtension();
        BlueJ bluej = mock(BlueJ.class);
        extension.startup(bluej);

        JavaEditor editor = mock(JavaEditor.class);
        BClass bClass = mock(BClass.class);
        when(bClass.getName()).thenReturn("Student");
        when(bClass.getDeclaredMethods()).thenReturn(new BMethod[0]);
        when(bClass.getJavaEditorIfOpen()).thenReturn(editor);

        ClassEvent state = mock(ClassEvent.class);
        when(state.getBClass()).thenReturn(bClass);
        extension.classStateChanged(state);
        extension.classStateChanged(state);

        verify(editor, times(1)).addDocumentListener(any());

        BPackage project = mock(BPackage.class);
        when(project.getClasses()).thenReturn(new BClass[] {bClass});
        BProject bluejProject = mock(BProject.class);
        when(bluejProject.getName()).thenReturn("Demo");
        when(project.getProject()).thenReturn(bluejProject);
        PackageEvent closing = mock(PackageEvent.class);
        when(closing.getPackage()).thenReturn(project);
        extension.packageClosing(closing);
        verify(editor, times(1)).removeDocumentListener(any());

        extension.classStateChanged(state);
        extension.terminate();
        verify(editor, times(2)).removeDocumentListener(any());
    }

    @Test
    void editorAttachmentHandlesUnavailableEditorsAndRuntimeExceptions() throws Exception {
        BlueJExtension extension = new BlueJExtension();
        BClass unavailable = mock(BClass.class);
        when(unavailable.getName()).thenReturn("Unavailable");
        when(unavailable.getDeclaredMethods()).thenReturn(new BMethod[0]);
        when(unavailable.getJavaEditorIfOpen()).thenReturn(null);

        BClass failing = mock(BClass.class);
        when(failing.getName()).thenReturn("Failing");
        when(failing.getDeclaredMethods()).thenReturn(new BMethod[0]);
        when(failing.getJavaEditorIfOpen()).thenThrow(new RuntimeException("editor unavailable"));

        for (BClass bClass : new BClass[] {unavailable, failing}) {
            ClassEvent event = mock(ClassEvent.class);
            when(event.getBClass()).thenReturn(bClass);
            assertDoesNotThrow(() -> extension.classStateChanged(event));
        }
    }

    @Test
    void menuGeneratorCreatesTelemetryMenuAndExpectedItems() {
        BlueJ bluej = mock(BlueJ.class);
        BlueJExtension extension = new BlueJExtension();
        extension.startup(bluej);

        ArgumentCaptor<MenuGenerator> captor = ArgumentCaptor.forClass(MenuGenerator.class);
        verify(bluej).setMenuGenerator(captor.capture());
        Menu menu = (Menu) captor.getValue().getToolsMenuItem(mock(BPackage.class));

        assertEquals("Telemetry Reports", menu.getText());
        List<String> labels = menu.getItems().stream().map(MenuItem::getText).toList();
        assertEquals(List.of("Export JSON Report", "Export PDF Report"), labels);

        extension.terminate();
    }

    @Test
    void missingSessionStartAndEmptyEventsPreventAutomaticExport() {
        BlueJExtension extension = new BlueJExtension();
        assertDoesNotThrow(extension::exportSessionReport);
        LocalDateTime start = LocalDateTime.now();
        extension.setSessionStartTime(start);
        assertDoesNotThrow(extension::exportSessionReport);
        assertEquals(start, extension.getSessionStartTime());
    }

    @Test
    void existingJsonAndPdfExtensionsAreNotDuplicated(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)
        throws Exception {
        BlueJExtension extension = new BlueJExtension();
        extension.setSessionStartTime(LocalDateTime.now());
        assertTrue(extension.exportJsonReport(dir.resolve("report.JSON")).getFileName().toString().endsWith("JSON"));
        assertTrue(extension.exportPdfReport(dir.resolve("report.PDF")).getFileName().toString().endsWith("PDF"));
    }
}
