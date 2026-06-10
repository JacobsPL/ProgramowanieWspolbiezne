package org.example.customers;

import org.example.config.AppConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CustomerFactory {

    private final List<String> names;
    private final int minAge;
    private final int maxAge;
    private final int vipChancePercent;
    private final int pampersChancePercent;



    public CustomerFactory() throws IOException {
        AppConfig config = AppConfig.getInstance();
        names = loadNames(config.getString("customer.names.file"));
        minAge = config.getInt("customer.age.min");
        maxAge = config.getInt("customer.age.max");
        vipChancePercent = config.getInt("customer.vip.chance.percent");
        pampersChancePercent = config.getInt("customer.pampers.chance.percent");
    }

    private String generateName(){
        return names.get(ThreadLocalRandom.current().nextInt(1, names.size()));
    }

    private int generateAge(){
       return ThreadLocalRandom.current().nextInt(minAge, maxAge + 1);
    }

    private boolean generateVip(){
        return ThreadLocalRandom.current().nextInt(100) < vipChancePercent;
    }

    private boolean generateHasPampers(){
        return ThreadLocalRandom.current().nextInt(100) < pampersChancePercent;
    }

    public Customer generateCustomer(){
        String name = generateName();
        int age = generateAge();
        boolean isVip;
        if(age<18){
            isVip = false;
        }else{
            isVip = generateVip();
        }

        boolean hasPampers;
        if(age>3){
            hasPampers = false;
        }else{
            hasPampers = generateHasPampers();
        }
        return new Customer(name,age,isVip,hasPampers);
    }

    private List<String> loadNames(String fileName) throws IOException {
        InputStream inputStream = CustomerFactory.class.getClassLoader().getResourceAsStream(fileName);
        if (inputStream != null) {
            try (inputStream) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
            }
        }

        return Files.readAllLines(Path.of(fileName), StandardCharsets.UTF_8);
    }
}
