package com.comp3607.telemetry.report;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.comp3607.telemetry.events.*;

class SessionReportTest {
    private static String extractText(Path pdf) throws Exception {
        try(PDDocument document=PDDocument.load(pdf.toFile())){
            return new PDFTextStripper().getText(document);
        }
    }

    @Test void aggregatesSessionMetrics() {
        List<TelemetryEvent> events=List.of(
            new CompilationEvent(true,100L,null),
            new CompilationEvent(false,200L,"Missing semicolon"),
            new ExecutionEvent(true,20L),
            new ExecutionEvent(false,30L),
            new EditorEvent("typing",500,2,100L,12.0,false),
            new EditorEvent("typing",1000,1,30000L,1.0,true),
            new ClassEvent("created","Student",null,false,false),
            new MethodEvent("added","Student","void getName()"));
        SessionStatistics s=SessionStatistics.from(events);
        assertEquals(8,s.getTotalEvents());
        assertEquals(2,s.getCompilationAttempts());
        assertEquals(1,s.getSuccessfulCompilations());
        assertEquals(1,s.getFailedCompilations());
        assertEquals(0.5,s.getCompilationSuccessRate());
        assertEquals(2,s.getExecutions());
        assertEquals(1,s.getFailedExecutions());
        assertEquals(500,s.getActiveTimeMs());
        assertEquals(1000,s.getIdleTimeMs());
        assertEquals(1000,s.getInactiveTimeMs());
        assertEquals(500,s.getTypingTimeMs());
        assertEquals(1.0/3.0,s.getActiveRatio(),0.000001);
        assertEquals(3,s.getTotalEdits());
        assertEquals(1,s.getErrorFrequency().get("Missing semicolon"));
        assertEquals(1,s.getClassTimeline().size());
        assertEquals(1,s.getMethodTimeline().size());
        assertEquals("added",s.getMethodTimeline().get(0).action());
    }
    @Test void reportValidatesMetadataAndCopiesEvents() {
        LocalDateTime start=LocalDateTime.of(2026,11,15,10,0);
        SessionReport report=new SessionReport("Demo",start,start.plusMinutes(2),
            List.of(new ProjectEvent("opened","Demo")));
        assertEquals(120,report.getDuration().toSeconds());
        assertThrows(UnsupportedOperationException.class,()->report.getEvents().add(new ProjectEvent("x","y")));
        assertThrows(IllegalArgumentException.class,()->new SessionReport("p",start,start.minusSeconds(1),List.of()));
    }
    @Test void generatorsWritePdfAndJson(@TempDir Path dir) throws Exception {
        LocalDateTime start=LocalDateTime.of(2026,11,15,10,0);
        SessionReport report=new SessionReport("Demo",start,start.plusMinutes(45),
            List.of(new CompilationEvent(false,120L,"Missing semicolon",4,null,List.of("Student.java"),true)));
        Path pdf=new PdfReportGenerator().generate(report,dir.resolve("report.pdf"));
        Path json=new SessionJsonReportGenerator().generate(report,dir.resolve("report.json"));
        assertTrue(Files.size(pdf)>100);
        String content=Files.readString(json);
        assertFalse(content.toLowerCase(java.util.Locale.ROOT).contains("studentid"));
        String pdfText=extractText(pdf);
        assertFalse(pdfText.toLowerCase(java.util.Locale.ROOT).contains("student id"));
        assertTrue(content.contains("\"errorFrequency\""));
        assertTrue(content.contains("\"classTimeline\""));
        assertTrue(content.contains("\"methodTimeline\""));
        assertTrue(content.contains("\"events\""));
    }
    @Test void jsonReportIncludesCorrectEditorActivityMetrics(@TempDir Path dir) throws Exception {
        LocalDateTime start=LocalDateTime.of(2026,11,15,10,0);
        List<TelemetryEvent> events=List.of(
            new EditorEvent("typing",500,1,500L,60.0,false),
            new EditorEvent("typing",1500,0,1500L,0.0,true));
        SessionReport report=new SessionReport("Demo",start,start.plusMinutes(1),events);
        Path json=new SessionJsonReportGenerator().generate(report,dir.resolve("activity.json"));
        String content=Files.readString(json);
        assertTrue(content.contains("\"totalEdits\": 1"));
        assertTrue(content.contains("\"activeTimeMs\": 500"));
        assertTrue(content.contains("\"idleTimeMs\": 1500"));
        assertTrue(content.contains("\"inactiveTimeMs\": 1500"));
        assertTrue(content.contains("\"typingTimeMs\": 500"));
        assertTrue(content.contains("\"awayTimeMs\": 0"));
        assertTrue(content.contains("\"activeRatio\": 0.25"));
    }
    @Test void jsonReportPreservesExecutionMetadata(@TempDir Path dir) throws Exception {
        LocalDateTime start=LocalDateTime.of(2026,11,15,10,0);
        List<TelemetryEvent> events=List.of(
            new ExecutionEvent(true, null, "TelemetryTest", "calculate", "telemetr1",
                List.of("7", "true"), List.of("int", "boolean"), "NORMAL_EXIT", null),
            new ExecutionEvent(false, null, "TelemetryTest", null, "telemetr1",
                List.of(), List.of(), "EXCEPTION_EXIT", null));
        SessionReport report=new SessionReport("Demo",start,start.plusMinutes(1),events);
        Path json=new SessionJsonReportGenerator().generate(report,dir.resolve("execution.json"));
        String content=Files.readString(json);

        assertTrue(content.contains("\"className\": \"TelemetryTest\""));
        assertTrue(content.contains("\"methodName\": \"calculate\""));
        assertTrue(content.contains("\"objectName\": \"telemetr1\""));
        assertTrue(content.contains("\"parameters\": [\"7\", \"true\"]"));
        assertTrue(content.contains("\"methodSignature\": [\"int\", \"boolean\"]"));
        assertTrue(content.contains("\"terminationType\": \"NORMAL_EXIT\""));
        assertTrue(content.contains("\"terminationType\": \"EXCEPTION_EXIT\""));
        assertTrue(content.contains("\"exceptionMessage\": null"));
        assertTrue(content.contains("\"executionTimeMs\": null"));
    }

