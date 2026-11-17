package com.comp3607.telemetry.editor;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import com.comp3607.telemetry.events.EditorEvent;

class EditorActivityTrackerTest {
    @Test void classifiesShortIntervalsAsActiveAndLongIntervalsAsIdle() {
        EditorActivityTracker tracker=new EditorActivityTracker(30_000);
        Instant start=Instant.parse("2026-11-15T10:00:00Z");
        EditorEvent first=tracker.recordEdit(start);
        EditorEvent active=tracker.recordEdit(start.plusSeconds(5));
        EditorEvent idle=tracker.recordEdit(start.plusSeconds(45));
        assertFalse(first.isIdle());
        assertFalse(active.isIdle());
        assertEquals(5000,active.getDuration());
        assertTrue(idle.isIdle());
        assertEquals(40000,idle.getDuration());
        assertEquals(1,idle.getEditCount());
    }
    @Test void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class,()->new EditorActivityTracker(-1));
        EditorActivityTracker tracker=new EditorActivityTracker(10);
        assertThrows(IllegalArgumentException.class,()->tracker.recordEdit(null));
    }
}
