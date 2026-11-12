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
            List.of("1"), "EXCEPTION_EXIT", "boom").createEvent();

        assertNull(event.getExecutionTime());
        assertEquals("A", event.getClassName());
        assertEquals("run", event.getMethodName());
        assertEquals(List.of("1"), event.getParameters());
        assertEquals("EXCEPTION_EXIT", event.getTerminationType());
        assertEquals("boom", event.getExceptionMessage());
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
