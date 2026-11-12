
package com.comp3607.telemetry.events;

import java.util.List;

public class CompilationEvent extends TelemetryEvent {

    private final boolean successful;
    private final long compilationTime;
    private final String errorMessage;
    private final Integer errorLineNumber;
    private final int[] errorPosition;
    private final List<String> files;
    private final boolean userGenerated;
    private final boolean compilationTimeAvailable;

    public CompilationEvent(boolean successful,long compilationTime,String errorMessage) {
        this(successful, compilationTime, errorMessage, null, null, List.of(), false, true);
    }

    public CompilationEvent(boolean successful, Long compilationTime, String errorMessage,
                            Integer errorLineNumber, int[] errorPosition, List<String> files,
                            boolean userGenerated) {
        this(successful, compilationTimeOrZero(compilationTime), errorMessage,
            errorLineNumber, errorPosition, files, userGenerated, compilationTime != null);
    }

    private CompilationEvent(boolean successful, long compilationTime, String errorMessage,
                             Integer errorLineNumber, int[] errorPosition, List<String> files,
                             boolean userGenerated, boolean compilationTimeAvailable) {
        super("COMPILATION",getmessage(successful));

        this.successful = successful;
        this.compilationTime = compilationTime;
        this.errorMessage = errorMessage;
        this.errorLineNumber = errorLineNumber;
        this.errorPosition = copyErrorPosition(errorPosition);
        this.files = copyFiles(files);
        this.userGenerated = userGenerated;
        this.compilationTimeAvailable = compilationTimeAvailable;
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

    public Integer getErrorLineNumber() {
        return errorLineNumber;
    }

    public int[] getErrorPosition() {
        return copyErrorPosition(errorPosition);
    }

    public List<String> getFiles() {
        return files;
    }

    public boolean isUserGeneratedCompilation() {
        return userGenerated;
    }

    public boolean isCompilationTimeAvailable() {
        return compilationTimeAvailable;
    }

    private static long compilationTimeOrZero(Long compilationTime) {
        if (compilationTime == null) {
            return 0L;
        }
        return compilationTime;
    }

    private static int[] copyErrorPosition(int[] errorPosition) {
        if (errorPosition == null) {
            return null;
        }
        return errorPosition.clone();
    }

    private static List<String> copyFiles(List<String> files) {
        if (files == null) {
            return List.of();
        }
        return List.copyOf(files);
    }
}
