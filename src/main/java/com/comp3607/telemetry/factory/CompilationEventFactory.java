package com.comp3607.telemetry.factory;

import java.util.List;

import com.comp3607.telemetry.events.CompilationEvent;

/**
 * Creates compilation events after validating compilation metadata.
 */
public class CompilationEventFactory extends TelemetryEventFactory {

    private final boolean successful;
    private final long compilationTime;
    private final String errorMessage;
    private final Long measuredCompilationTime;
    private final Integer errorLineNumber;
    private final int[] errorPosition;
    private final List<String> files;
    private final boolean userGenerated;

    public CompilationEventFactory(boolean successful, long compilationTime, String errorMessage) {
        if (compilationTime < 0) {
            throw new IllegalArgumentException("Compilation time cannot be negative");
        }

        this.successful = successful;
        this.compilationTime = compilationTime;
        this.errorMessage = errorMessage;
        this.measuredCompilationTime = compilationTime;
        this.errorLineNumber = null;
        this.errorPosition = null;
        this.files = List.of();
        this.userGenerated = false;
    }

    public CompilationEventFactory(boolean successful, Long compilationTime, String errorMessage,
                                   Integer errorLineNumber, int[] errorPosition,
                                   List<String> files, boolean userGenerated) {
        if (compilationTime != null && compilationTime < 0) {
            throw new IllegalArgumentException("Compilation time cannot be negative");
        }
        this.successful = successful;
        this.compilationTime = compilationTimeOrZero(compilationTime);
        this.measuredCompilationTime = compilationTime;
        this.errorMessage = errorMessage;
        this.errorLineNumber = errorLineNumber;
        this.errorPosition = copyErrorPosition(errorPosition);
        this.files = copyFiles(files);
        this.userGenerated = userGenerated;
    }

    @Override
    public CompilationEvent createEvent() {
        return new CompilationEvent(successful, measuredCompilationTime, errorMessage,
            errorLineNumber, errorPosition, files, userGenerated);
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
