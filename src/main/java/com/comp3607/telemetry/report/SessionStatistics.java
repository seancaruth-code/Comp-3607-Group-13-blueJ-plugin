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
    private final double averageCompilationTimeMs;
    /** Estimated idle time in milliseconds. */
    private final long activeTimeMs, idleTimeMs;
    private final long inactiveTimeMs, typingTimeMs;
    private final long sessionDurationMs, unclassifiedTimeMs, awayTimeMs;
    private final double activeRatio;
    /** Event categories and their counts. */
    private final Map<String,Integer> errorFrequency, eventCounts;
    /** Chronological class lifecycle events. */
    private final List<ClassTimelineEntry> classTimeline;
    /** Chronological method signature changes. */
    private final List<MethodTimelineEntry> methodTimeline;

    private SessionStatistics(List<TelemetryEvent> events) {
        this(events, 0L);
    }

    private SessionStatistics(List<TelemetryEvent> events, long sessionDurationMs) {
        int attempts=0, successes=0, failures=0, runs=0, failedRuns=0, edits=0;
        long active=0, idle=0, compilationTime=0;
        int timedCompilations=0;
        Map<String,Integer> errors=new LinkedHashMap<>(), counts=new TreeMap<>();
        List<ClassTimelineEntry> classes=new ArrayList<>();
        List<MethodTimelineEntry> methods=new ArrayList<>();
        for (TelemetryEvent event: events) {
            counts.merge(event.getEventType(),1,Integer::sum);
            if (event instanceof CompilationEvent c) {
                attempts++;
                if(c.isCompilationTimeAvailable()){
                    compilationTime+=Math.max(0,c.getCompilationTime());
                    timedCompilations++;
                }
                if (c.getSuccessful()) successes++;
                else {
                    failures++;
                    String message=c.getErrorMessage();
                    if(message!=null && !message.isBlank())
                        errors.merge(message.strip(),1,Integer::sum);
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
        this.inactiveTimeMs=idle;
        this.typingTimeMs=active;
        this.sessionDurationMs=Math.max(0L,sessionDurationMs);
        long measured=Math.min(this.sessionDurationMs,active)+Math.min(Math.max(0L,this.sessionDurationMs-active),idle);
        this.unclassifiedTimeMs=Math.max(0L,this.sessionDurationMs-measured);
        this.awayTimeMs=0L;
        this.activeRatio=ratio(active,active+idle);
        this.averageCompilationTimeMs=timedCompilations==0?0.0:(double)compilationTime/timedCompilations;
        this.errorFrequency=Collections.unmodifiableMap(new LinkedHashMap<>(errors)); this.eventCounts=Collections.unmodifiableMap(new LinkedHashMap<>(counts));
        this.classTimeline=List.copyOf(classes); this.methodTimeline=List.copyOf(methods);
    }
    public static SessionStatistics from(List<TelemetryEvent> events) {
        if (events==null) throw new IllegalArgumentException("Events cannot be null");
        return new SessionStatistics(List.copyOf(events));
    }
    public static SessionStatistics from(List<TelemetryEvent> events, long sessionDurationMs) {
        if (events==null) throw new IllegalArgumentException("Events cannot be null");
        return new SessionStatistics(List.copyOf(events),sessionDurationMs);
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
    public double getAverageCompilationTimeMs(){return averageCompilationTimeMs;}
    public long getActiveTimeMs(){return activeTimeMs;}
    public long getIdleTimeMs(){return idleTimeMs;}
    public long getInactiveTimeMs(){return inactiveTimeMs;}
    public long getTypingTimeMs(){return typingTimeMs;}
    public long getSessionDurationMs(){return sessionDurationMs;}
    public long getUnclassifiedTimeMs(){return unclassifiedTimeMs;}
    public long getAwayTimeMs(){return awayTimeMs;}
    public double getActiveRatio(){return activeRatio;}
    public Map<String,Integer> getErrorFrequency(){return errorFrequency;}
    public Map<String,Integer> getEventCounts(){return eventCounts;}
    public List<ClassTimelineEntry> getClassTimeline(){return classTimeline;}
    public List<MethodTimelineEntry> getMethodTimeline(){return methodTimeline;}
    private static double ratio(long part,long whole){return whole==0?0.0:(double)part/whole;}
}
