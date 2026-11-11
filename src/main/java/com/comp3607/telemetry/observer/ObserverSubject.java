
package com.comp3607.telemetry.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.comp3607.telemetry.events.TelemetryEvent;

public class TelemetrySubject {

    private final List<TelemetryObserver> observers =
            new CopyOnWriteArrayList<>();

    public void addObserver(TelemetryObserver observer) {
        if (observer == null) {
            throw new IllegalArgumentException(
                "Observer cannot be null"
            );
        }

        observers.add(observer);
    }

    public void removeObserver(TelemetryObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(TelemetryEvent event) {
        if (event == null) {
            throw new IllegalArgumentException(
                "Event cannot be null"
            );
        }

        for (TelemetryObserver observer : observers) {
            observer.onEvent(event);
        }
    }
}