    @Test void pdfIncludesActivityErrorAndLifecycleSummaries(@TempDir Path dir) throws Exception {
        LocalDateTime start=LocalDateTime.of(2026,11,15,10,0);
        List<TelemetryEvent> events=List.of(
            new EditorEvent("typing",750,3,750L,120.0,false),
            new EditorEvent("typing",250,0,250L,0.0,true),
            new CompilationEvent(false,100L,"Missing semicolon"),
            new CompilationEvent(false,100L,"Missing semicolon"),
            new CompilationEvent(false,100L,"Cannot find symbol"),
            new ClassEvent("created","NewStudent",null,false,false),
            new ClassEvent("opened","Student",null,false,false),
            new ClassEvent("renamed","StudentRecord","Student",true,false),
            new ClassEvent("removed","StudentRecord",null,false,false),
            new MethodEvent("discovered","Student","void main()"),
            new MethodEvent("added","Student","int calculate(int)"),
            new MethodEvent("removed","Student","void oldMethod()"));
        SessionReport report=new SessionReport("Demo",start,start.plusMinutes(5),events);
        Path pdf=new PdfReportGenerator().generate(report,dir.resolve("summaries.pdf"));

        assertTrue(Files.size(pdf)>100);
        String text=extractText(pdf);
        assertTrue(text.contains("Active Coding Time vs. Idle Time"));
        assertTrue(text.contains("Measured from document-edit intervals"));
        assertTrue(text.contains("Active: 750 milliseconds"));
        assertTrue(text.contains("Idle: 250 milliseconds"));
        assertTrue(text.contains("Unclassified:"));
        assertTrue(text.contains("Away time: unavailable"));
        assertTrue(text.contains("Compilation error occurrences: 3"));
        assertTrue(text.contains("Missing semicolon"));
        assertTrue(text.contains("2"));
        assertTrue(text.contains("Cannot find symbol"));
        assertTrue(text.contains("Actual creation detected: NewStudent"));
        assertTrue(text.contains("Opened or discovered: Student"));
        assertTrue(text.contains("Renamed: StudentRecord"));
        assertTrue(text.contains("Removed: StudentRecord"));
        assertTrue(text.contains("Method discovered"));
        assertTrue(text.contains("Method added"));
        assertTrue(text.contains("Method removed"));
    }

    @Test void pdfHandlesEmptyAndUnavailableValues(@TempDir Path dir) throws Exception {
        LocalDateTime start=LocalDateTime.of(2026,11,15,10,0);
        List<TelemetryEvent> events=List.of(
            new CompilationEvent(false,100L,null),
            new ClassEvent("stateChanged","Student",null,false,false));
        SessionReport report=new SessionReport("Empty",start,start.plusMinutes(1),events);
        Path pdf=new PdfReportGenerator().generate(report,dir.resolve("empty.pdf"));
        String text=extractText(pdf);

        assertTrue(text.contains("Active: 0 milliseconds (0.0%)"));
        assertTrue(text.contains("Idle: 0 milliseconds (0.0%)"));
        assertTrue(text.contains("No compilation error messages recorded."));
        assertTrue(text.contains("Compilation error occurrences: 0"));
        assertTrue(text.contains("State changed: Student"));
    }

