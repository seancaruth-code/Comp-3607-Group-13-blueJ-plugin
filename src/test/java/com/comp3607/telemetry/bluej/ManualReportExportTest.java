package com.comp3607.telemetry.bluej;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import com.comp3607.telemetry.events.ProjectEvent;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ManualReportExportTest {
    @Test
    void exportsValidEmptyJsonAndAddsExtension(@TempDir Path dir) throws Exception {
        BlueJExtension extension = new BlueJExtension();
        LocalDateTime start = LocalDateTime.now();
        extension.setSessionStartTime(start);

        Path saved = extension.exportJsonReport(dir.resolve("empty-report"));

        assertEquals(".json", saved.getFileName().toString().substring(saved.getFileName().toString().length() - 5));
        assertTrue(Files.exists(saved));
        assertTrue(Files.size(saved) > 0);
        String json = Files.readString(saved, StandardCharsets.UTF_8);
        assertTrue(json.trim().startsWith("{"));
        assertFalse(json.toLowerCase(java.util.Locale.ROOT).contains("studentid"));
        assertTrue(json.contains("\"totalCompilationAttempts\""));
        assertTrue(json.contains("\"awayTimeMs\": \"0\"") || json.contains("\"awayTimeMs\": 0"));
        assertEquals(start, extension.getSessionStartTime());
        assertTrue(extension.getSessionEventsSnapshot().isEmpty());
    }

    @Test
    void exportsPdfAndPreservesEventsAndSessionStart(@TempDir Path dir) throws Exception {
        BlueJExtension extension = new BlueJExtension();
        LocalDateTime start = LocalDateTime.now().minusMinutes(5);
        extension.setSessionStartTime(start);
        extension.publishSessionEvent(new ProjectEvent("opened", "Demo"));
        int eventCount = extension.getSessionEventsSnapshot().size();

        Path saved = extension.exportPdfReport(dir.resolve("manual-report"));

        assertTrue(saved.getFileName().toString().endsWith(".pdf"));
        assertTrue(Files.exists(saved));
        assertTrue(Files.size(saved) > 100);
        try (PDDocument document = PDDocument.load(saved.toFile())) {
            String pdfText = new PDFTextStripper().getText(document);
            assertFalse(pdfText.toLowerCase(java.util.Locale.ROOT).contains("student id"));
            assertTrue(pdfText.contains("Project:"));
        }
        assertEquals(start, extension.getSessionStartTime());
        assertEquals(eventCount, extension.getSessionEventsSnapshot().size());
    }

    @Test
    void cancellationIsRepresentedByNullDestination() throws Exception {
        BlueJExtension extension = new BlueJExtension();
        assertNull(extension.exportJsonReport(null));
        assertNull(extension.exportPdfReport(null));
    }

    @Test
    void reportsFileWritingErrors(@TempDir Path dir) {
        BlueJExtension extension = new BlueJExtension();
        Path blockingFile = dir.resolve("not-a-directory");
        try {
            Files.writeString(blockingFile, "blocking");
        } catch (Exception exception) {
            fail(exception);
        }
        assertThrows(java.io.IOException.class,
            () -> extension.exportJsonReport(blockingFile.resolve("report.json")));
        assertThrows(java.io.IOException.class,
            () -> extension.exportPdfReport(blockingFile.resolve("report.pdf")));
    }

    @Test
    void automaticReportGenerationStillRuns(@TempDir Path dir) throws Exception {
        BlueJExtension extension = new BlueJExtension();
        extension.setSessionStartTime(LocalDateTime.now().minusMinutes(1));
        extension.publishSessionEvent(new ProjectEvent("opened", "Demo"));
        String previous = System.getProperty("bluej.telemetry.output");
        System.setProperty("bluej.telemetry.output", dir.toString());
        try {
            extension.exportSessionReport();
            assertTrue(Files.list(dir).anyMatch(path -> path.getFileName().toString().endsWith(".json")));
            assertTrue(Files.list(dir).anyMatch(path -> path.getFileName().toString().endsWith(".pdf")));
            assertNull(extension.getSessionStartTime());
        } finally {
            if (previous == null) {
                System.clearProperty("bluej.telemetry.output");
            } else {
                System.setProperty("bluej.telemetry.output", previous);
            }
        }
    }
}
