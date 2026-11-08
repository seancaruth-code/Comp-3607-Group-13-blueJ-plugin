
package com.comp3607.telemetry.events;

public class ExecutionEvent extends TelemetryEvent {

    private final boolean successful;
    private final long executionTime;

    public ExecutionEvent(boolean successful,long executionTime) {

        super("EXECUTION",getmessage(successful));

        this.successful = successful;
        this.executionTime = executionTime;
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

    public long getExecutionTime() {
        return executionTime;
    }
}
