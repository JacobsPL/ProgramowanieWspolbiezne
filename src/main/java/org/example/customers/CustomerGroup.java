package org.example.customers;

import java.util.List;

public class CustomerGroup {

    private final List<Customer> customers;
    private int ageSum;

    private int customerId;

    public CustomerGroup(List<Customer> customers, int customerId) {
        this.customers = customers;
        for (Customer customer: customers){
            ageSum += customer.getAge();
        }
        this.customerId = customerId;
    }
    public int getAgeSum(){
        return ageSum;
    }
    public List<Customer> getCustomerList(){
        return customers;
    }
    public int getCustomerAmount(){
        return customers.size();
    }
    public boolean hasChildUnder5() {
        for (Customer customer : customers) {
            if (customer.getAge() <= 5) {
                return true;
            }
        }
        return false;
    }
    public boolean hasChildUnder10() {
        for (Customer customer : customers) {
            if (customer.getAge() < 10) {
                return true;
            }
        }
        return false;
    }
    public boolean hasNoChildNotFitForPaddling() {
        for (Customer customer : customers) {
            if (customer.getAge() > 5 && customer.getAge() < 18) {
                return false;
            }
        }
        return true;
    }
    public boolean hasAdult() {
        for (Customer customer : customers) {
            if (customer.isAdult()) {
                return true;
            }
        }
        return false;
    }
    public boolean allAdults(){
        for(Customer customer : customers){
            if(!customer.isAdult()) return false;
        }
        return true;
    }

    public boolean isVipGroup() {
        for (Customer customer : customers) {
            if (customer.isVip()) {
                return true;
            }
        }
        return false;
    }

    public int getCustomerId() {
        return customerId;
    }
}
