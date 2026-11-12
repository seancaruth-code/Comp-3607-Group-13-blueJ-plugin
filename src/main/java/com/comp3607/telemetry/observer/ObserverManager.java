
package com.comp3607.telemetry.observer;

import java.util.ArrayList;
import java.util.List;

import com.comp3607.telemetry.events.TelemetryEvent;

public class ObserverManager implements Observer {

    private final List<TelemetryEvent> events =new ArrayList<>();

    @Override
    public synchronized void Event(TelemetryEvent event) {

        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        events.add(event);
    }

    public synchronized List<TelemetryEvent> getEvents() {
        return List.copyOf(events);
    }

    public synchronized int getEventCount() {
        return events.size();
    }

    public synchronized void clearEvents() {
        events.clear();
    }
}
