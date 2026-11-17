package com.comp3607.telemetry.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

/** Generates a multi-page PDF summary for one telemetry session. */
public final class PdfReportGenerator {
    private static final DateTimeFormatter FORMAT=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Path generate(SessionReport report, Path destination) throws IOException {
        if(report==null || destination==null)
            throw new IllegalArgumentException("Report and destination are required");
        Path parent=destination.toAbsolutePath().getParent();
        if(parent!=null) Files.createDirectories(parent);
        try(PDDocument document=new PDDocument()) {
            PageWriter page=new PageWriter(document);
            page.write("BlueJ Telemetry Session Report",18,true);
            page.write("Student ID: "+report.getStudentId(),11,false);
            page.write("Project: "+report.getProjectName(),11,false);
            page.write("Start: "+report.getStartTime().format(FORMAT),11,false);
            page.write("End: "+report.getEndTime().format(FORMAT),11,false);
            page.write("Duration: "+report.getDuration().toMinutes()+" minutes",11,false);

            SessionStatistics stats=report.getStatistics();
            page.heading("Session overview");
            page.write("Total telemetry events: "+stats.getTotalEvents(),10,false);
            page.write(String.format(java.util.Locale.ROOT,"Active editing time: %d ms (%.1f%%)",
                stats.getActiveTimeMs(),stats.getActiveRatio()*100),10,false);
            page.write(String.format(java.util.Locale.ROOT,"Idle time: %d ms (%.1f%%)",
                stats.getIdleTimeMs(),(1-stats.getActiveRatio())*100),10,false);
            page.write("Total edits: "+stats.getTotalEdits(),10,false);

            page.heading("Compilation statistics");
            page.write("Attempts: "+stats.getCompilationAttempts(),10,false);
            page.write("Successful: "+stats.getSuccessfulCompilations(),10,false);
            page.write("Failed: "+stats.getFailedCompilations(),10,false);
            page.write(String.format(java.util.Locale.ROOT,"Success rate: %.1f%%",
                stats.getCompilationSuccessRate()*100),10,false);

            page.heading("Compilation error frequency");
            if(stats.getErrorFrequency().isEmpty()) page.write("No compilation errors recorded.",10,false);
            else for(Map.Entry<String,Integer> e:stats.getErrorFrequency().entrySet())
                page.write(truncate(e.getKey(),100)+" : "+e.getValue(),10,false);

            page.heading("Execution statistics");
            page.write("Program runs: "+stats.getExecutions(),10,false);
            page.write("Failed runs: "+stats.getFailedExecutions(),10,false);

            page.heading("Event counts");
            for(Map.Entry<String,Integer> e:stats.getEventCounts().entrySet())
                page.write(e.getKey()+": "+e.getValue(),10,false);

            page.heading("Class progression");
            if(stats.getClassTimeline().isEmpty()) page.write("No class lifecycle events recorded.",10,false);
            else for(SessionStatistics.ClassTimelineEntry e:stats.getClassTimeline())
                page.write(e.time().format(FORMAT)+"  "+e.action()+"  "+e.className(),9,false);

            page.heading("Method progression");
            if(stats.getMethodTimeline().isEmpty()) page.write("No method changes recorded.",10,false);
            else for(SessionStatistics.MethodTimelineEntry e:stats.getMethodTimeline())
                page.write(e.time().format(FORMAT)+"  "+e.action()+"  "+e.className()+"  "+e.methodSignature(),9,false);

            page.close();
            document.getDocumentInformation().setTitle("BlueJ Telemetry - "+report.getProjectName());
            document.save(destination.toFile());
        }
        return destination;
    }

    private static String truncate(String text,int max) {
        return text.length()<=max?text:text.substring(0,max-3)+"...";
    }

    private static String sanitize(String text) {
        StringBuilder out=new StringBuilder();
        text.codePoints().forEach(c->out.append(c>=32&&c<=255?(char)c:'?'));
        return out.toString();
    }

    /** Owns page creation and automatically continues long reports on another page. */
    private static final class PageWriter implements AutoCloseable {
        private static final float LEFT=50, TOP=790, BOTTOM=50;
        private final PDDocument document;
        private PDPageContentStream stream;
        private float y;

        PageWriter(PDDocument document) throws IOException {
            this.document=document;
            newPage();
        }
        private void newPage() throws IOException {
            if(stream!=null) stream.close();
            PDPage page=new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream=new PDPageContentStream(document,page);
            y=TOP;
        }
        void heading(String value) throws IOException {
            ensureSpace(32);
            y-=5;
            write(value,14,true);
        }
        void write(String value,float size,boolean bold) throws IOException {
            ensureSpace(20);
            stream.beginText();
            stream.setFont(bold?PDType1Font.HELVETICA_BOLD:PDType1Font.HELVETICA,size);
            stream.newLineAtOffset(LEFT,y);
            stream.showText(sanitize(value));
            stream.endText();
            y-=size+7;
        }
        private void ensureSpace(float needed) throws IOException {
            if(y-needed<BOTTOM) newPage();
        }
        @Override public void close() throws IOException {
            if(stream!=null) {stream.close(); stream=null;}
        }
    }
}
