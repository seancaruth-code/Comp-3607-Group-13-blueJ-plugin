
package com.comp3607.telemetry.events;

public class EditorEvent extends TelemetryEvent {

    private final String activityType;
    private final long duration;

    public EditorEvent(String activityType, long duration) {


        super("EDITOR",activityType + " activity");

        this.activityType = activityType;
        this.duration = duration;
    }

    public String getActivityType() {
        return activityType;
    }

    public long getDuration() {
        return duration;
    }
}
