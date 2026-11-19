package com.comp3607.telemetry.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import com.comp3607.telemetry.strategy.JsonExportStrategy;

/** Writes session metadata, summary metrics and the complete event list as JSON. */
public final class SessionJsonReportGenerator {
    public Path generate(SessionReport report, Path destination) throws IOException {
        if(report==null || destination==null) throw new IllegalArgumentException("Report and destination are required");
        Path parent=destination.toAbsolutePath().getParent();
        if(parent!=null) Files.createDirectories(parent);
        SessionStatistics s=report.getStatistics();
        StringBuilder b=new StringBuilder("{\n  \"session\": {\n");
        field(b,"projectName",quote(report.getProjectName()),true);
        field(b,"startTime",quote(report.getStartTime().toString()),true);
        field(b,"endTime",quote(report.getEndTime().toString()),true);
        field(b,"durationSeconds",String.valueOf(report.getDuration().toSeconds()),false);
        b.append("  },\n  \"summary\": {\n");
        field(b,"totalEvents",String.valueOf(s.getTotalEvents()),true);
        field(b,"totalCompilationAttempts",String.valueOf(s.getCompilationAttempts()),true);
        field(b,"averageCompilationTimeMs",String.valueOf(s.getAverageCompilationTimeMs()),true);
        field(b,"totalSyntaxErrors",String.valueOf(s.getFailedCompilations()),true);
        field(b,"successfulCompilations",String.valueOf(s.getSuccessfulCompilations()),true);
        field(b,"failedCompilations",String.valueOf(s.getFailedCompilations()),true);
        field(b,"compilationSuccessRate",String.valueOf(s.getCompilationSuccessRate()),true);
        field(b,"compilationErrorRatio",String.valueOf(s.getCompilationErrorRatio()),true);
        field(b,"totalEdits",String.valueOf(s.getTotalEdits()),true);
        field(b,"activeTimeMs",String.valueOf(s.getActiveTimeMs()),true);
        field(b,"idleTimeMs",String.valueOf(s.getIdleTimeMs()),true);
        field(b,"inactiveTimeMs",String.valueOf(s.getInactiveTimeMs()),true);
        field(b,"typingTimeMs",String.valueOf(s.getTypingTimeMs()),true);
        field(b,"awayTimeMs",String.valueOf(s.getAwayTimeMs()),true);
        field(b,"unclassifiedTimeMs",String.valueOf(s.getUnclassifiedTimeMs()),true);
        field(b,"activeRatio",String.valueOf(s.getActiveRatio()),true);
        field(b,"sessionDurationMs",String.valueOf(s.getSessionDurationMs()),true);
        field(b,"executions",String.valueOf(s.getExecutions()),true);
        field(b,"failedExecutions",String.valueOf(s.getFailedExecutions()),false);
        b.append("  },\n  \"eventCounts\": {");
        int i=0; for(Map.Entry<String,Integer> e:s.getEventCounts().entrySet()){
            if(i++>0)b.append(',');
            b.append("\n    ").append(quote(e.getKey())).append(": ").append(e.getValue());
        }
        if(i>0)b.append('\n');
        b.append("  },\n  \"errorFrequency\": {");
        i=0; for(Map.Entry<String,Integer> e:s.getErrorFrequency().entrySet()){
            if(i++>0)b.append(',');
            b.append("\n    ").append(quote(e.getKey())).append(": ").append(e.getValue());
        }
        if(i>0)b.append('\n');
        b.append("  },\n  \"classTimeline\": [");
        for(int c=0;c<s.getClassTimeline().size();c++){
            SessionStatistics.ClassTimelineEntry entry=s.getClassTimeline().get(c);
            if(c>0)b.append(',');
            b.append("\n    {\"timestamp\": ").append(quote(entry.time().toString()))
             .append(", \"action\": ").append(quote(entry.action()))
             .append(", \"className\": ").append(quote(entry.className())).append('}');
        }
        if(!s.getClassTimeline().isEmpty())b.append('\n');
        b.append("  ],\n  \"methodTimeline\": [");
        for(int m=0;m<s.getMethodTimeline().size();m++){
            SessionStatistics.MethodTimelineEntry entry=s.getMethodTimeline().get(m);
            if(m>0)b.append(',');
            b.append("\n    {\"timestamp\": ").append(quote(entry.time().toString()))
             .append(", \"action\": ").append(quote(entry.action()))
             .append(", \"className\": ").append(quote(entry.className()))
             .append(", \"methodSignature\": ").append(quote(entry.methodSignature())).append('}');
        }
        if(!s.getMethodTimeline().isEmpty())b.append('\n');
        b.append("  ],\n  \"events\": ").append(new JsonExportStrategy().export(report.getEvents())).append("\n}\n");
        Files.writeString(destination,b.toString(),StandardCharsets.UTF_8);
        return destination;
    }
    private static void field(StringBuilder b,String key,String value,boolean comma){
        b.append("    ").append(quote(key)).append(": ").append(value);
        if(comma)b.append(',');
        b.append('\n');
    }
    private static String quote(String s){
        StringBuilder out=new StringBuilder("\"");
        for(char c:s.toCharArray()){
            switch(c){
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> { if(c<0x20) out.append(String.format("\\u%04x",(int)c)); else out.append(c); }
            }
        }
        return out.append('"').toString();
    }
}
