package com.comp3607.telemetry.statistics;

import java.util.HashMap;
import java.util.Map;

import com.comp3607.telemetry.events.CompilationEvent;
import com.comp3607.telemetry.events.TelemetryEvent;
import com.comp3607.telemetry.observer.Observer;

public class CompilationStatistics implements Observer {

    private int totalCompilationAttempts;
    private int successfulCompilations;
    private int failedCompilations;
    private long totalCompilationTime;
    private int timedCompilations;
    private final Map<String, Integer> errorFrequency = new HashMap<>();

    @Override
    public synchronized void Event(TelemetryEvent event) {
        if (!(event instanceof CompilationEvent)) {
            return;
        }

        CompilationEvent compilationEvent = (CompilationEvent) event;
        totalCompilationAttempts++;
        if (compilationEvent.isCompilationTimeAvailable()) {
            totalCompilationTime += compilationEvent.getCompilationTime();
            timedCompilations++;
        }

        if (compilationEvent.getSuccessful()) {
            successfulCompilations++;
            return;
        }

        failedCompilations++;
        String errorMessage = compilationEvent.getErrorMessage();
        if (errorMessage != null && !errorMessage.isBlank()) {
            errorFrequency.merge(errorMessage, 1, Integer::sum);
        }
    }

    public synchronized int getTotalCompilationAttempts() {
        return totalCompilationAttempts;
    }

    public synchronized int getSuccessfulCompilations() {
        return successfulCompilations;
    }

    public synchronized int getFailedCompilations() {
        return failedCompilations;
    }

    public synchronized double getAverageCompilationTime() {
        if (timedCompilations == 0) {
            return 0.0;
        }
        return (double) totalCompilationTime / timedCompilations;
    }

    public synchronized Map<String, Integer> getErrorFrequency() {
        return Map.copyOf(errorFrequency);
    }
}
