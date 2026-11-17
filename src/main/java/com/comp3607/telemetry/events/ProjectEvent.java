
package com.comp3607.telemetry.events;

/**
 * Represents a BlueJ project lifecycle action.
 */
public class ProjectEvent extends TelemetryEvent {

    /** Lifecycle action represented by the event. */

    private final String action;
    /** Project item associated with the action. */
    private final String itemName;

    public ProjectEvent(String action, String itemName) {


        super("PROJECT",action + ": " + itemName);

        this.action = action;
        this.itemName = itemName;
    }

    public String getAction() {
        return action;
    }

    public String getItemName() {
        return itemName;
    }
}
