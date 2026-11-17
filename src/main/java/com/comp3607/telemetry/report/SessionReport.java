package com.comp3607.telemetry.report;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import com.comp3607.telemetry.events.TelemetryEvent;

/** Immutable snapshot of a BlueJ coding session and its derived statistics. */
public final class SessionReport {
    /** Name of the BlueJ project. */
    private final String studentId, projectName;
    /** Time the telemetry session ended. */
    private final LocalDateTime startTime, endTime;
    /** Immutable snapshot of the recorded telemetry events. */
    private final List<TelemetryEvent> events;
    /** Statistics calculated from the recorded events. */
    private final SessionStatistics statistics;

    public SessionReport(String studentId, String projectName, LocalDateTime startTime,
                         LocalDateTime endTime, List<TelemetryEvent> events) {
        if (studentId==null || projectName==null || startTime==null || endTime==null || events==null)
            throw new IllegalArgumentException("Session metadata and events are required");
        if (endTime.isBefore(startTime)) throw new IllegalArgumentException("End time cannot precede start time");
        this.studentId=studentId; this.projectName=projectName;
        this.startTime=startTime; this.endTime=endTime;
        this.events=List.copyOf(events); this.statistics=SessionStatistics.from(this.events);
    }
    public String getStudentId(){return studentId;}
    public String getProjectName(){return projectName;}
    public LocalDateTime getStartTime(){return startTime;}
    public LocalDateTime getEndTime(){return endTime;}
    public Duration getDuration(){return Duration.between(startTime,endTime);}
    public List<TelemetryEvent> getEvents(){return events;}
    public SessionStatistics getStatistics(){return statistics;}
}
