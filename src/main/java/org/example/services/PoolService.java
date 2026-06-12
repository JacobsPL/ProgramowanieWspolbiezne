package org.example.services;

import org.example.config.AppConfig;
import org.example.events.SimulationEvent;
import org.example.events.SimulationEventListener;
import org.example.events.SimulationEventType;

public class PoolService implements Runnable {

    private final int intervalTime;
    private final int cleaningTime;
    private final SwimmingCenter swimmingCenter;
    private final SimulationEventListener eventListener;

    public PoolService(SwimmingCenter swimmingCenter,
                       SimulationEventListener eventListener) {
        AppConfig config = AppConfig.getInstance();
        this.intervalTime = config.getInt("service.interval.time.ms");
        this.cleaningTime = config.getInt("service.cleaning.time.ms");
        this.swimmingCenter = swimmingCenter;
        this.eventListener=eventListener;

    }

    public PoolService(SwimmingCenter swimmingCenter,
                       SimulationEventListener eventListener,
                       int intervalTime,
                       int cleaningTime) {
        this.intervalTime = intervalTime;
        this.cleaningTime = cleaningTime;
        this.swimmingCenter = swimmingCenter;
        this.eventListener=eventListener;

    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(intervalTime);

                swimmingCenter.closeEntry();
                swimmingCenter.waitUntilEmpty();

                eventListener.onEvent(new SimulationEvent(
                        SimulationEventType.CLEANING_STARTED,
                        -1,
                        "cleaning"
                ));

                System.out.println("Centrum puste - sprzatanie rozpoczete.");
                Thread.sleep(cleaningTime);


                eventListener.onEvent(new SimulationEvent(
                        SimulationEventType.CLEANING_FINISHED,
                        -1,
                        "cleaning-finished"
                ));
                swimmingCenter.openEntry();
                System.out.println("Sprzatanie zakonczone - centrum otwarte.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
