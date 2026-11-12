
package com.comp3607.telemetry.observer;

import org.junit.jupiter.api.Test;

import com.comp3607.telemetry.events.CompilationEvent;

import static org.junit.jupiter.api.Assertions.*;

class observerTest {

    @Test
    void testObserverReceivesEvent() {

        ObserverSubject subject = new ObserverSubject();
        ObserverManager manager = new ObserverManager();

        subject.addObserver(manager);

        CompilationEvent event = new CompilationEvent(
            true,
            250,
            null
        );

        subject.notifyObservers(event);

        assertEquals(1, manager.getEventCount());
        assertEquals(
            "COMPILATION",
            manager.getEvents().get(0).getEventType()
        );
    }

    @Test
    void testRemoveObserver() {

        ObserverSubject subject = new ObserverSubject();
        ObserverManager manager = new ObserverManager();

        subject.addObserver(manager);
        subject.removeObserver(manager);

        subject.notifyObservers(
            new CompilationEvent(true, 200, null)
        );

        assertEquals(0, manager.getEventCount());
    }

    @Test
    void testMultipleEvents() {

        ObserverSubject subject = new ObserverSubject();
        ObserverManager manager = new ObserverManager();

        subject.addObserver(manager);

        subject.notifyObservers(
            new CompilationEvent(true, 200, null)
        );

        subject.notifyObservers(
            new CompilationEvent(false, 350, "Syntax error")
        );

        assertEquals(2, manager.getEventCount());
    }

    @Test
    void observerFailureDoesNotPreventLaterObservers() {
        ObserverSubject subject = new ObserverSubject();
        ObserverManager manager = new ObserverManager();
        subject.addObserver(event -> { throw new IllegalStateException("test failure"); });
        subject.addObserver(manager);

        subject.notifyObservers(new CompilationEvent(true, 200, null));

        assertEquals(1, manager.getEventCount());
    }
}
