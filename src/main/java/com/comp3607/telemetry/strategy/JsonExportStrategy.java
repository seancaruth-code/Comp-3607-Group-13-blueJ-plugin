package com.comp3607.telemetry.strategy;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.comp3607.telemetry.events.TelemetryEvent;

public class JsonExportStrategy implements ExportStrategy {

    @Override
    public String export(List<TelemetryEvent> events) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) {
                out.append(",");
            }
            out.append(System.lineSeparator()).append("  {");
            Iterator<Map.Entry<String, Object>> it = EventDetails.of(events.get(i)).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> entry = it.next();
                out.append("\"").append(entry.getKey()).append("\": ").append(toJson(entry.getValue()));
                if (it.hasNext()) {
                    out.append(", ");
                }
            }
            out.append("}");
        }
        if (!events.isEmpty()) {
            out.append(System.lineSeparator());
        }
        return out.append("]").toString();
    }

    private static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number number) {
            // JSON has no NaN/Infinity literals; writing them would make the whole file unparseable
            return Double.isFinite(number.doubleValue()) ? String.valueOf(number) : "null";
        }
        if (value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof List<?> list) {
            return "[" + String.join(", ", list.stream().map(JsonExportStrategy::toJson).toList()) + "]";
        }
        return quote(String.valueOf(value));
    }

    static String quote(String text) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append("\"").toString();
    }

    @Override
    public String getFileExtension() {
        return "json";
    }
}
