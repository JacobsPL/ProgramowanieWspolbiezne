package org.example.services;

import org.example.config.AppConfig;
import org.example.customers.CustomerGroup;
import org.example.events.SimulationEvent;
import org.example.events.SimulationEventType;
import org.example.pools.OlympicPool;
import org.example.pools.PaddlingPool;
import org.example.pools.RecreationalPool;
import org.example.pools.SwimmingPool;
import org.example.events.SimulationEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

public class SwimmingCenter {

    private final List<SwimmingPool> poolList = new ArrayList<>();
    private final Cashier cashier;
    private final Semaphore centerCapacity;
    private final Semaphore gateQueue;
    private final Semaphore gatePassage;
    private final int maxCenterCapacity;
    private final int gatePassageTimeMs;

    private boolean closed = false;
    private int peopleInside = 0;

    public SwimmingCenter() {
        AppConfig config = AppConfig.getInstance();
        this.maxCenterCapacity = config.getInt("center.capacity");
        this.centerCapacity = new Semaphore(maxCenterCapacity, true);
        this.gateQueue = new Semaphore(config.getInt("gate.queue.capacity"), true);
        this.gatePassage = new Semaphore(1, true);
        this.gatePassageTimeMs = config.getInt("gate.passage.time.ms");
        this.cashier = new Cashier(gateQueue);

        SwimmingPool olympicPool = new OlympicPool(
                config.getString("pool.olympic.name"),
                config.getInt("pool.olympic.capacity")
        );
        SwimmingPool recreationalPool = new RecreationalPool(
                config.getString("pool.recreational.name"),
                config.getInt("pool.recreational.capacity"),
                config.getInt("pool.recreational.max.average.age")
        );
        SwimmingPool paddlingPool = new PaddlingPool(
                config.getString("pool.paddling.name"),
                config.getInt("pool.paddling.capacity")
        );

        poolList.add(olympicPool);
        poolList.add(recreationalPool);
        poolList.add(paddlingPool);
    }

    public boolean enterCenter(CustomerGroup customerGroup, SimulationEventListener eventListener) throws InterruptedException {
        if (customerGroup.getCustomerAmount() > maxCenterCapacity) {
            System.out.println("Grupa jest zbyt liczna, aby wejsc do centrum.");
            return false;
        }

        boolean gateQueueReserved = false;
        boolean centerCapacityReserved = false;

        try {
            gateQueueReserved = cashier.buyTicket(customerGroup, eventListener);

            if (customerGroup.isVipGroup()) {
                gateQueue.acquire();
                gateQueueReserved = true;

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
                System.out.println("Grupa z osoba VIP omija kase i ustawia sie do bramki.");
            }

            synchronized (this) {
                while (closed) {
                    wait();
                }
            }

            centerCapacity.acquire(customerGroup.getCustomerAmount());
            centerCapacityReserved = true;

            gatePassage.acquire();
            try {
                Thread.sleep(gatePassageTimeMs);
            } finally {
                gatePassage.release();
            }

            if (gateQueueReserved) {
                gateQueue.release();
                gateQueueReserved = false;
            }

            synchronized (this) {
                peopleInside += customerGroup.getCustomerAmount();
                System.out.println("Grupa weszla do centrum. Zajete miejsca: "
                        + peopleInside + "/" + maxCenterCapacity);
            }
            return true;
        } catch (InterruptedException e) {
            if (centerCapacityReserved) {
                centerCapacity.release(customerGroup.getCustomerAmount());
            }
            if (gateQueueReserved) {
                gateQueue.release();
            }
            throw e;
        }
    }

    public void leaveCenter(CustomerGroup customerGroup) {
        centerCapacity.release(customerGroup.getCustomerAmount());
        synchronized (this){
            peopleInside -= customerGroup.getCustomerAmount();
            if (peopleInside < 0) {
                peopleInside = 0;
            }
            System.out.println("Grupa wyszla z centrum. Zajete miejsca: "
                    + peopleInside + "/" + maxCenterCapacity);
            notifyAll();
        }
    }

    public SwimmingPool tryEnterAnyPool(CustomerGroup customerGroup, SimulationEventListener eventListener) throws InterruptedException {
        SwimmingPool firstAllowedPool = null;
        List<SwimmingPool> randomizedPools = new ArrayList<>(poolList);
        Collections.shuffle(randomizedPools, ThreadLocalRandom.current());

        for (SwimmingPool pool : randomizedPools) {
            if (!pool.canAccept(customerGroup)) {
                continue;
            }

            if (firstAllowedPool == null) {
                firstAllowedPool = pool;
            }

            if (pool.enterPoolIfSpace(customerGroup)) {
                return pool;
            }
        }

        if (firstAllowedPool != null) {
            if (eventListener != null) {
                eventListener.onEvent(
                        new SimulationEvent(
                                SimulationEventType.MOVED_TO_POOL_QUEUE,
                                customerGroup.getCustomerId(),
                                firstAllowedPool.getName(),
                                customerGroup.getCustomerAmount()
                        )
                );
            }

            if (firstAllowedPool.enterPool(customerGroup)) {
                return firstAllowedPool;
            }
        }

        return null;
    }

    public synchronized void closeEntry(){
        closed = true;
        System.out.println("Wejscie do centrum zostalo zamkniete.");
    }
    public synchronized void openEntry(){
        closed = false;
        System.out.println("Wejscie do centrum zostalo otwarte.");
        notifyAll();
    }
    public synchronized void waitUntilEmpty() throws InterruptedException {
        while (peopleInside >0){
            wait();
        }
    }
}
