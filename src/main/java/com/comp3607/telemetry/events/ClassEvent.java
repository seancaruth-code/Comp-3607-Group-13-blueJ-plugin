package com.comp3607.telemetry.events;

public class ClassEvent extends TelemetryEvent {
    private final String action;
    private final String className;
    private final String oldName;
    private final boolean compiled;
    private final boolean hasError;

    public ClassEvent(String action, String className, String oldName,boolean compiled, boolean hasError) {
        
        super("CLASS", action + ": " + className);
        this.action = action;
        this.className = className;
        this.oldName = oldName;
        this.compiled = compiled;
        this.hasError = hasError;
    }

    public String getAction() { return action; }
    public String getClassName() { return className; }
    public String getOldName() { return oldName; }
    public boolean isCompiled() { return compiled; }
    public boolean hasError() { return hasError; }
}
