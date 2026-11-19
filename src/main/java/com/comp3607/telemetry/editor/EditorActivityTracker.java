package com.comp3607.telemetry.editor;

import java.time.Duration;
import java.time.Instant;
import com.comp3607.telemetry.events.EditorEvent;
import com.comp3607.telemetry.factory.EditorEventFactory;

/**
 * Estimates active and idle editor intervals from document-replacement callbacks.
 * BlueJ's callback does not expose individual keystrokes or keyboard focus.
 */
public class EditorActivityTracker {
    private final long idleThresholdMillis;
    private Instant firstEdit;
    private Instant lastEdit;
    private int editCount;
    private boolean sessionEnded;

    public EditorActivityTracker(long idleThresholdMillis) {
        if(idleThresholdMillis<0) throw new IllegalArgumentException("Idle threshold cannot be negative");
        this.idleThresholdMillis=idleThresholdMillis;
    }

    public synchronized EditorEvent recordEdit(Instant timestamp) {
        if (sessionEnded) {
            throw new IllegalStateException("The editor activity session has ended");
        }
        if(timestamp==null) throw new IllegalArgumentException("Timestamp cannot be null");
        long interval=lastEdit==null?0:Math.max(0,Duration.between(lastEdit,timestamp).toMillis());
        Long cadence=lastEdit==null?null:interval;
        boolean idle=lastEdit!=null && interval>=idleThresholdMillis;
        if(firstEdit==null) firstEdit=timestamp;
        lastEdit=timestamp;
        editCount++;
        long elapsed=Math.max(1,Duration.between(firstEdit,timestamp).toMillis());
        double frequency=lastEdit.equals(firstEdit) ? 0.0 : editCount*60_000.0/elapsed;
        return new EditorEventFactory("typing",interval,1,cadence,frequency,idle).createEvent();
    }

    /**
     * Ends the session without assuming that time before the first edit was typing.
     * If the session ends before another edit, only the interval from the final edit
     * to the session end is classified; otherwise no interval is added.
     */
    public synchronized EditorEvent endSession(Instant timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        if (sessionEnded) {
            return null;
        }
        sessionEnded = true;
        if (lastEdit == null) {
            return null;
        }
        long interval = Math.max(0, Duration.between(lastEdit, timestamp).toMillis());
        if (interval == 0) {
            return null;
        }
        boolean idle = interval >= idleThresholdMillis;
        double elapsed = Math.max(1, Duration.between(firstEdit, timestamp).toMillis());
        double frequency = elapsed == 0.0 ? 0.0 : editCount * 60_000.0 / elapsed;
        return new EditorEventFactory("typing", interval, 0, interval, frequency, idle).createEvent();
    }
}
