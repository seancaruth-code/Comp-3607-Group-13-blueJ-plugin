
package com.comp3607.telemetry.events;

import java.util.List;

/**
 * Records the outcome and diagnostic metadata of a compilation.
 */
public class CompilationEvent extends TelemetryEvent {

    /** Whether the operation completed successfully. */

    private final boolean successful;
    /** Measured compilation duration in milliseconds. */
    private final long compilationTime;
    /** Compiler diagnostic text, when available. */
    private final String errorMessage;
    /** Line containing the compilation error, when available. */
    private final Integer errorLineNumber;
    /** Editor position associated with the compilation diagnostic. */
    private final int[] errorPosition;
    /** Source files included in the compilation. */
    private final List<String> files;
    /** Whether the compilation was initiated by the user. */
    private final boolean userGenerated;
    /** Whether a compilation duration was measured. */
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
