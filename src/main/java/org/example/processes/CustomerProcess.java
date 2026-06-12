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

    private static final int ANIMATION_STEP_DELAY_MS = 700;

    private CustomerGroup customerGroup;
    private SwimmingCenter swimmingCenter;
    private long ticketDurationMs;
    private int minSwimmingTimeMs;
    private int maxSwimmingTimeMs;
    private int minWaitTimeMs;
    private int maxWaitTimeMs;

    private final SimulationEventListener eventListener;

    public CustomerProcess(CustomerGroup customerGroup, SwimmingCenter swimmingCenter, SimulationEventListener eventListener) {
        this(customerGroup, swimmingCenter, eventListener, AppConfig.getInstance().getLong("ticket.duration.ms"));
    }

    public CustomerProcess(CustomerGroup customerGroup,
                           SwimmingCenter swimmingCenter,
                           SimulationEventListener eventListener,
                           long ticketDurationMs) {
        this.eventListener = eventListener;
        AppConfig config = AppConfig.getInstance();
        this.customerGroup = customerGroup;
        this.swimmingCenter = swimmingCenter;
        this.ticketDurationMs = ticketDurationMs;
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

    private void waitForAnimationStep() throws InterruptedException {
        Thread.sleep(ANIMATION_STEP_DELAY_MS);
    }

    @Override
    public void run() {
        SwimmingPool enteredPool = null;
        boolean enteredCenter = false;
        eventListener.onEvent(
                new SimulationEvent(
                        SimulationEventType.CUSTOMER_CREATED,
                        customerGroup.getCustomerId(),
                        "main-queue",
                        customerGroup.isVipGroup(),
                        customerGroup.hasChild(),
                        customerGroup.getCustomerAmount()));
        try {
            waitForAnimationStep();
            enteredCenter = swimmingCenter.enterCenter(customerGroup, eventListener);
            if (!enteredCenter) {
                return;
            }
            eventListener.onEvent(
                    new SimulationEvent(
                            SimulationEventType.ENTERED_CENTER,
                            customerGroup.getCustomerId(),
                            "gate",
                            customerGroup.isVipGroup(),
                            customerGroup.hasChild(),
                            customerGroup.getCustomerAmount()));
            waitForAnimationStep();

            long ticketEndTime = System.currentTimeMillis() + ticketDurationMs;

            while (System.currentTimeMillis() < ticketEndTime) {
                enteredPool = swimmingCenter.tryEnterAnyPool(customerGroup, eventListener);


                if (enteredPool == null) {
                    Thread.sleep(generateRandomWaitForAvailablePoolTime());
                    continue;
                }
                eventListener.onEvent(
                        new SimulationEvent(
                                SimulationEventType.ENTERED_POOL,
                                customerGroup.getCustomerId(),
                                enteredPool.getName(),
                                customerGroup.getCustomerAmount()));
                waitForAnimationStep();


                int swimmingTime = generateRandomSwimmingTime();
                long remainingTicketTime = ticketEndTime - System.currentTimeMillis();

                if (remainingTicketTime <= 0) {
                    eventListener.onEvent(
                            new SimulationEvent(
                                    SimulationEventType.LEFT_POOL,
                                    customerGroup.getCustomerId(),
                                    enteredPool.getName(),
                                    customerGroup.getCustomerAmount()));
                    enteredPool.leavePool(customerGroup);

                    enteredPool = null;
                    break;

                }

                Thread.sleep(Math.min(remainingTicketTime, swimmingTime));
                eventListener.onEvent(
                        new SimulationEvent(
                                SimulationEventType.LEFT_POOL,
                                customerGroup.getCustomerId(),
                                enteredPool.getName(),
                                customerGroup.getCustomerAmount()));

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
                                enteredPool.getName(),
                                customerGroup.getCustomerAmount()));
                enteredPool.leavePool(customerGroup);
            }
        } finally {
            if (enteredCenter) {
                eventListener.onEvent(
                        new SimulationEvent(
                        SimulationEventType.EXIT_CENTER,
                        customerGroup.getCustomerId(),
                        "exit-zone",
                        customerGroup.getCustomerAmount()));
                swimmingCenter.leaveCenter(customerGroup);
            }
        }

        System.out.println("Grupa zakonczyla korzystanie z biletu.");
    }
}
