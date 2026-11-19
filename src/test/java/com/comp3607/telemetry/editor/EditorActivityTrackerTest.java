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

    @Test void noEditsAndOneEditDoNotInventActivity() {
        Instant start = Instant.parse("2026-11-15T10:00:00Z");
        EditorActivityTracker noEdits = new EditorActivityTracker(30_000);
        assertNull(noEdits.endSession(start.plusSeconds(300)));

        EditorActivityTracker oneEdit = new EditorActivityTracker(30_000);
        oneEdit.recordEdit(start);
        assertNull(oneEdit.endSession(start));
        assertThrows(IllegalStateException.class,
            () -> oneEdit.recordEdit(start.plusSeconds(1)));
    }

    @Test void sessionEndClassifiesFinalIntervalAndDoesNotExaggerateActiveTime() {
        Instant start = Instant.parse("2026-11-15T10:00:00Z");
        EditorActivityTracker shortEnd = new EditorActivityTracker(30_000);
        shortEnd.recordEdit(start);
        EditorEvent shortFinal = shortEnd.endSession(start.plusSeconds(10));
        assertFalse(shortFinal.isIdle());
        assertEquals(10_000, shortFinal.getDuration());

        EditorActivityTracker idleEnd = new EditorActivityTracker(30_000);
        idleEnd.recordEdit(start);
        EditorEvent idleFinal = idleEnd.endSession(start.plusSeconds(30));
        assertTrue(idleFinal.isIdle());
        assertEquals(30_000, idleFinal.getDuration());
    }

    @Test void multipleEditingAndIdleIntervalsAreClassified() {
        Instant start = Instant.parse("2026-11-15T10:00:00Z");
        EditorActivityTracker tracker = new EditorActivityTracker(30_000);
        tracker.recordEdit(start);
        assertEquals(5_000, tracker.recordEdit(start.plusSeconds(5)).getDuration());
        assertEquals(10_000, tracker.recordEdit(start.plusSeconds(15)).getDuration());
        assertTrue(tracker.recordEdit(start.plusSeconds(50)).isIdle());
        assertEquals(35_000, tracker.recordEdit(start.plusSeconds(85)).getDuration());
    }
}
