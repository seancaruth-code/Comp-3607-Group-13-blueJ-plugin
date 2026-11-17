package com.comp3607.telemetry.strategy;

import java.util.List;
import java.util.Map;

import com.comp3607.telemetry.events.TelemetryEvent;

/**
 * Serializes telemetry events as a readable plain-text report.
 */
public class TextExportStrategy implements ExportStrategy {

    @Override
    public String export(List<TelemetryEvent> events) {
        StringBuilder out = new StringBuilder();
        for (TelemetryEvent event : events) {
            Map<String, Object> fields = EventDetails.of(event);
            out.append("[").append(fields.get("timestamp")).append("] ")
               .append(event.getEventType()).append(" - ").append(event.getDescription())
               .append(System.lineSeparator());
            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                String key = entry.getKey();
                if (key.equals("eventId") || key.equals("eventType")
                        || key.equals("timestamp") || key.equals("description")
                        || entry.getValue() == null) {
                    continue;
                }
                out.append("    ").append(key).append(": ")
                   .append(EventDetails.render(entry.getValue()))
                   .append(System.lineSeparator());
            }
        }
        return out.toString();
    }

    @Override
    public String getFileExtension() {
        return "txt";
    }
}
