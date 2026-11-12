package com.comp3607.telemetry.editor;

import java.time.Duration;
import java.time.Instant;

import com.comp3607.telemetry.events.EditorEvent;
import com.comp3607.telemetry.factory.EditorEventFactory;

/**
 * Computes edit metrics from BlueJ document replacement callbacks.
 * A replacement is an edit activity, not an individual keystroke.
 */
public class EditorActivityTracker {
    private final long idleThresholdMillis;
    private Instant firstEdit;
    private Instant lastEdit;
    private int editCount;

    public EditorActivityTracker(long idleThresholdMillis) {
        if (idleThresholdMillis < 0) {
            throw new IllegalArgumentException("Idle threshold cannot be negative");
        }
        this.idleThresholdMillis = idleThresholdMillis;
    }

    public synchronized EditorEvent recordEdit(Instant timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        Long cadence = null;
        long idle = 0;
        if (lastEdit != null) {
            long interval = Math.max(0, Duration.between(lastEdit, timestamp).toMillis());
            cadence = interval;
            if (interval >= idleThresholdMillis) {
                idle = interval;
            }
        }
        if (firstEdit == null) {
            firstEdit = timestamp;
        }
        lastEdit = timestamp;
        editCount++;
        long elapsed = Math.max(1, Duration.between(firstEdit, timestamp).toMillis());
        double frequency = editCount * 60_000.0 / elapsed;
        return new EditorEventFactory("EDIT", idle, editCount, cadence, frequency, true).createEvent();
    }
}
