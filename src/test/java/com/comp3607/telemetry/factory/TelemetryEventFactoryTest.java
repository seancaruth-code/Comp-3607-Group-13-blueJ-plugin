package com.comp3607.telemetry.factory;

import org.junit.jupiter.api.Test;

import com.comp3607.telemetry.events.*;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryEventFactoryTest {

    @Test
    void compilationFactoryCreatesEventWithSuppliedProperties() {
        TelemetryEventFactory factory =
            new CompilationEventFactory(false, 350, "Missing semicolon");

        TelemetryEvent event = factory.createEvent();

        CompilationEvent compilationEvent = assertInstanceOf(CompilationEvent.class, event);
        assertFalse(compilationEvent.getSuccessful());
        assertEquals(350, compilationEvent.getCompilationTime());
        assertEquals("Missing semicolon", compilationEvent.getErrorMessage());
    }

    @Test
    void projectFactoryCreatesEventWithSuppliedProperties() {
        TelemetryEventFactory factory = new ProjectEventFactory("CREATE", "Student.java");

        TelemetryEvent event = factory.createEvent();

        ProjectEvent projectEvent = assertInstanceOf(ProjectEvent.class, event);
        assertEquals("CREATE", projectEvent.getAction());
        assertEquals("Student.java", projectEvent.getItemName());
    }

    @Test
    void editorFactoryCreatesEventWithSuppliedProperties() {
        TelemetryEventFactory factory = new EditorEventFactory("INACTIVE", 30000);

        TelemetryEvent event = factory.createEvent();

        EditorEvent editorEvent = assertInstanceOf(EditorEvent.class, event);
        assertEquals("INACTIVE", editorEvent.getActivityType());
        assertEquals(30000, editorEvent.getDuration());
    }

    @Test
    void executionFactoryCreatesEventWithSuppliedProperties() {
        TelemetryEventFactory factory = new ExecutionEventFactory(true, 500);

        TelemetryEvent event = factory.createEvent();

        ExecutionEvent executionEvent = assertInstanceOf(ExecutionEvent.class, event);
        assertTrue(executionEvent.getSuccessful());
        assertEquals(500, executionEvent.getExecutionTime());
        assertTrue(executionEvent.isExecutionTimeAvailable());
    }

    @Test
    void executionFactoryCreatesEventWithUnavailableDuration() {
        TelemetryEventFactory factory = new ExecutionEventFactory(true, (Long) null);

        ExecutionEvent executionEvent =
            assertInstanceOf(ExecutionEvent.class, factory.createEvent());

        assertTrue(executionEvent.getSuccessful());
        assertNull(executionEvent.getExecutionTime());
        assertFalse(executionEvent.isExecutionTimeAvailable());
    }

    @Test
    void factoriesCreateFreshEvents() {
        CompilationEventFactory factory = new CompilationEventFactory(true, 250, null);

        assertFalse(factory.createEvent() == factory.createEvent());
    }

    @Test
    void invalidProjectTextIsRejected() {
        assertThrows(NullPointerException.class, () -> new ProjectEventFactory(null, "Student.java"));
        assertThrows(NullPointerException.class, () -> new ProjectEventFactory("CREATE", null));
    }

    @Test
    void invalidEditorTextAndDurationAreRejected() {
        assertThrows(NullPointerException.class, () -> new EditorEventFactory(null, 10));
        assertThrows(IllegalArgumentException.class, () -> new EditorEventFactory("ACTIVE", -1));
    }

    @Test
    void invalidCompilationAndExecutionTimesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new CompilationEventFactory(true, -1, null));
        assertThrows(IllegalArgumentException.class, () -> new ExecutionEventFactory(true, -1));
    }
}
