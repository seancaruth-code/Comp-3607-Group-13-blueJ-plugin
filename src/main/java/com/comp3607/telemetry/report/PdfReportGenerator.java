package com.comp3607.telemetry.report;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
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
            page.write("Project: "+report.getProjectName(),11,false);
            page.write("Start: "+report.getStartTime().format(FORMAT),11,false);
            page.write("End: "+report.getEndTime().format(FORMAT),11,false);
            page.write("Duration: "+formatDuration(report.getDuration().toMillis()),11,false);

            SessionStatistics stats=report.getStatistics();
            page.heading("Session overview");
            page.write("Total telemetry events: "+stats.getTotalEvents(),10,false);
            page.write("Total edits: "+stats.getTotalEdits(),10,false);

            page.heading("Active Coding Time vs. Idle Time");
            page.write("Measured from document-edit intervals; away time is not available.",9,false);
            writeActivitySummary(page,stats);

            page.heading("Compilation statistics");
            page.write("Attempts: "+stats.getCompilationAttempts(),10,false);
            page.write("Successful: "+stats.getSuccessfulCompilations(),10,false);
            page.write("Failed: "+stats.getFailedCompilations(),10,false);
            page.write("Failed compilations: "+stats.getFailedCompilations(),10,false);
            page.write(String.format(java.util.Locale.ROOT,"Average compilation time: %.1f ms",
                stats.getAverageCompilationTimeMs()),10,false);
            page.write(String.format(java.util.Locale.ROOT,"Success rate: %.1f%%",
                stats.getCompilationSuccessRate()*100),10,false);

            page.heading("Compilation error frequency");
            int totalCompilationErrors=stats.getErrorFrequency().values().stream().mapToInt(Integer::intValue).sum();
            page.write("Compilation error occurrences: "+totalCompilationErrors,10,false);
            page.write("Failed compilations: "+stats.getFailedCompilations(),10,false);
            if(stats.getErrorFrequency().isEmpty()) page.write("No compilation error messages recorded.",10,false);
            else {
                page.errorTableHeader();
                for(Map.Entry<String,Integer> e:stats.getErrorFrequency().entrySet())
                    page.errorTableRow(e.getKey(),String.valueOf(e.getValue()));
            }

            page.heading("Execution statistics");
            page.write("Program runs: "+stats.getExecutions(),10,false);
            page.write("Failed runs: "+stats.getFailedExecutions(),10,false);

            page.heading("Event counts");
            for(Map.Entry<String,Integer> e:stats.getEventCounts().entrySet())
                page.write(e.getKey()+": "+e.getValue(),10,false);

            page.heading("Class progression");
            if(stats.getClassTimeline().isEmpty()) page.write("No class lifecycle events recorded.",10,false);
            else for(SessionStatistics.ClassTimelineEntry e:stats.getClassTimeline()) {
                String name=e.className()==null||e.className().isBlank()?"Unnamed/unavailable class":e.className();
                page.write(e.time().format(FORMAT)+"  "+describeClassAction(e.action(),name),9,false);
            }

            page.heading("Method progression");
            if(stats.getMethodTimeline().isEmpty()) page.write("No method changes recorded.",10,false);
            else for(SessionStatistics.MethodTimelineEntry e:stats.getMethodTimeline()) {
                String className=e.className()==null||e.className().isBlank()?"Unnamed/unavailable class":e.className();
                String method=e.methodSignature()==null||e.methodSignature().isBlank()?"signature unavailable":e.methodSignature();
                page.write(e.time().format(FORMAT)+"  "+describeMethodAction(e.action())+"  "+className+"."+method,9,false);
            }

            page.close();
            document.getDocumentInformation().setTitle("BlueJ Telemetry - "+report.getProjectName());
            document.save(destination.toFile());
        }
        return destination;
    }

    private static void writeActivitySummary(PageWriter page,SessionStatistics stats) throws IOException {
        long active=Math.max(0,stats.getActiveTimeMs());
        long idle=Math.max(0,stats.getIdleTimeMs());
        long session=Math.max(stats.getSessionDurationMs(),active+idle);
        long unclassified=Math.max(0,stats.getUnclassifiedTimeMs());
        long denominator=Math.max(session,active+idle);
        double activePct=denominator==0?0.0:(active*100.0/denominator);
        double idlePct=denominator==0?0.0:(idle*100.0/denominator);
        double unclassifiedPct=denominator==0?0.0:(unclassified*100.0/denominator);
        page.write(String.format(java.util.Locale.ROOT,"Active: %s (%.1f%%)",formatDuration(active),activePct),10,false);
        page.write(String.format(java.util.Locale.ROOT,"Idle: %s (%.1f%%)",formatDuration(idle),idlePct),10,false);
        page.write(String.format(java.util.Locale.ROOT,"Unclassified: %s (%.1f%%)",formatDuration(unclassified),unclassifiedPct),10,false);
        page.write("Away time: unavailable (not measured)",10,false);
        page.activityBar(active,idle,unclassified);
    }

    private static String describeClassAction(String action,String className) {
        if(action==null) return "Class state unavailable: "+className;
        return switch(action.toLowerCase(java.util.Locale.ROOT)) {
            case "created","creation","actualcreation","actual_created" ->
                "Actual creation detected: "+className;
            case "opened","discovered","discovery" ->
                "Opened or discovered: "+className;
            case "renamed" -> "Renamed: "+className;
            case "removed","deleted" -> "Removed: "+className;
            case "statechanged","state_change","state-change","state" ->
                "State changed: "+className;
            default -> "Class event ("+truncate(action,30)+"): "+className;
        };
    }

    private static String describeMethodAction(String action) {
        if(action==null) return "Method event unavailable";
        return switch(action.toLowerCase(java.util.Locale.ROOT)) {
            case "discovered","discovery" -> "Method discovered";
            case "added","addition" -> "Method added";
            case "removed","deleted","removal" -> "Method removed";
            default -> "Method event ("+truncate(action,30)+")";
        };
    }

    private static String truncate(String text,int max) {
        return text.length()<=max?text:text.substring(0,max-3)+"...";
    }

    private static String formatDuration(long millis) {
        long safe=Math.max(0,millis);
        if(safe<1000) return safe+" millisecond"+(safe==1?"":"s");
        long seconds=(safe+500)/1000;
        if(seconds<60) return seconds+" second"+(seconds==1?"":"s");
        long minutes=seconds/60;
        long remainder=seconds%60;
        return minutes+" minute"+(minutes==1?"":"s")+(remainder==0?"":" "+remainder+" second"+(remainder==1?"":"s"));
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
        void errorTableHeader() throws IOException {
            ensureSpace(24);
            y-=2;
            stream.setStrokingColor(0.25f,0.25f,0.25f);
            stream.setLineWidth(0.6f);
            stream.addRect(LEFT,y-10,360,18);
            stream.stroke();
            stream.addRect(410,y-10,100,18);
            stream.stroke();
            stream.beginText();
            stream.setFont(PDType1Font.HELVETICA_BOLD,9);
            stream.newLineAtOffset(LEFT+6,y-3);
            stream.showText("Error message");
            stream.endText();
            stream.beginText();
            stream.newLineAtOffset(416,y-3);
            stream.showText("Occurrences");
            stream.endText();
            y-=22;
        }
        void errorTableRow(String message,String count) throws IOException {
            List<String> lines=wrap(message==null?"":message,50);
            if(lines.isEmpty()) lines=List.of("");
            float rowHeight=Math.max(24,(float)lines.size()*12+12);
            ensureSpace(rowHeight+4);
            stream.setStrokingColor(0.65f,0.65f,0.65f);
            stream.setLineWidth(0.4f);
            stream.addRect(LEFT,y-rowHeight+12,360,rowHeight);
            stream.stroke();
            stream.addRect(410,y-rowHeight+12,100,rowHeight);
            stream.stroke();
            for(int i=0;i<lines.size();i++){
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA,8);
                stream.newLineAtOffset(LEFT+6,y-i*12);
                stream.showText(sanitize(lines.get(i)));
                stream.endText();
            }
            stream.beginText();
            stream.setFont(PDType1Font.HELVETICA_BOLD,9);
            stream.newLineAtOffset(450,y-2);
            stream.showText(sanitize(count));
            stream.endText();
            y-=rowHeight+4;
        }
        void activityBar(long active,long idle,long unclassified) throws IOException {
            ensureSpace(75);
            y-=8;
            float chartLeft=LEFT, chartWidth=460, chartHeight=18;
            long total=active+idle+unclassified;
            float activeWidth=total==0?0:chartWidth*((float)active/total);
            float idleWidth=total==0?0:chartWidth*((float)idle/total);
            stream.addRect(chartLeft,y,activeWidth,chartHeight);
            stream.setNonStrokingColor(0.2f,0.55f,0.75f);
            stream.fill();
            stream.setNonStrokingColor(0.55f,0.55f,0.55f);
            stream.addRect(chartLeft+activeWidth,y,idleWidth,chartHeight);
            stream.fill();
            stream.setNonStrokingColor(0.78f,0.78f,0.78f);
            stream.addRect(chartLeft+activeWidth+idleWidth,y,chartWidth-activeWidth-idleWidth,chartHeight);
            stream.fill();
            stream.setNonStrokingColor(0f,0f,0f);
            y-=30;
            stream.beginText();
            stream.setFont(PDType1Font.HELVETICA,9);
            stream.newLineAtOffset(chartLeft,y);
            stream.showText("Active");
            stream.endText();
            stream.beginText();
            stream.setFont(PDType1Font.HELVETICA,9);
            stream.newLineAtOffset(chartLeft+activeWidth,y);
            stream.showText("Idle");
            stream.endText();
            stream.beginText();
            stream.setFont(PDType1Font.HELVETICA,9);
            stream.newLineAtOffset(chartLeft+activeWidth+idleWidth,y);
            stream.showText("Unclassified");
            stream.endText();
            y-=16;
        }
        private static List<String> wrap(String text,int maxChars) {
            List<String> result=new ArrayList<>();
            if(text==null||text.isEmpty()){result.add("");return result;}
            int start=0;
            while(start<text.length()){
                int end=Math.min(start+maxChars,text.length());
                if(end<text.length()){
                    int space=text.lastIndexOf(' ',end);
                    if(space>start) end=space;
                }
                result.add(text.substring(start,end).trim());
                start=end;
                while(start<text.length()&&Character.isWhitespace(text.charAt(start)))start++;
            }
            return result;
        }
        private void ensureSpace(float needed) throws IOException {
            if(y-needed<BOTTOM) newPage();
        }
        @Override public void close() throws IOException {
            if(stream!=null) {stream.close(); stream=null;}
        }
    }
}
