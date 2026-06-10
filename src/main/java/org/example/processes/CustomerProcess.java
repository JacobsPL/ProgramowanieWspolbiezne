package org.example.processes;

import org.example.config.AppConfig;
import org.example.customers.CustomerGroup;
import org.example.events.SimulationEvent;
import org.example.events.SimulationEventListener;
import org.example.events.SimulationEventType;
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

    private final SimulationEventListener eventListener;

    public CustomerProcess(CustomerGroup customerGroup, SwimmingCenter swimmingCenter, SimulationEventListener eventListener) {
        this.eventListener = eventListener;
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
        eventListener.onEvent(
                new SimulationEvent(
                        SimulationEventType.CUSTOMER_CREATED,
                        customerGroup.getCustomerId(),
                        "main-queue"));
        try {
            eventListener.onEvent(
                    new SimulationEvent(
                            SimulationEventType.MOVED_TO_CASHIER,
                            customerGroup.getCustomerId(),
                            "cashier"));
            enteredCenter = swimmingCenter.enterCenter(customerGroup);
            if (!enteredCenter) {
                return;
            }
            eventListener.onEvent(
                    new SimulationEvent(
                            SimulationEventType.ENTERED_CENTER,
                            customerGroup.getCustomerId(),
                            "gate"));

            long ticketEndTime = System.currentTimeMillis() + ticketDurationMs;

            while (System.currentTimeMillis() < ticketEndTime) {
                enteredPool = swimmingCenter.tryEnterAnyPool(customerGroup);


                if (enteredPool == null) {
                    Thread.sleep(generateRandomWaitForAvailablePoolTime());
                    continue;
                }
                eventListener.onEvent(
                        new SimulationEvent(
                                SimulationEventType.MOVED_TO_POOL_QUEUE,
                                customerGroup.getCustomerId(),
                                enteredPool.getName()));
                eventListener.onEvent(
                        new SimulationEvent(
                                SimulationEventType.ENTERED_POOL,
                                customerGroup.getCustomerId(),
                                enteredPool.getName()));


                int swimmingTime = generateRandomSwimmingTime();
                long remainingTicketTime = ticketEndTime - System.currentTimeMillis();

                if (remainingTicketTime <= 0) {
                    eventListener.onEvent(
                            new SimulationEvent(
                                    SimulationEventType.LEFT_POOL,
                                    customerGroup.getCustomerId(),
                                    enteredPool.getName()));
                    enteredPool.leavePool(customerGroup);

                    enteredPool = null;
                    break;

                }

                Thread.sleep(Math.min(remainingTicketTime, swimmingTime));
                eventListener.onEvent(
                        new SimulationEvent(
                                SimulationEventType.LEFT_POOL,
                                customerGroup.getCustomerId(),
                                enteredPool.getName()));

                enteredPool.leavePool(customerGroup);
                enteredPool = null;
                Thread.sleep(generateRandomWaitForAvailablePoolTime());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            if (enteredPool != null) {
                eventListener.onEvent(
                        new SimulationEvent(
                                SimulationEventType.LEFT_POOL,
                                customerGroup.getCustomerId(),
                                enteredPool.getName()));
                enteredPool.leavePool(customerGroup);
            }
        } finally {
            if (enteredCenter) {
                eventListener.onEvent(
                        new SimulationEvent(
                                SimulationEventType.EXIT_CENTER,
                                customerGroup.getCustomerId(),
                                "exit-zone"));
                swimmingCenter.leaveCenter(customerGroup);
            }
        }

        System.out.println("Grupa zakonczyla korzystanie z biletu.");
    }
}
