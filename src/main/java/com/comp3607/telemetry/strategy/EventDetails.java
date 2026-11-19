package com.comp3607.telemetry.strategy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import com.comp3607.telemetry.events.ClassEvent;
import com.comp3607.telemetry.events.CompilationEvent;
import com.comp3607.telemetry.events.EditorEvent;
import com.comp3607.telemetry.events.ExecutionEvent;
import com.comp3607.telemetry.events.ProjectEvent;
import com.comp3607.telemetry.events.MethodEvent;
import com.comp3607.telemetry.events.TelemetryEvent;

final class EventDetails {

    private EventDetails() {
    }

    static Map<String, Object> of(TelemetryEvent event) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("eventId", event.getEventId());
        fields.put("eventType", event.getEventType());
        fields.put("timestamp", event.getTimestamp().toString());
        fields.put("description", event.getDescription());

        if (event instanceof CompilationEvent e) {
            fields.put("successful", e.getSuccessful());
            fields.put("compilationTimeMs", e.isCompilationTimeAvailable() ? e.getCompilationTime() : null);
            fields.put("errorMessage", e.getErrorMessage());
            fields.put("errorLine", e.getErrorLineNumber());
            int[] position = e.getErrorPosition();
            fields.put("errorPosition", position == null ? null : Arrays.stream(position).boxed().toList());
            fields.put("files", e.getFiles());
            fields.put("userGenerated", e.isUserGeneratedCompilation());
        } else if (event instanceof ExecutionEvent e) {
            fields.put("successful", e.getSuccessful());
            fields.put("executionTimeMs", e.getExecutionTime());
            fields.put("className", e.getClassName());
            fields.put("methodName", e.getMethodName());
            fields.put("objectName", e.getObjectName());
            fields.put("parameters", e.getParameters());
            fields.put("methodSignature", e.getMethodSignature());
            fields.put("terminationType", e.getTerminationType());
            fields.put("exceptionMessage", e.getExceptionMessage());
        } else if (event instanceof EditorEvent e) {
            fields.put("activityType", e.getActivityType());
            fields.put("durationMs", e.getDuration());
            fields.put("editCount", e.getEditCount());
            fields.put("typingCadenceMs", e.getTypingCadence());
            fields.put("editFrequencyPerMin", e.getEditFrequency());
            fields.put("idle", e.isIdle());
        } else if (event instanceof ClassEvent e) {
            fields.put("action", e.getAction());
            fields.put("className", e.getClassName());
            fields.put("oldName", e.getOldName());
            fields.put("compiled", e.isCompiled());
            fields.put("hasError", e.hasError());
        } else if (event instanceof ProjectEvent e) {
            fields.put("action", e.getAction());
            fields.put("itemName", e.getItemName());
        } else if (event instanceof MethodEvent e) {
            fields.put("action", e.getAction());
            fields.put("className", e.getClassName());
            fields.put("methodSignature", e.getMethodSignature());
        }
        return fields;
    }

    static String render(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof List<?> list) {
            return String.join(";", list.stream().map(String::valueOf).toList());
        }
        return String.valueOf(value);
    }
}