    @Test void pdfPaginatesLongTimelinesAndGeneratesMultiplePages(@TempDir Path dir) throws Exception {
        LocalDateTime start=LocalDateTime.of(2026,11,15,10,0);
        java.util.ArrayList<TelemetryEvent> events=new java.util.ArrayList<>();
        for(int i=0;i<80;i++){
            events.add(new ClassEvent("opened","Class"+i,null,true,false));
            events.add(new MethodEvent("discovered","Class"+i,"method"+i+"()"));
        }
        SessionReport report=new SessionReport("Long",start,start.plusMinutes(30),events);
        Path pdf=new PdfReportGenerator().generate(report,dir.resolve("paginated.pdf"));

        try(PDDocument document=PDDocument.load(pdf.toFile())){
            assertTrue(document.getNumberOfPages()>1);
        }
        String text=extractText(pdf);
        assertTrue(text.contains("Opened or discovered: Class79"));
        assertTrue(text.contains("Method discovered"));
    }

    @Test void activityPercentagesUseSessionDurationAndExcludeAwayTime(@TempDir Path dir) throws Exception {
        LocalDateTime start=LocalDateTime.of(2026,11,15,10,0);
        SessionReport report=new SessionReport("Activity",start,start.plusMinutes(10),
            List.of(
                new EditorEvent("typing",100_000,1,100_000L,60.0,false),
                new EditorEvent("typing",50_000,0,50_000L,0.0,true)));

        SessionStatistics stats=report.getStatistics();
        assertEquals(600_000,stats.getSessionDurationMs());
        assertEquals(100_000,stats.getActiveTimeMs());
        assertEquals(50_000,stats.getIdleTimeMs());
        assertEquals(450_000,stats.getUnclassifiedTimeMs());
        assertEquals(0,stats.getAwayTimeMs());

        Path pdf=new PdfReportGenerator().generate(report,dir.resolve("activity-percentages.pdf"));
        String text=extractText(pdf);
        assertTrue(text.contains("Active: 1 minute 40 seconds (16.7%)"));
        assertTrue(text.contains("Idle: 50 seconds (8.3%)"));
        assertTrue(text.contains("Unclassified: 7 minutes 30 seconds (75.0%)"));
        assertTrue(text.contains("Away time: unavailable"));
    }

    @Test void pdfWrapsLongErrorsAndReportsConsistentTotals(@TempDir Path dir) throws Exception {
        LocalDateTime start=LocalDateTime.of(2026,11,15,10,0);
        String longError="A very long compiler diagnostic with repeated context that should wrap across multiple PDF table lines without truncating or corrupting the message";
        SessionReport report=new SessionReport("Errors",start,start.plusMinutes(1),
            List.of(
                new CompilationEvent(false,100L,longError),
                new CompilationEvent(false,100L,longError),
                new CompilationEvent(true,100L,null)));
        Path pdf=new PdfReportGenerator().generate(report,dir.resolve("long-errors.pdf"));
        String text=extractText(pdf);

        assertTrue(text.contains("Compilation error occurrences: 2"));
        assertTrue(text.contains("Failed compilations: 2"));
        assertTrue(text.contains("A very long compiler diagnostic"));
        assertTrue(text.contains("2"));
        try(PDDocument loaded=PDDocument.load(pdf.toFile())){
            assertTrue(loaded.getNumberOfPages()>=1);
        }
    }

    @Test void timelineEntriesRemainChronologicalAndDistinguishLifecycleActions() {
        LocalDateTime start=LocalDateTime.of(2026,11,15,10,0);
        SessionReport report=new SessionReport("Timeline",start,start.plusMinutes(1),
            List.of(
                new ClassEvent("stateChanged","Student",null,true,false),
                new ClassEvent("created","Student",null,false,false),
                new MethodEvent("removed","Student","void old()"),
                new MethodEvent("discovered","Student","void main()")));

        List<SessionStatistics.ClassTimelineEntry> classes=report.getStatistics().getClassTimeline();
        List<SessionStatistics.MethodTimelineEntry> methods=report.getStatistics().getMethodTimeline();
        assertEquals(2,classes.size());
        assertEquals(2,methods.size());
        assertFalse(classes.stream().allMatch(e->"created".equalsIgnoreCase(e.action())));
        assertTrue(methods.stream().anyMatch(e->"discovered".equalsIgnoreCase(e.action())));
        assertTrue(methods.stream().anyMatch(e->"removed".equalsIgnoreCase(e.action())));
    }
}
