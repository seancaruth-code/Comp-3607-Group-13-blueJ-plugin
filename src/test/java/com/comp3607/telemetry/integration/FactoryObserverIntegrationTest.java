package com.comp3607.telemetry.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.comp3607.telemetry.events.CompilationEvent;
import com.comp3607.telemetry.factory.CompilationEventFactory;
import com.comp3607.telemetry.observer.ObserverManager;
import com.comp3607.telemetry.observer.ObserverSubject;
import com.comp3607.telemetry.statistics.CompilationStatistics;

class FactoryObserverIntegrationTest {

    @Test
    void factoryEventsAreStoredAndSummarizedByObservers() {
        ObserverSubject subject = new ObserverSubject();
        ObserverManager manager = new ObserverManager();
        CompilationStatistics statistics = new CompilationStatistics();
        subject.addObserver(manager);
        subject.addObserver(statistics);

        CompilationEvent failedEvent =
            new CompilationEventFactory(false, 350, "Missing semicolon").createEvent();
        subject.notifyObservers(failedEvent);

        assertEquals(1, manager.getEventCount());
        assertEquals(failedEvent, manager.getEvents().get(0));
        assertEquals(1, statistics.getTotalCompilationAttempts());
        assertEquals(0, statistics.getSuccessfulCompilations());
        assertEquals(1, statistics.getFailedCompilations());
        assertEquals(350.0, statistics.getAverageCompilationTime());
        assertEquals(1, statistics.getErrorFrequency().get("Missing semicolon"));

        CompilationEvent successfulEvent =
            new CompilationEventFactory(true, 150, null).createEvent();
        subject.notifyObservers(successfulEvent);

        assertEquals(2, manager.getEventCount());
        assertEquals(2, statistics.getTotalCompilationAttempts());
        assertEquals(1, statistics.getSuccessfulCompilations());
        assertEquals(1, statistics.getFailedCompilations());
        assertEquals(250.0, statistics.getAverageCompilationTime());
    }
}
