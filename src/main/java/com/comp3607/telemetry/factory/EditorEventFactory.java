
package com.comp3607.telemetry.factory;

import java.util.Objects;

import com.comp3607.telemetry.events.EditorEvent;

public class EditorEventFactory extends TelemetryEventFactory {

    private final String activityType;
    private final long duration;
    private final int editCount;
    private final Long typingCadence;
    private final double editFrequency;
    private final boolean idle;

    public EditorEventFactory(String activityType, long duration) {
        this(activityType, duration, 0, null, 0.0, false);
    }

    public EditorEventFactory(String activityType, long duration, int editCount,
                              Long typingCadence, double editFrequency, boolean idle) {
        this.activityType = Objects.requireNonNull(activityType, "Activity type cannot be null");

        if (duration < 0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }

        if (editCount < 0 || editFrequency < 0) {
            throw new IllegalArgumentException("Editor metrics cannot be negative");
        }

        this.duration = duration;
        this.editCount = editCount;
        this.typingCadence = typingCadence;
        this.editFrequency = editFrequency;
        this.idle = idle;
    }

    @Override
    public EditorEvent createEvent() {
        return new EditorEvent(activityType, duration, editCount, typingCadence,editFrequency, idle);
    }
}
