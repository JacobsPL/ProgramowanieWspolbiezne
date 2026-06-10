package org.example.services;

import org.example.config.AppConfig;
import org.example.customers.CustomerGroup;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

public class Cashier {

    private final Semaphore cashiers;
    private final int maxServiceTime;
    private final int minServiceTime;

    public Cashier() {
        AppConfig config = AppConfig.getInstance();
        this.cashiers = new Semaphore(config.getInt("cashier.count"), true);
        this.maxServiceTime = config.getInt("cashier.service.time.max.ms");
        this.minServiceTime = config.getInt("cashier.service.time.min.ms");
    }

    public void buyTicket(CustomerGroup customerGroup) throws InterruptedException {
        if (customerGroup.isVipGroup()) {
            System.out.println("Grupa z osoba VIP omija kase.");
            return;
        }

        cashiers.acquire();
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minServiceTime, maxServiceTime + 1));
            System.out.println("Grupa kupila bilety.");
        } finally {
            cashiers.release();
        }
    }
}
