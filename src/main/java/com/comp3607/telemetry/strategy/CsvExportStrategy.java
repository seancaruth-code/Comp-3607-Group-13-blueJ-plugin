package com.comp3607.telemetry.strategy;

import java.util.List;

import com.comp3607.telemetry.events.TelemetryEvent;

/**
 * Serializes telemetry events as CSV rows.
 */
public class CsvExportStrategy implements ExportStrategy {

    private static final String HEADER = "eventId,eventType,timestamp,description,details";

    @Override
    public String export(List<TelemetryEvent> events) {
        StringBuilder out = new StringBuilder(HEADER).append(System.lineSeparator());
        for (TelemetryEvent event : events) {
            StringBuilder details = new StringBuilder();
            EventDetails.of(event).forEach((key, value) -> {
                if (value == null || isCommon(key)) {
                    return;
                }
                if (details.length() > 0) {
                    details.append("; ");
                }
                details.append(key).append("=").append(EventDetails.render(value));
            });
            out.append(escape(event.getEventId())).append(',')
               .append(escape(event.getEventType())).append(',')
               .append(escape(event.getTimestamp().toString())).append(',')
               .append(escape(event.getDescription())).append(',')
               .append(escape(details.toString()))
               .append(System.lineSeparator());
        }
        return out.toString();
    }

    private static boolean isCommon(String key) {
        return key.equals("eventId") || key.equals("eventType")
            || key.equals("timestamp") || key.equals("description");
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }
}
