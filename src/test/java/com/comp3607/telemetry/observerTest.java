
package com.comp3607.telemetry;

import org.junit.jupiter.api.Test;

import com.comp3607.telemetry.events.CompilationEvent;
import com.comp3607.telemetry.observer.TelemetryManager;
import com.comp3607.telemetry.observer.TelemetrySubject;

import static org.junit.jupiter.api.Assertions.*;

class TelemetrySubjectTest {

    @Test
    void testObserverReceivesEvent() {

        TelemetrySubject subject = new TelemetrySubject();
        TelemetryManager manager = new TelemetryManager();

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

        TelemetrySubject subject = new TelemetrySubject();
        TelemetryManager manager = new TelemetryManager();

        subject.addObserver(manager);
        subject.removeObserver(manager);

        subject.notifyObservers(
            new CompilationEvent(true, 200, null)
        );

        assertEquals(0, manager.getEventCount());
    }

    @Test
    void testMultipleEvents() {

        TelemetrySubject subject = new TelemetrySubject();
        TelemetryManager manager = new TelemetryManager();

        subject.addObserver(manager);

        subject.notifyObservers(
            new CompilationEvent(true, 200, null)
        );

        subject.notifyObservers(
            new CompilationEvent(false, 350, "Syntax error")
        );

        assertEquals(2, manager.getEventCount());
    }
}
