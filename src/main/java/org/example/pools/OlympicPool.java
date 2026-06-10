package org.example.pools;

import org.example.customers.CustomerGroup;

public class OlympicPool extends SwimmingPool{
    public OlympicPool(String name, int maxCapacity) {
        super(name, maxCapacity);
    }

    @Override
    protected boolean canEnter(CustomerGroup customerGroup) {
            if(customerGroup.allAdults()){
                return true;
            }
        return false;
    }
}
