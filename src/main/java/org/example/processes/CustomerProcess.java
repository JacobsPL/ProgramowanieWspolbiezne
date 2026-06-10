package org.example.processes;

import org.example.config.AppConfig;
import org.example.customers.CustomerGroup;
import org.example.pools.SwimmingPool;
import org.example.services.SwimmingCenter;

import java.util.concurrent.ThreadLocalRandom;

public class CustomerProcess implements Runnable {

    private CustomerGroup customerGroup;
    private SwimmingCenter swimmingCenter;
    private long ticketDurationMs;
    private int minSwimmingTimeMs;
    private int maxSwimmingTimeMs;
    private int minWaitTimeMs;
    private int maxWaitTimeMs;

    public CustomerProcess(CustomerGroup customerGroup, SwimmingCenter swimmingCenter) {
        AppConfig config = AppConfig.getInstance();
        this.customerGroup = customerGroup;
        this.swimmingCenter = swimmingCenter;
        this.ticketDurationMs = config.getLong("ticket.duration.ms");
        this.minSwimmingTimeMs = config.getInt("customer.swimming.time.min.ms");
        this.maxSwimmingTimeMs = config.getInt("customer.swimming.time.max.ms");
        this.minWaitTimeMs = config.getInt("customer.wait.time.min.ms");
        this.maxWaitTimeMs = config.getInt("customer.wait.time.max.ms");
    }

    private int generateRandomSwimmingTime() {
        return ThreadLocalRandom.current().nextInt(minSwimmingTimeMs, maxSwimmingTimeMs + 1);
    }

    private int generateRandomWaitForAvailablePoolTime() {
        return ThreadLocalRandom.current().nextInt(minWaitTimeMs, maxWaitTimeMs + 1);
    }

    @Override
    public void run() {
        SwimmingPool enteredPool = null;
        boolean enteredCenter = false;

        try {
            enteredCenter = swimmingCenter.enterCenter(customerGroup);
            if (!enteredCenter) {
                return;
            }

            long ticketEndTime = System.currentTimeMillis() + ticketDurationMs;

            while (System.currentTimeMillis() < ticketEndTime) {
                enteredPool = swimmingCenter.tryEnterAnyPool(customerGroup);

                if (enteredPool == null) {
                    Thread.sleep(generateRandomWaitForAvailablePoolTime());
                    continue;
                }

                int swimmingTime = generateRandomSwimmingTime();
                long remainingTicketTime = ticketEndTime - System.currentTimeMillis();

                if (remainingTicketTime <= 0) {
                    enteredPool.leavePool(customerGroup);
                    enteredPool = null;
                    break;
                }

                Thread.sleep(Math.min(remainingTicketTime, swimmingTime));
                enteredPool.leavePool(customerGroup);
                enteredPool = null;
                Thread.sleep(generateRandomWaitForAvailablePoolTime());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            if (enteredPool != null) {
                enteredPool.leavePool(customerGroup);
            }
        } finally {
            if (enteredCenter) {
                swimmingCenter.leaveCenter(customerGroup);
            }
        }

        System.out.println("Grupa zakonczyla korzystanie z biletu.");
    }
}
