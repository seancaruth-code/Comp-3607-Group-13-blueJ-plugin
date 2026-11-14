package com.comp3607.telemetry.strategy;

import java.util.List;

import com.comp3607.telemetry.events.TelemetryEvent;

public interface ExportStrategy {

    /** Formats the supplied events into a single exportable string. */
    String export(List<TelemetryEvent> events);

    /** File extension (without the dot) suitable for this format. */
    String getFileExtension();
}
