package com.comp3607.telemetry.strategy;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.comp3607.telemetry.events.ClassEvent;
import com.comp3607.telemetry.events.CompilationEvent;
import com.comp3607.telemetry.events.EditorEvent;
import com.comp3607.telemetry.events.ExecutionEvent;
import com.comp3607.telemetry.events.ProjectEvent;
import com.comp3607.telemetry.events.TelemetryEvent;

class StrategyTest {

    /** Test double that records how the context calls a strategy. */
    private static class StubStrategy implements ExportStrategy {
        List<TelemetryEvent> received;

        @Override
        public String export(List<TelemetryEvent> events) {
            received = events;
            return "STUB";
        }

        @Override
        public String getFileExtension() {
            return "stub";
        }
    }

    private static List<TelemetryEvent> sampleEvents() {
        return List.of(
            new ProjectEvent("opened", "Demo"),
            new CompilationEvent(false, 120L, "missing ';'", 4, null, List.of("A.java", "B.java"), true),
            new ExecutionEvent(false, 40L, "Main", "main", null, List.of(), "EXCEPTION", "NullPointerException"),
            new EditorEvent("typing", 5000, 12, 250L, 2.5, false),
            new ClassEvent("created", "Student", null, false, false),
            new TelemetryEvent("CUSTOM", "custom event"));
    }

    static Stream<ExportStrategy> allStrategies() {
        return Stream.of(new JsonExportStrategy(), new CsvExportStrategy(), new TextExportStrategy());
    }

    // ---------- Context: EventExporter ----------

    @Test
    void exporterDelegatesToTheCurrentStrategy() {
        StubStrategy stub = new StubStrategy();
        List<TelemetryEvent> events = sampleEvents();
        assertEquals("STUB", new EventExporter(stub).export(events));
        assertSame(events, stub.received);
    }

    @Test
    void strategyCanBeSwitchedAtRuntime() {
        EventExporter exporter = new EventExporter(new CsvExportStrategy());
        assertTrue(exporter.export(sampleEvents()).startsWith("eventId,"));
        exporter.setStrategy(new JsonExportStrategy());
        assertTrue(exporter.export(sampleEvents()).startsWith("["));
    }

    @Test
    void nullStrategyIsRejectedAndThePreviousOneIsKept() {
        assertThrows(IllegalArgumentException.class, () -> new EventExporter(null));
        ExportStrategy json = new JsonExportStrategy();
        EventExporter exporter = new EventExporter(json);
        assertThrows(IllegalArgumentException.class, () -> exporter.setStrategy(null));
        assertSame(json, exporter.getStrategy());
    }

    @Test
    void nullEventListIsRejected() {
        StubStrategy stub = new StubStrategy();
        assertThrows(IllegalArgumentException.class, () -> new EventExporter(stub).export(null));
        assertNull(stub.received);
    }

    // ---------- Contract: applies to every strategy ----------

    @ParameterizedTest
    @MethodSource("allStrategies")
    void everyStrategyHandlesEmptyAndMixedEventLists(ExportStrategy strategy) {
        assertNotNull(strategy.export(List.of()));
        String out = strategy.export(sampleEvents());
        for (TelemetryEvent event : sampleEvents()) {
            assertTrue(out.contains(event.getEventType()), event.getEventType());
            assertTrue(out.contains(event.getDescription()), event.getDescription());
        }
    }

    @ParameterizedTest
    @MethodSource("allStrategies")
    void everyStrategyLeavesItsInputUnchanged(ExportStrategy strategy) {
        List<TelemetryEvent> events = new ArrayList<>(sampleEvents());
        List<TelemetryEvent> before = List.copyOf(events);
        String first = strategy.export(events);
        assertEquals(before, events);
        assertEquals(first, strategy.export(events));
    }

    @ParameterizedTest
    @MethodSource("allStrategies")
    void everyStrategyHasAUsableFileExtension(ExportStrategy strategy) {
        String ext = strategy.getFileExtension();
        assertFalse(ext.isBlank());
        assertFalse(ext.startsWith("."));
    }

    // ---------- JSON ----------

    @Test
    void jsonIsAnArrayWithOneObjectPerEventInOrder() {
        List<TelemetryEvent> events = sampleEvents();
        String json = new JsonExportStrategy().export(events);
        assertTrue(json.startsWith("[") && json.endsWith("]"));
        assertEquals(events.size(), json.split("\"eventId\"", -1).length - 1);
        int last = -1;
        for (TelemetryEvent event : events) {
            int at = json.indexOf(event.getEventId());
            assertTrue(at > last, "events out of order");
            last = at;
        }
    }

