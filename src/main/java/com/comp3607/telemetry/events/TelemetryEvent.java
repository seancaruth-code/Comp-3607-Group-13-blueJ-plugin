
package com.comp3607.telemetry.events;

import java.time.LocalDateTime;
import java.util.UUID;

public class TelemetryEvent {

    private final String eventId;
    private final String eventType;
    private final LocalDateTime timestamp;
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
