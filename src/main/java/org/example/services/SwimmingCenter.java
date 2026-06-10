package org.example.services;

import org.example.config.AppConfig;
import org.example.customers.CustomerGroup;
import org.example.pools.OlympicPool;
import org.example.pools.PaddlingPool;
import org.example.pools.RecreationalPool;
import org.example.pools.SwimmingPool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class SwimmingCenter {

    private final List<SwimmingPool> poolList = new ArrayList<>();
    private final Cashier cashier;
    private final Semaphore centerCapacity;
    private final int maxCenterCapacity;

    private boolean closed = false;
    private int peopleInside = 0;

    public SwimmingCenter() {
        AppConfig config = AppConfig.getInstance();
        this.cashier = new Cashier();
        this.maxCenterCapacity = config.getInt("center.capacity");
        this.centerCapacity = new Semaphore(maxCenterCapacity, true);

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

    public boolean enterCenter(CustomerGroup customerGroup) throws InterruptedException {
        if (customerGroup.getCustomerAmount() > maxCenterCapacity) {
            System.out.println("Grupa jest zbyt liczna, aby wejsc do centrum.");
            return false;
        }

        cashier.buyTicket(customerGroup);

        synchronized (this) {
            while (closed) {
                wait();
            }
        }

        centerCapacity.acquire(customerGroup.getCustomerAmount());

        synchronized (this) {
            peopleInside += customerGroup.getCustomerAmount();
            System.out.println("Grupa weszla do centrum. Zajete miejsca: "
                    + peopleInside + "/" + maxCenterCapacity);
        }
        return true;
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

    public SwimmingPool tryEnterAnyPool(CustomerGroup customerGroup) throws InterruptedException {
        SwimmingPool firstAllowedPool = null;

        for (SwimmingPool pool : poolList) {
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

        if (firstAllowedPool != null && firstAllowedPool.enterPool(customerGroup)) {
            return firstAllowedPool;
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
