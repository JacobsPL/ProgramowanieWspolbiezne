package org.example.events;

public class SimulationEvent {

    private final SimulationEventType type;
    private final int customerId;

    private final String target;

    public SimulationEvent(SimulationEventType type, int customerId, String target) {
        this.type = type;
        this.customerId = customerId;
        this.target = target;
    }

    public SimulationEventType getType(){
        return this.type;
    }

    public int getCustomerId(){
        return customerId;
    }
    public String getTarget() {
        return target;
    }
}
