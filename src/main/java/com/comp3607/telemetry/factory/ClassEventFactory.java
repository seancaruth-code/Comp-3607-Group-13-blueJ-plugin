package com.comp3607.telemetry.factory;

import java.util.Objects;

import com.comp3607.telemetry.events.ClassEvent;

public class ClassEventFactory extends TelemetryEventFactory {
    private final String action;
    private final String className;
    private final String oldName;
    private final boolean compiled;
    private final boolean hasError;

    public ClassEventFactory(String action, String className, String oldName,boolean compiled, boolean hasError) {
        
        this.action = Objects.requireNonNull(action, "Action cannot be null");
        this.className = Objects.requireNonNull(className, "Class name cannot be null");
        this.oldName = oldName;
        this.compiled = compiled;
        this.hasError = hasError;
    }

    @Override
    public ClassEvent createEvent() {
        return new ClassEvent(action, className, oldName, compiled, hasError);
    }
}
