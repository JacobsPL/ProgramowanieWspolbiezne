package org.example.events;

public class SimulationEvent {

    private final SimulationEventType type;
    private final int customerId;
    private final String target;
    private final boolean vipGroup;
    private final boolean childGroup;
    private final int groupSize;

    public SimulationEvent(SimulationEventType type, int customerId, String target) {
        this(type, customerId, target, false, false, 0);
    }

    public SimulationEvent(SimulationEventType type, int customerId, String target, boolean vipGroup, boolean childGroup) {
        this(type, customerId, target, vipGroup, childGroup, 0);
    }

    public SimulationEvent(SimulationEventType type, int customerId, String target, int groupSize) {
        this(type, customerId, target, false, false, groupSize);
    }

    public SimulationEvent(SimulationEventType type, int customerId, String target, boolean vipGroup, boolean childGroup, int groupSize) {
        this.type = type;
        this.customerId = customerId;
        this.target = target;
        this.vipGroup = vipGroup;
        this.childGroup = childGroup;
        this.groupSize = groupSize;
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

    public boolean isVipGroup() {
        return vipGroup;
    }

    public boolean hasChild() {
        return childGroup;
    }

    public int getGroupSize() {
        return groupSize;
    }
}
