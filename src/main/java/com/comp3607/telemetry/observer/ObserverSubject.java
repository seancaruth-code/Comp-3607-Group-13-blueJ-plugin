
package com.comp3607.telemetry.observer;

import java.util.*;
import java.util.logging.*;

import com.comp3607.telemetry.events.TelemetryEvent;

/**
 * Maintains observers and broadcasts telemetry events to them.
 */
public class ObserverSubject {
    private static final Logger LOGGER = Logger.getLogger(ObserverSubject.class.getName());

    private final List<Observer> observers = new ArrayList<>();

    public synchronized void addObserver(Observer observer) {
        if (observer == null) {
            throw new IllegalArgumentException("Observer cannot be null");
        }

        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public synchronized void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    public void notifyObservers(TelemetryEvent event) {
        if (event == null) {
            throw new IllegalArgumentException(
                "Event cannot be null"
            );
        }

        List<Observer> observerCopy;

        synchronized (this) {
            observerCopy = new ArrayList<>(observers);
        }

        for (Observer observer : observerCopy) {
            try {
                observer.onEvent(event);
            } catch (RuntimeException exception) {
                LOGGER.log(Level.WARNING, "Observer failed while receiving telemetry event", exception);
            }
        }
    }
}