package org.example.services;

import org.example.config.AppConfig;

public class PoolService implements Runnable {

    private final int intervalTime;
    private final int cleaningTime;
    private final SwimmingCenter swimmingCenter;

    public PoolService(SwimmingCenter swimmingCenter) {
        AppConfig config = AppConfig.getInstance();
        this.intervalTime = config.getInt("service.interval.time.ms");
        this.cleaningTime = config.getInt("service.cleaning.time.ms");
        this.swimmingCenter = swimmingCenter;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(intervalTime);

                swimmingCenter.closeEntry();
                swimmingCenter.waitUntilEmpty();

                System.out.println("Centrum puste - sprzatanie rozpoczete.");
                Thread.sleep(cleaningTime);

                swimmingCenter.openEntry();
                System.out.println("Sprzatanie zakonczone - centrum otwarte.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
