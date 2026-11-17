
package com.comp3607.telemetry.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base type shared by every timestamped telemetry event.
 */
public class TelemetryEvent {

    /** Unique identifier assigned to this event. */

    private final String eventId;
    /** Category of the telemetry event. */
    private final String eventType;
    /** Local date and time when the event was created. */
    private final LocalDateTime timestamp;
    /** Human-readable summary of the event. */
    private final String description;

    public TelemetryEvent(String eventType, String description) {

        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
        this.description = description;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }
}
