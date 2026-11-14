package com.comp3607.telemetry.strategy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.comp3607.telemetry.events.TelemetryEvent;

public class EventExporter {

    private volatile ExportStrategy strategy;

    public EventExporter(ExportStrategy strategy) {
        setStrategy(strategy);
    }

    public void setStrategy(ExportStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Export strategy cannot be null");
        }
        this.strategy = strategy;
    }

    public ExportStrategy getStrategy() {
        return strategy;
    }

    public String export(List<TelemetryEvent> events) {
        if (events == null) {
            throw new IllegalArgumentException("Events cannot be null");
        }
        return strategy.export(events);
    }

    /**
     * Writes the export to a file. The extension of {@code fileNameWithoutExtension}
     * is supplied by the active strategy; returns the path written.
     */
    public Path exportToFile(List<TelemetryEvent> events, Path directory, String fileNameWithoutExtension)
            throws IOException {
        Path target = directory.resolve(fileNameWithoutExtension + "." + strategy.getFileExtension());
        Files.writeString(target, export(events), StandardCharsets.UTF_8);
        return target;
    }
}
