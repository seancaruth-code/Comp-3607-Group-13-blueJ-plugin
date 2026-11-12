package com.comp3607.telemetry.factory;

import java.util.List;

import com.comp3607.telemetry.events.ExecutionEvent;

public class ExecutionEventFactory extends TelemetryEventFactory {

    private final boolean successful;
    private final Long executionTime;
    private final String className;
    private final String methodName;
    private final String objectName;
    private final List<String> parameters;
    private final String terminationType;
    private final String exceptionMessage;

    public ExecutionEventFactory(boolean successful, long executionTime) {
        this(successful, Long.valueOf(executionTime));
    }

    public ExecutionEventFactory(boolean successful, Long executionTime) {
        if (executionTime != null && executionTime < 0) {
            throw new IllegalArgumentException("Execution time cannot be negative");
        }
        this.successful = successful;
        this.executionTime = executionTime;
        this.className = null;
        this.methodName = null;
        this.objectName = null;
        this.parameters = List.of();
        this.terminationType = null;
        this.exceptionMessage = null;
    }

    public ExecutionEventFactory(boolean successful, Long executionTime, String className,
                                 String methodName, String objectName, List<String> parameters,
                                 String terminationType, String exceptionMessage) {
        if (executionTime != null && executionTime < 0) {
            throw new IllegalArgumentException("Execution time cannot be negative");
        }
        this.successful = successful;
        this.executionTime = executionTime;
        this.className = className;
        this.methodName = methodName;
        this.objectName = objectName;
        this.parameters = copyParameters(parameters);
        this.terminationType = terminationType;
        this.exceptionMessage = exceptionMessage;
    }

    @Override
    public ExecutionEvent createEvent() {
        return new ExecutionEvent(successful, executionTime, className, methodName, objectName,
            parameters, terminationType, exceptionMessage);
    }

    private static List<String> copyParameters(List<String> parameters) {
        if (parameters == null) {
            return List.of();
        }
        return List.copyOf(parameters);
    }
}
