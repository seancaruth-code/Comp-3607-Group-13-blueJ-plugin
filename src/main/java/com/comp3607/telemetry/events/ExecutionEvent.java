
package com.comp3607.telemetry.events;

import java.util.List;

public class ExecutionEvent extends TelemetryEvent {

    private final boolean successful;
    private final Long executionTime;
    private final String className;
    private final String methodName;
    private final String objectName;
    private final List<String> parameters;
    private final String terminationType;
    private final String exceptionMessage;

    public ExecutionEvent(boolean successful, long executionTime) {
        this(successful, Long.valueOf(executionTime));
    }

    public ExecutionEvent(boolean successful, Long executionTime) {
        this(successful, executionTime, null, null, null, List.of(), null, null);
    }

    public ExecutionEvent(boolean successful, Long executionTime, String className,String methodName, String objectName, List<String> parameters,String terminationType, String exceptionMessage) {

        super("EXECUTION",getmessage(successful));

        this.successful = successful;
        this.executionTime = executionTime;
        this.className = className;
        this.methodName = methodName;
        this.objectName = objectName;
        this.parameters = copyParameters(parameters);
        this.terminationType = terminationType;
        this.exceptionMessage = exceptionMessage;
    }

     private static String getmessage(boolean successful) {
        if (successful) {
            return "Program executed successfully";
        } 
        else {
            return "Program execution failed";
        }
    }



    public boolean getSuccessful() {
        return successful;
    }

    public Long getExecutionTime() {
        return executionTime;
    }

    public boolean isExecutionTimeAvailable() {
        return executionTime != null;
    }

    public String getClassName() { 
        return className; 
    }
    public String getMethodName() { 
        return methodName; 
    }
    public String getObjectName() { 
        return objectName; 
    }
    public List<String> getParameters() {
        return parameters; 
    }
    public String getTerminationType() { 
        return terminationType; 
    }
    public String getExceptionMessage() { 
        return exceptionMessage; 
    }

    private static List<String> copyParameters(List<String> parameters) {
        if (parameters == null) {
            return List.of();
        }
        return List.copyOf(parameters);
    }
}
