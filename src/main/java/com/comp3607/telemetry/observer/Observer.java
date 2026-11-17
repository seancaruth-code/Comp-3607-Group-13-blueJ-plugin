package com.comp3607.telemetry.observer;

import com.comp3607.telemetry.events.TelemetryEvent;

/** Receives telemetry events published by an ObserverSubject. */
public interface Observer {
    /** Handles one newly published telemetry event. */
    void onEvent(TelemetryEvent event);

    /**
     * Backwards-compatible alias for the original callback name.
     * @deprecated use {@link #onEvent(TelemetryEvent)}
     */
    @Deprecated
    default void Event(TelemetryEvent event) {
        onEvent(event);
    }
}
