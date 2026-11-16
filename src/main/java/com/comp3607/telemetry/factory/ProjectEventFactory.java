package com.comp3607.telemetry.factory;

import java.util.Objects;

import com.comp3607.telemetry.events.ProjectEvent;

/**
 * Creates project lifecycle events.
 */
public class ProjectEventFactory extends TelemetryEventFactory {

    private final String action;
    private final String itemName;

    public ProjectEventFactory(String action, String itemName) {
        this.action = Objects.requireNonNull(action, "Action cannot be null");
        this.itemName = Objects.requireNonNull(itemName, "Item name cannot be null");
    }

    @Override
    public ProjectEvent createEvent() {
        return new ProjectEvent(action, itemName);
    }
}
