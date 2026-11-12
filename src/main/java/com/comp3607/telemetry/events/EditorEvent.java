
package com.comp3607.telemetry.events;

public class EditorEvent extends TelemetryEvent {

    private final String activityType;
    private final long duration;
    private final int editCount;
    private final Long typingCadence;
    private final double editFrequency;
    private final boolean idle;

    public EditorEvent(String activityType, long duration) {
        this(activityType, duration, 0, null, 0.0, false);
    }

    public EditorEvent(String activityType, long duration, int editCount,
                       Long typingCadence, double editFrequency, boolean idleEstimated) {


        super("EDITOR",activityType + " activity");

        this.activityType = activityType;
        this.duration = duration;
        this.editCount = editCount;
        this.typingCadence = typingCadence;
        this.editFrequency = editFrequency;
        this.idle = idleEstimated;
    }

    public String getActivityType() {
        return activityType;
    }

    public long getDuration() {
        return duration;
    }

    public int getEditCount() { 
        return editCount; 
    }
    public Long getTypingCadence() { 
        return typingCadence; 
    }
    public double getEditFrequency() { 
        return editFrequency; 
    }
    public boolean isIdle() { 
        return idle; 
    }

    public boolean isIdleEstimated() {
        return idle;
    }
}
