package com.comp3607.telemetry.report;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.comp3607.telemetry.events.ClassEvent;
import com.comp3607.telemetry.events.CompilationEvent;
import com.comp3607.telemetry.events.EditorEvent;
import com.comp3607.telemetry.events.ExecutionEvent;
import com.comp3607.telemetry.events.TelemetryEvent;
import com.comp3607.telemetry.events.MethodEvent;

/**
 * Calculates the session metrics used by the JSON and PDF reports.
 * This class has no dependency on BlueJ and can be tested independently.
 */
public final class SessionStatistics {
    public record ClassTimelineEntry(LocalDateTime time, String action, String className) {}
    public record MethodTimelineEntry(LocalDateTime time, String action, String className, String methodSignature) {}

    private final int totalEvents, compilationAttempts, successfulCompilations,
        failedCompilations, executions, failedExecutions, totalEdits;
    /** Estimated idle time in milliseconds. */
    private final long activeTimeMs, idleTimeMs;
    /** Event categories and their counts. */
    private final Map<String,Integer> errorFrequency, eventCounts;
    /** Chronological class lifecycle events. */
    private final List<ClassTimelineEntry> classTimeline;
    /** Chronological method signature changes. */
    private final List<MethodTimelineEntry> methodTimeline;

    private SessionStatistics(List<TelemetryEvent> events) {
        int attempts=0, successes=0, failures=0, runs=0, failedRuns=0, edits=0;
        long active=0, idle=0;
        Map<String,Integer> errors=new LinkedHashMap<>(), counts=new TreeMap<>();
        List<ClassTimelineEntry> classes=new ArrayList<>();
        List<MethodTimelineEntry> methods=new ArrayList<>();
        for (TelemetryEvent event: events) {
            counts.merge(event.getEventType(),1,Integer::sum);
            if (event instanceof CompilationEvent c) {
                attempts++;
                if (c.getSuccessful()) successes++;
                else {
                    failures++;
                    String message=c.getErrorMessage();
                    errors.merge(message==null || message.isBlank() ? "Unknown error" : message.strip(),1,Integer::sum);
                }
            } else if (event instanceof ExecutionEvent e) {
                runs++;
                if (!e.getSuccessful()) failedRuns++;
            } else if (event instanceof EditorEvent e) {
                long duration=Math.max(0,e.getDuration());
                if (e.isIdle()) idle+=duration; else active+=duration;
                edits+=Math.max(0,e.getEditCount());
            } else if (event instanceof ClassEvent c) {
                classes.add(new ClassTimelineEntry(c.getTimestamp(),c.getAction(),c.getClassName()));
            } else if (event instanceof MethodEvent m) {
                methods.add(new MethodTimelineEntry(m.getTimestamp(),m.getAction(),m.getClassName(),m.getMethodSignature()));
            }
        }
        classes.sort(Comparator.comparing(ClassTimelineEntry::time));
        methods.sort(Comparator.comparing(MethodTimelineEntry::time));
        this.totalEvents=events.size(); this.compilationAttempts=attempts;
        this.successfulCompilations=successes; this.failedCompilations=failures;
        this.executions=runs; this.failedExecutions=failedRuns; this.totalEdits=edits;
        this.activeTimeMs=active; this.idleTimeMs=idle;
        this.errorFrequency=Collections.unmodifiableMap(new LinkedHashMap<>(errors)); this.eventCounts=Collections.unmodifiableMap(new LinkedHashMap<>(counts));
        this.classTimeline=List.copyOf(classes); this.methodTimeline=List.copyOf(methods);
    }
    public static SessionStatistics from(List<TelemetryEvent> events) {
        if (events==null) throw new IllegalArgumentException("Events cannot be null");
        return new SessionStatistics(List.copyOf(events));
    }
    public int getTotalEvents(){return totalEvents;}
    public int getCompilationAttempts(){return compilationAttempts;}
    public int getSuccessfulCompilations(){return successfulCompilations;}
    public int getFailedCompilations(){return failedCompilations;}
    public double getCompilationSuccessRate(){return ratio(successfulCompilations,compilationAttempts);}
    public double getCompilationErrorRatio(){return ratio(failedCompilations,compilationAttempts);}
    public int getExecutions(){return executions;}
    public int getFailedExecutions(){return failedExecutions;}
    public int getTotalEdits(){return totalEdits;}
    public long getActiveTimeMs(){return activeTimeMs;}
    public long getIdleTimeMs(){return idleTimeMs;}
    public double getActiveRatio(){return ratio(activeTimeMs,activeTimeMs+idleTimeMs);}
    public Map<String,Integer> getErrorFrequency(){return errorFrequency;}
    public Map<String,Integer> getEventCounts(){return eventCounts;}
    public List<ClassTimelineEntry> getClassTimeline(){return classTimeline;}
    public List<MethodTimelineEntry> getMethodTimeline(){return methodTimeline;}
    private static double ratio(long part,long whole){return whole==0?0.0:(double)part/whole;}
}
