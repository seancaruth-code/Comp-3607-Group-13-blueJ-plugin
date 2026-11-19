
package com.comp3607.telemetry.events;

import java.util.List;

/**
 * Records the outcome and available metadata of a method invocation.
 */
public class ExecutionEvent extends TelemetryEvent {

    /** Whether the operation completed successfully. */

    private final boolean successful;
    /** Invocation duration in milliseconds, when available. */
    private final Long executionTime;
    /** Name of the class associated with this event. */
    private final String className;
    /** Name of the invoked method. */
    private final String methodName;
    /** Name of the object on which the method was invoked. */
    private final String objectName;
    /** Invocation parameter descriptions. */
    private final List<String> parameters;
    /** Parameter types exposed by BlueJ for this invocation. */
    private final List<String> methodSignature;
    /** BlueJ invocation termination category. */
    private final String terminationType;
    /** Exception message, when an invocation failed. */
    private final String exceptionMessage;

    public ExecutionEvent(boolean successful, long executionTime) {
        this(successful, Long.valueOf(executionTime));
    }

    public ExecutionEvent(boolean successful, Long executionTime) {
        this(successful, executionTime, null, null, null, List.of(), null, null);
    }

    public ExecutionEvent(boolean successful, Long executionTime, String className,String methodName, String objectName, List<String> parameters,String terminationType, String exceptionMessage) {
        this(successful, executionTime, className, methodName, objectName, parameters,
            List.of(), terminationType, exceptionMessage);
    }

    public ExecutionEvent(boolean successful, Long executionTime, String className, String methodName,
                         String objectName, List<String> parameters, List<String> methodSignature,
                         String terminationType, String exceptionMessage) {
        super("EXECUTION",getmessage(successful));

        this.successful = successful;
        this.executionTime = executionTime;
        this.className = className;
        this.methodName = methodName;
        this.objectName = objectName;
        this.parameters = copyParameters(parameters);
        this.methodSignature = copyParameters(methodSignature);
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
    public List<String> getMethodSignature() {
        return methodSignature;
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
