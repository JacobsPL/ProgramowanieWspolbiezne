package org.example.services;

import org.example.config.AppConfig;
import org.example.customers.CustomerGroup;
import org.example.events.SimulationEvent;
import org.example.events.SimulationEventListener;
import org.example.events.SimulationEventType;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

public class Cashier {

    private final Semaphore cashiers;
    private final Semaphore gateQueue;
    private final int maxServiceTime;
    private final int minServiceTime;

    public Cashier(Semaphore gateQueue) {
        AppConfig config = AppConfig.getInstance();
        this.cashiers = new Semaphore(config.getInt("cashier.count"), true);
        this.gateQueue = gateQueue;
        this.maxServiceTime = config.getInt("cashier.service.time.max.ms");
        this.minServiceTime = config.getInt("cashier.service.time.min.ms");
    }

    public boolean buyTicket(CustomerGroup customerGroup, SimulationEventListener eventListener) throws InterruptedException {
        if (customerGroup.isVipGroup()) {
            System.out.println("Grupa z osoba VIP omija kase.");
            return false;
        }

        cashiers.acquire();
        try {
            if (eventListener != null) {
                eventListener.onEvent(
                        new SimulationEvent(
                                SimulationEventType.MOVED_TO_CASHIER,
                                customerGroup.getCustomerId(),
                                "cashier",
                                customerGroup.getCustomerAmount()
                        )
                );
            }
            Thread.sleep(ThreadLocalRandom.current().nextInt(minServiceTime, maxServiceTime + 1));
            gateQueue.acquire();
            if (eventListener != null) {
                eventListener.onEvent(
                        new SimulationEvent(
                                SimulationEventType.MOVED_TO_GATE_QUEUE,
                                customerGroup.getCustomerId(),
                                "gate",
                                customerGroup.getCustomerAmount()
                        )
                );
            }
            System.out.println("Grupa kupila bilety.");
            return true;
        } finally {
            cashiers.release();
        }
    }
}
