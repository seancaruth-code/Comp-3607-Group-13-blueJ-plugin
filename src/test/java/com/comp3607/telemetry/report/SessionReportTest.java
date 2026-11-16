package com.comp3607.telemetry.report;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.comp3607.telemetry.events.*;

class SessionReportTest {
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
        assertEquals(3,s.getTotalEdits());
        assertEquals(1,s.getErrorFrequency().get("Missing semicolon"));
        assertEquals(1,s.getClassTimeline().size());
        assertEquals(1,s.getMethodTimeline().size());
        assertEquals("added",s.getMethodTimeline().get(0).action());
    }
    @Test void reportValidatesMetadataAndCopiesEvents() {
        LocalDateTime start=LocalDateTime.of(2026,11,15,10,0);
        SessionReport report=new SessionReport("student","Demo",start,start.plusMinutes(2),
            List.of(new ProjectEvent("opened","Demo")));
        assertEquals(120,report.getDuration().toSeconds());
        assertThrows(UnsupportedOperationException.class,()->report.getEvents().add(new ProjectEvent("x","y")));
        assertThrows(IllegalArgumentException.class,()->new SessionReport("s","p",start,start.minusSeconds(1),List.of()));
    }
    @Test void generatorsWritePdfAndJson(@TempDir Path dir) throws Exception {
        LocalDateTime start=LocalDateTime.of(2026,11,15,10,0);
        SessionReport report=new SessionReport("STUDENT_ID_PLACEHOLDER","Demo",start,start.plusMinutes(45),
            List.of(new CompilationEvent(false,120L,"Missing semicolon",4,null,List.of("Student.java"),true)));
        Path pdf=new PdfReportGenerator().generate(report,dir.resolve("report.pdf"));
        Path json=new SessionJsonReportGenerator().generate(report,dir.resolve("report.json"));
        assertTrue(Files.size(pdf)>100);
        String content=Files.readString(json);
        assertTrue(content.contains("\"studentId\": \"STUDENT_ID_PLACEHOLDER\""));
        assertTrue(content.contains("\"errorFrequency\""));
        assertTrue(content.contains("\"classTimeline\""));
        assertTrue(content.contains("\"methodTimeline\""));
        assertTrue(content.contains("\"events\""));
    }
}
