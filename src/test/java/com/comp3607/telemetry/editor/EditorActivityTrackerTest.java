package com.comp3607.telemetry.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.comp3607.telemetry.events.EditorEvent;

class EditorActivityTrackerTest {
    @Test
    void calculatesCadenceFrequencyAndIdleEstimateFromDocumentEdits() {
        EditorActivityTracker tracker = new EditorActivityTracker(30_000);

        EditorEvent first = tracker.recordEdit(Instant.ofEpochMilli(0));
        EditorEvent second = tracker.recordEdit(Instant.ofEpochMilli(5_000));
        EditorEvent third = tracker.recordEdit(Instant.ofEpochMilli(40_000));

        assertNull(first.getTypingCadence());
        assertEquals(5_000L, second.getTypingCadence());
        assertEquals(35_000L, third.getTypingCadence());
        assertEquals(35_000L, third.getDuration());
        assertEquals(3, third.getEditCount());
        assertEquals(3 * 60_000.0 / 40_000.0, third.getEditFrequency());
        assertEquals(true, third.isIdleEstimated());
    }
}
