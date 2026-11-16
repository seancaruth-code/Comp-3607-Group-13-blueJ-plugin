package com.comp3607.telemetry.factory;

import com.comp3607.telemetry.events.TelemetryEvent;


/**
 * Factory Method abstraction for constructing telemetry event objects.
 */
public abstract class TelemetryEventFactory {


    public abstract TelemetryEvent createEvent();
}
