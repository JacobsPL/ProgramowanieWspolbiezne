package org.example.pools;

import org.example.customers.CustomerGroup;

public class PaddlingPool extends SwimmingPool{
    public PaddlingPool(String name, int maxCapacity) {
        super(name, maxCapacity);
    }

    @Override
    protected boolean canEnter(CustomerGroup customerGroup) {
            if(customerGroup.hasChildUnder5()
                    && customerGroup.hasAdult()
                    && customerGroup.hasNoChildNotFitForPaddling()
                    && hasPampersOrDoesNotRequire(customerGroup)) {
                return true;
            }
        return false;
    }
}