    @Test
    void jsonEmptyListIsEmptyArray() {
        assertEquals("[]", new JsonExportStrategy().export(List.of()));
    }

    @Test
    void jsonContainsTheEventFieldsWithCorrectTypes() {
        String json = new JsonExportStrategy().export(sampleEvents());
        assertTrue(json.contains("\"successful\": false"));
        assertTrue(json.contains("\"compilationTimeMs\": 120"));
        assertTrue(json.contains("\"errorLine\": 4"));
        assertTrue(json.contains("\"files\": [\"A.java\", \"B.java\"]"));
        assertTrue(json.contains("\"editFrequencyPerMin\": 2.5"));
        assertTrue(json.contains("\"oldName\": null"));
        assertTrue(json.contains("\"durationMs\": 5000"));
        assertTrue(json.contains("\"parameters\": []"));
        assertTrue(json.contains("\"userGenerated\": true"));
    }

    @Test
    void jsonEscapesQuotesBackslashesAndNewlines() {
        CompilationEvent event = new CompilationEvent(false, 1L, "say \"hi\"\\\nnext", 1, null, List.of(), true);
        String json = new JsonExportStrategy().export(List.of(event));
        assertTrue(json.contains("say \\\"hi\\\"\\\\\\nnext"));
        assertFalse(json.contains("\nnext"), "raw newline must not appear inside a string");
    }

    @Test
    void jsonNeverContainsNonFiniteNumbers() {
        EditorEvent event = new EditorEvent("typing", 1000, 1, 100L, Double.NaN, false);
        String json = new JsonExportStrategy().export(List.of(event));
        assertFalse(json.contains("NaN"));
        assertTrue(json.contains("\"editFrequencyPerMin\": null"));
    }

    // ---------- CSV ----------

    @Test
    void csvStartsWithHeaderAndHasOneRowPerEvent() {
        String[] lines = new CsvExportStrategy().export(sampleEvents()).split(System.lineSeparator());
        assertEquals("eventId,eventType,timestamp,description,details", lines[0]);
        assertEquals(sampleEvents().size() + 1, lines.length);
    }

    @Test
    void csvQuotesCommasAndDoublesEmbeddedQuotes() {
        CompilationEvent event = new CompilationEvent(false, 1L, "a,b \"q\"", 1, null, List.of(), true);
        String csv = new CsvExportStrategy().export(List.of(event));
        assertTrue(csv.contains("errorMessage=a,b \"\"q\"\""));
        assertTrue(csv.contains("\"successful=false"), "details column must be quoted because it has commas");
    }

    // ---------- Text ----------

    @Test
    void textShowsHeaderLineAndIndentedDetails() {
        CompilationEvent event = new CompilationEvent(false, 120L, "oops", 4, null, List.of("A.java"), true);
        String text = new TextExportStrategy().export(List.of(event));
        assertTrue(text.startsWith("[" + event.getTimestamp() + "] COMPILATION - Compilation failed"));
        assertTrue(text.contains("    errorMessage: oops"));
        assertTrue(text.contains("    files: A.java"));
        assertFalse(text.contains("eventId"));
    }

    @Test
    void textEmptyListIsEmptyString() {
        assertEquals("", new TextExportStrategy().export(List.of()));
    }

    // ---------- File output ----------

    @Test
    void exportToFileUsesTheStrategyExtensionAndWritesItsOutput(@TempDir Path dir) throws IOException {
        List<TelemetryEvent> events = sampleEvents();
        EventExporter exporter = new EventExporter(new JsonExportStrategy());

        Path json = exporter.exportToFile(events, dir, "session");
        assertEquals("session.json", json.getFileName().toString());
        assertEquals(exporter.export(events), Files.readString(json));

        exporter.setStrategy(new CsvExportStrategy());
        assertEquals("session.csv", exporter.exportToFile(events, dir, "session").getFileName().toString());
    }

    @Test
    void exportToFileFailsForMissingDirectory(@TempDir Path dir) {
        EventExporter exporter = new EventExporter(new JsonExportStrategy());
        assertThrows(NoSuchFileException.class,
            () -> exporter.exportToFile(sampleEvents(), dir.resolve("missing"), "x"));
    }
}

