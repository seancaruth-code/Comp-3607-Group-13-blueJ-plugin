
package com.comp3607.telemetry.observer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.comp3607.telemetry.events.TelemetryEvent;

public class TelemetryManager implements TelemetryObserver {

    private final List<TelemetryEvent> events =
            new ArrayList<>();

    @Override
    public synchronized void onEvent(TelemetryEvent event) {

        if (event == null) {
            throw new IllegalArgumentException(
                "Event cannot be null"
            );
        }

        events.add(event);
    }

    public synchronized List<TelemetryEvent> getEvents() {
        return Collections.unmodifiableList(
            new ArrayList<>(events)
        );
    }

    public synchronized int getEventCount() {
        return events.size();
    }

    public synchronized void clearEvents() {
        events.clear();
    }
}
