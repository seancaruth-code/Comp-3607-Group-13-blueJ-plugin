
package com.comp3607.telemetry.events;

public class ProjectEvent extends TelemetryEvent {

    private final String action;
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
