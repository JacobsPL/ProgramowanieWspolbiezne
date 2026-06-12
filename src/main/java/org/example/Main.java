package org.example;

import org.example.config.AppConfig;
import org.example.customers.CustomerGroup;
import org.example.customers.CustomerGroupFactory;
import org.example.events.SimulationEventListener;
import org.example.processes.CustomerProcess;
import org.example.services.PoolService;
import org.example.services.SwimmingCenter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {

        AppConfig config = AppConfig.getInstance();

        CustomerGroupFactory customerGroupFactory = new CustomerGroupFactory();
        SwimmingCenter swimmingCenter = new SwimmingCenter();
        SimulationEventListener eventListener = event -> System.out.println(
                "[EVENT] " + event.getType()
                        + " customerId=" + event.getCustomerId()
                        + " target=" + event.getTarget()
        );

        int customerGroups = config.getInt("simulation.customer.groups");
        List<Thread> customersThreads = new ArrayList<>();
        Thread poolServiceThread = new Thread(new PoolService(swimmingCenter,eventListener));
        poolServiceThread.start();

        for (int i = 0; i < customerGroups; i++) {
            CustomerGroup group = customerGroupFactory.generateGroup();
            customersThreads.add(new Thread(new CustomerProcess(group, swimmingCenter, eventListener)));
        }

        for (Thread thread : customersThreads) {
            thread.start();
        }
        for (Thread thread : customersThreads) {
            thread.join();
        }
        poolServiceThread.interrupt();
        poolServiceThread.join();

        System.out.println("Koniec symulacji.");
    }
}
