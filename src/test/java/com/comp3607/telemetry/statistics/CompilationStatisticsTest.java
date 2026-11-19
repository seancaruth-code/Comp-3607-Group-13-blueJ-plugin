package com.comp3607.telemetry.statistics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.comp3607.telemetry.events.*;

class CompilationStatisticsTest {

    @Test
    void countsSuccessfulCompilations() {
        CompilationStatistics statistics = new CompilationStatistics();

        statistics.onEvent(new CompilationEvent(true, 100, null));

        assertEquals(1, statistics.getTotalCompilationAttempts());
        assertEquals(1, statistics.getSuccessfulCompilations());
        assertEquals(0, statistics.getFailedCompilations());
    }

    @Test
    void countsFailedCompilations() {
        CompilationStatistics statistics = new CompilationStatistics();

        statistics.onEvent(new CompilationEvent(false, 200, "Syntax error"));

        assertEquals(1, statistics.getTotalCompilationAttempts());
        assertEquals(0, statistics.getSuccessfulCompilations());
        assertEquals(1, statistics.getFailedCompilations());
    }

    @Test
    void calculatesAverageCompilationTime() {
        CompilationStatistics statistics = new CompilationStatistics();

        statistics.onEvent(new CompilationEvent(true, 100, null));
        statistics.onEvent(new CompilationEvent(false, 300, "Syntax error"));

        assertEquals(200.0, statistics.getAverageCompilationTime());
    }

    @Test
    void countsRepeatedErrorMessages() {
        CompilationStatistics statistics = new CompilationStatistics();

        statistics.onEvent(new CompilationEvent(false, 100, "Syntax error"));
        statistics.onEvent(new CompilationEvent(false, 150, "Syntax error"));
        statistics.onEvent(new CompilationEvent(false, 200, "Type error"));

        assertEquals(Map.of("Syntax error", 2, "Type error", 1),
            statistics.getErrorFrequency());
    }

    @Test
    void ignoresMissingAndBlankErrorMessages() {
        CompilationStatistics statistics = new CompilationStatistics();

        statistics.onEvent(new CompilationEvent(false, 100, null));
        statistics.onEvent(new CompilationEvent(false, 100, ""));
        statistics.onEvent(new CompilationEvent(false, 100, "   "));

        assertTrue(statistics.getErrorFrequency().isEmpty());
        assertEquals(3, statistics.getFailedCompilations());
    }

    @Test
    void ignoresNonCompilationEvents() {
        CompilationStatistics statistics = new CompilationStatistics();

        statistics.onEvent(new ProjectEvent("CREATE", "Student.java"));

        assertEquals(0, statistics.getTotalCompilationAttempts());
        assertEquals(0.0, statistics.getAverageCompilationTime());
        assertTrue(statistics.getErrorFrequency().isEmpty());
    }

    @Test
    void startsWithEmptyStatisticsAndReturnsSafeErrorMap() {
        CompilationStatistics statistics = new CompilationStatistics();

        assertEquals(0, statistics.getTotalCompilationAttempts());
        assertEquals(0, statistics.getSuccessfulCompilations());
        assertEquals(0, statistics.getFailedCompilations());
        assertEquals(0.0, statistics.getAverageCompilationTime());

        Map<String, Integer> errors = statistics.getErrorFrequency();
        assertFalse(errors.containsKey("anything"));
    }
}
