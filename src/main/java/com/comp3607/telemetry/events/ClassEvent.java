package com.comp3607.telemetry.events;

/**
 * Represents a BlueJ class lifecycle or state change.
 */
public class ClassEvent extends TelemetryEvent {
    /** Lifecycle action represented by the event. */
    private final String action;
    /** Name of the class associated with this event. */
    private final String className;
    /** Previous class name when a class was renamed. */
    private final String oldName;
    /** Whether the class was compiled at the time of the event. */
    private final boolean compiled;
    /** Whether the class currently has a compilation error. */
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
