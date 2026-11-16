package com.comp3607.telemetry.events;

/** Records a method signature appearing or disappearing from a BlueJ class. */
public final class MethodEvent extends TelemetryEvent {
    /** Lifecycle action represented by the event. */
    private final String action;
    /** Name of the class associated with this event. */
    private final String className;
    /** Method signature reported by BlueJ. */
    private final String methodSignature;

    public MethodEvent(String action, String className, String methodSignature) {
        super("METHOD", action + ": " + className + "." + methodSignature);
        if(action==null || className==null || methodSignature==null)
            throw new IllegalArgumentException("Method event fields are required");
        this.action=action; this.className=className; this.methodSignature=methodSignature;
    }
    public String getAction(){return action;}
    public String getClassName(){return className;}
    public String getMethodSignature(){return methodSignature;}
}
