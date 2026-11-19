package com.comp3607.telemetry.events;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.comp3607.telemetry.factory.*;

class ExtendedEventTest {
    @Test
    void preservesCompilationDiagnosticsAndFiles() {
        CompilationEvent event = new CompilationEventFactory(false, null, "Syntax error",
            12, new int[] { 4, 9 }, List.of("A.java"), true).createEvent();

        assertFalse(event.isCompilationTimeAvailable());
        assertEquals(12, event.getErrorLineNumber());
        assertArrayEquals(new int[] { 4, 9 }, event.getErrorPosition());
        assertEquals(List.of("A.java"), event.getFiles());
        assertTrue(event.isUserGeneratedCompilation());
    }

    @Test
    void preservesExecutionMetadataAndUnknownDuration() {
        ExecutionEvent event = new ExecutionEventFactory(false, null, "A", "run", "a",
            List.of("1"), List.of("java.lang.String"), "EXCEPTION_EXIT", null).createEvent();

        assertNull(event.getExecutionTime());
        assertEquals("A", event.getClassName());
        assertEquals("run", event.getMethodName());
        assertEquals("a", event.getObjectName());
        assertEquals(List.of("1"), event.getParameters());
        assertEquals(List.of("java.lang.String"), event.getMethodSignature());
        assertEquals("EXCEPTION_EXIT", event.getTerminationType());
        assertNull(event.getExceptionMessage());
    }

    @Test
    void preservesSuccessfulStaticAndConstructorExecutions() {
        ExecutionEvent staticCall = new ExecutionEventFactory(true, null, "TelemetryTest", "main",
            null, List.of(), List.of("[Ljava.lang.String;", "java.lang.String"), "NORMAL_EXIT", null)
            .createEvent();
        ExecutionEvent constructor = new ExecutionEventFactory(true, null, "TelemetryTest", null,
            "telemetr1", List.of("7"), List.of("int"), "NORMAL_EXIT", null).createEvent();

        assertTrue(staticCall.getSuccessful());
        assertEquals("TelemetryTest", staticCall.getClassName());
        assertEquals("main", staticCall.getMethodName());
        assertNull(staticCall.getObjectName());
        assertEquals(List.of("[Ljava.lang.String;", "java.lang.String"),
            staticCall.getMethodSignature());
        assertEquals("NORMAL_EXIT", staticCall.getTerminationType());
        assertNull(staticCall.getExecutionTime());
        assertNull(staticCall.getExceptionMessage());

        assertTrue(constructor.getSuccessful());
        assertNull(constructor.getMethodName());
        assertEquals("telemetr1", constructor.getObjectName());
        assertEquals(List.of("7"), constructor.getParameters());
    }

    @Test
    void recordsClassLifecycleAndState() {
        ClassEvent event = new ClassEventFactory("renamed", "NewName", "OldName",
            true, false).createEvent();

        assertEquals("CLASS", event.getEventType());
        assertEquals("renamed", event.getAction());
        assertEquals("OldName", event.getOldName());
        assertTrue(event.isCompiled());
    }
}
