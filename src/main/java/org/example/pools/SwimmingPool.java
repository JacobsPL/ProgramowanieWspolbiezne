package org.example.pools;

import org.example.customers.Customer;
import org.example.customers.CustomerGroup;

import java.util.Iterator;

public abstract class SwimmingPool {

    protected final String NAME;
    protected final int MAX_CAPACITY;
    protected int currentCapacity;
    protected int ageSum;

    public SwimmingPool(String name, int maxCapacity) {
        this.NAME = name;
        this.MAX_CAPACITY = maxCapacity;
        currentCapacity = 0;
    }

    protected int calculateAvgAge(CustomerGroup customerGroup) {
        return (ageSum + customerGroup.getAgeSum()) / (currentCapacity + customerGroup.getCustomerAmount());
    }

    protected boolean hasSpace(CustomerGroup customerGroup) {
        return MAX_CAPACITY >= currentCapacity + customerGroup.getCustomerAmount();
    }

    protected int getMaxCapacity() {
        return this.MAX_CAPACITY;
    }

    protected boolean hasPampersOrDoesNotRequire(CustomerGroup customerGroup) {
        for (Customer customer : customerGroup.getCustomerList()) {
            if (customer.getAge() <= 3) {
                if (!customer.hasPampers()) {
                    return false;
                }
            }
        }
        return true;
    }

    protected void addGroup(CustomerGroup customerGroup) {
        ageSum += customerGroup.getAgeSum();
        currentCapacity = currentCapacity + customerGroup.getCustomerAmount();
    }

    protected boolean childHasGuardianRuleMeet(CustomerGroup customerGroup) {
        if (customerGroup.hasChildUnder10()) {
            return customerGroup.hasAdult();
        }
        return true;
    }

    protected abstract boolean canEnter(CustomerGroup customerGroup);

    public synchronized boolean canAccept(CustomerGroup customerGroup) {
        return canEnter(customerGroup) && customerGroup.getCustomerAmount() <= MAX_CAPACITY;
    }

    public synchronized boolean enterPoolIfSpace(CustomerGroup customerGroup) {
        if (!canAccept(customerGroup) || !hasSpace(customerGroup)) {
            return false;
        }

        addGroup(customerGroup);
        System.out.println(customerGroupToString(customerGroup) + " weszla do basenu " + NAME
                + ". Liczba osob: " + currentCapacity + "/" + MAX_CAPACITY);
        return true;
    }

    public synchronized boolean enterPool(CustomerGroup customerGroup) throws InterruptedException {
        if (!canAccept(customerGroup)) {
            return false;
        }

        while (!hasSpace(customerGroup)) {
            System.out.println(customerGroupToString(customerGroup) + " czeka na wolne miejsce w basenie " + NAME);
            wait();
        }

        addGroup(customerGroup);
        System.out.println(customerGroupToString(customerGroup) + " weszla do basenu " + NAME
                + ". Liczba osob: " + currentCapacity + "/" + MAX_CAPACITY);
        return true;
    }

    public synchronized void leavePool(CustomerGroup customerGroup) {
        currentCapacity = currentCapacity - customerGroup.getCustomerAmount();
        if (currentCapacity < 0) currentCapacity = 0;

        ageSum = ageSum - customerGroup.getAgeSum();
        if (ageSum < 0) ageSum = 0;
        notifyAll();
        System.out.println(customerGroupToString(customerGroup) + " wyszla z basenu " + NAME
                + ". Liczba osob: " + currentCapacity + "/" + MAX_CAPACITY);
    }

    private String customerGroupToString(CustomerGroup customerGroup) {
        StringBuilder sb = new StringBuilder("Grupa: ");

        Iterator<Customer> iterator = customerGroup.getCustomerList().iterator();

        while (iterator.hasNext()) {
            Customer customer = iterator.next();
            sb.append(customer.getName())
                    .append(", wiek: ")
                    .append(customer.getAge());

            if (iterator.hasNext()) {
                sb.append(" oraz ");
            }
        }

        return sb.toString();
    }
}
