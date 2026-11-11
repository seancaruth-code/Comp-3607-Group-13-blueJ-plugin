
package com.comp3607.telemetry.observer;

import com.comp3607.telemetry.events.TelemetryEvent;

public interface TelemetryObserver {

    void onEvent(TelemetryEvent event);

}
