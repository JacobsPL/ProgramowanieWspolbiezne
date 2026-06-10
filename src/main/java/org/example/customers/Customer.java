package org.example.customers;

public class Customer {
    protected final int AGE;
    protected final boolean HAS_PAMPERS;
    protected final String NAME;

    protected final boolean VIP;

    public Customer(String name, int age, boolean isVip, boolean hasPampers) {
        this.NAME = name;
        this.AGE = age;
        this.VIP = isVip;
        this.HAS_PAMPERS = hasPampers;
    }

    public int getAge(){
        return AGE;
    }

    public String getName(){
        return NAME;
    }

    public boolean isAdult(){
        return AGE>=18;
    }

    public boolean hasPampers(){
        return HAS_PAMPERS;
    }

    public boolean isVip(){
        return VIP;
    }



}
