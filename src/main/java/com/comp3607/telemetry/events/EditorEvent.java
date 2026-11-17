
package com.comp3607.telemetry.events;

/**
 * Records editor-document activity measurements and estimated idle intervals.
 */
public class EditorEvent extends TelemetryEvent {

    /** Kind of editor activity. */

    private final String activityType;
    /** Duration of the measured editor interval in milliseconds. */
    private final long duration;
    /** Number of document replacements represented by this event. */
    private final int editCount;
    /** Time between the previous and current document replacements. */
    private final Long typingCadence;
    /** Estimated document replacement frequency per minute. */
    private final double editFrequency;
    /** Whether the measured interval is classified as idle. */
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
