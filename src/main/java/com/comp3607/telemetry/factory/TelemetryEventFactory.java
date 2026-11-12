package com.comp3607.telemetry.factory;

import com.comp3607.telemetry.events.TelemetryEvent;


public abstract class TelemetryEventFactory {


    public abstract TelemetryEvent createEvent();
}
