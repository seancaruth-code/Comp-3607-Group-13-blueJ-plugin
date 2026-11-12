
package com.comp3607.telemetry.events;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    @Test
    void testEventCreation() {


        TelemetryEvent event = new TelemetryEvent("COMPILATION","Java class compiled successfully");

        assertNotNull(event.getEventId());

        assertEquals("COMPILATION",event.getEventType());

        assertEquals("Java class compiled successfully",event.getDescription());

        assertNotNull(event.getTimestamp());
    }
    
@Test
void testCompilationEvent() {

    CompilationEvent event = new CompilationEvent(false,350,"Missing semicolon");

    assertFalse(event.getSuccessful());
    assertEquals(350, event.getCompilationTime());
    assertEquals("Missing semicolon", event.getErrorMessage());
    assertEquals("COMPILATION", event.getEventType());
}

@Test
void testProjectEvent() {

    ProjectEvent event = new ProjectEvent("CREATE","Student.java");

    assertEquals("CREATE", event.getAction());
    assertEquals("Student.java", event.getItemName());
    assertEquals("PROJECT", event.getEventType());
}

@Test
void testEditorEvent() {


    EditorEvent event = new EditorEvent("INACTIVE",30000);

    assertEquals("INACTIVE", event.getActivityType());
    assertEquals(30000, event.getDuration());
}

@Test
void testExecutionEvent() {

    ExecutionEvent event = new ExecutionEvent(true,500);

    assertTrue(event.getSuccessful());
    assertEquals(500, event.getExecutionTime());
    assertTrue(event.isExecutionTimeAvailable());
    assertEquals("EXECUTION", event.getEventType());
}

@Test
void testExecutionEventWithUnavailableDuration() {
    ExecutionEvent event = new ExecutionEvent(false, (Long) null);

    assertFalse(event.getSuccessful());
    assertNull(event.getExecutionTime());
    assertFalse(event.isExecutionTimeAvailable());
}

}