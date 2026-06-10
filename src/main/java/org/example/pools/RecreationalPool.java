package org.example.pools;

import org.example.customers.CustomerGroup;

public class RecreationalPool extends SwimmingPool{
    protected final int MAX_AVG_AGE;

    public RecreationalPool(String name, int maxCapacity, int maxAvgAge) {
        super(name, maxCapacity);
        this.MAX_AVG_AGE = maxAvgAge;
        ageSum=0;
    }

    protected boolean canEnter(CustomerGroup customerGroup){
            if(MAX_AVG_AGE>=calculateAvgAge(customerGroup)
                    && hasPampersOrDoesNotRequire(customerGroup)
                    && childHasGuardianRuleMeet(customerGroup)){
                return true;
            }
        return false;
    }
}
