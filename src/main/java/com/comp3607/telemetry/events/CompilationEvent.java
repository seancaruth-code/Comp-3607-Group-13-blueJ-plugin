
package com.comp3607.telemetry.events;

public class CompilationEvent extends TelemetryEvent {

    private final boolean successful;
    private final long compilationTime;
    private final String errorMessage;

    public CompilationEvent(boolean successful,long compilationTime,String errorMessage) {

        super("COMPILATION",getmessage(successful));

        this.successful = successful;
        this.compilationTime = compilationTime;
        this.errorMessage = errorMessage;
    }

    private static String getmessage(boolean successful) {
        if (successful) {
            return "Compilation successful";
        } 
        else {
            return "Compilation failed";
        }
    }

    public boolean getSuccessful() {
        return successful;
    }

    public long getCompilationTime() {
        return compilationTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
