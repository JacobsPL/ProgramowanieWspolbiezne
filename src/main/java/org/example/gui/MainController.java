package org.example.gui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.example.config.AppConfig;
import org.example.customers.CustomerGroup;
import org.example.customers.CustomerGroupFactory;
import org.example.events.GuiSimulationEventListener;
import org.example.events.SimulationEvent;
import org.example.gui.helpers.EntranceHelper;
import org.example.gui.helpers.PoolsHelper;
import org.example.gui.helpers.QueueHelper;
import org.example.processes.CustomerProcess;
import org.example.services.PoolService;
import org.example.services.SwimmingCenter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class MainController {

    private final AppConfig config = AppConfig.getInstance();

    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;

    @FXML
    private Spinner customerCounterSpinner;
    @FXML
    private Spinner ticketDurationSpinner;
    @FXML
    private Spinner cleaningWaitTimeSpinner;
    @FXML
    private Spinner cleaningTimeSpinner;

    @FXML
    private Label cleaningStatusLabel;
    @FXML
    private Label centerStatusLabel;
    @FXML
    private Label peopleInsideLabel;
    @FXML
    private Label olympicPoolLabel;
    @FXML
    private Label recreationalPoolLabel;
    @FXML
    private Label paddlingPoolLabel;
    @FXML
    private Label cashiersBusyLabel;

    @FXML
    private Rectangle queueZone;
    @FXML
    private Rectangle entranceZone;
    @FXML
    private Rectangle exitZone;
    @FXML
    private Rectangle cashierZone1;
    @FXML
    private Rectangle cashierZone2;
    @FXML
    private Rectangle cashierZone3;
    @FXML
    private Rectangle olympicPoolZone;
    @FXML
    private Rectangle recreationalPoolZone;
    @FXML
    private Rectangle paddlingPoolZone;
    @FXML
    private Rectangle olympicWait;
    @FXML
    private Rectangle recreationalWait;
    @FXML
    private Rectangle paddlingWait;
    @FXML
    private Rectangle gateZone;
    @FXML
    private AnchorPane animationPane;

    private List<Rectangle> cashierZones;
    private PoolsHelper poolsHelper;
    private QueueHelper queueHelper;
    private EntranceHelper entranceHelper;
    private Timeline cleaningTimer;
    private int secondsToCleaning;
    private int peopleInside;
    private int olympicPoolPeople;
    private int recreationalPoolPeople;
    private int paddlingPoolPeople;
    private boolean simulationRunning;
    private boolean customerThreadsFinished;
    private List<Thread> customerThreads = new ArrayList<>();
    private Thread poolServiceThread;
    private Thread simulationWatcher;
    private final Map<Integer, Queue<SimulationEvent>> customerEvents = new HashMap<>();
    private final Map<Integer, Boolean> customerEventRunning = new HashMap<>();

    @FXML
    private void initialize() {
        initializeSpinners();
        updateStaticLabels();
        cleaningStatusLabel.setText("Status sprzatania: czeka");

        initializeHelpers();

        startButton.setOnAction(event -> startSimulation());
        stopButton.setOnAction(event -> stopSimulation());
        stopButton.setDisable(true);
    }

    private void initializeHelpers() {
        cashierZones = List.of(cashierZone1, cashierZone2, cashierZone3);
        poolsHelper = new PoolsHelper(
                animationPane,
                olympicPoolZone,
                recreationalPoolZone,
                paddlingPoolZone,
                olympicWait,
                recreationalWait,
                paddlingWait,
                exitZone
        );
        queueHelper = new QueueHelper(cashierZones, gateZone);
        queueHelper.setOnStateChanged(this::updateCashiersLabel);
        entranceHelper = new EntranceHelper(animationPane, queueZone, entranceZone);
    }

    public void handleEvent(SimulationEvent event) {
        if (!simulationRunning) {
            return;
        }

        switch (event.getType()) {
            case CLEANING_STARTED -> {
                stopCleaningTimer();
                cleaningStatusLabel.setText("Status sprzatania: trwa sprzatanie");
                centerStatusLabel.setText("Status: sprzatanie");
            }
            case CLEANING_FINISHED -> {
                centerStatusLabel.setText("Status: otwarte");
                startCleaningTimer();
            }
            default -> enqueueCustomerEvent(event);
        }
    }

    private void enqueueCustomerEvent(SimulationEvent event) {
        Queue<SimulationEvent> events = customerEvents.computeIfAbsent(event.getCustomerId(), id -> new ArrayDeque<>());
        events.add(event);
        processNextCustomerEvent(event.getCustomerId());
    }

    private void processNextCustomerEvent(int customerId) {
        if (customerEventRunning.getOrDefault(customerId, false)) {
            return;
        }

        Queue<SimulationEvent> events = customerEvents.get(customerId);
        if (events == null || events.isEmpty()) {
            return;
        }

        customerEventRunning.put(customerId, true);
        SimulationEvent event = events.poll();
        runCustomerEvent(event, () -> finishCustomerEvent(customerId));
    }

    private void finishCustomerEvent(int customerId) {
        customerEventRunning.put(customerId, false);
        processNextCustomerEvent(customerId);
        finishSimulationIfReady();
    }

    private void runCustomerEvent(SimulationEvent event, Runnable onFinished) {
        Circle customer = entranceHelper.getCustomer(event.getCustomerId());

        switch (event.getType()) {
            case CUSTOMER_CREATED -> entranceHelper.createCustomer(
                    event.getCustomerId(),
                    event.isVipGroup(),
                    event.hasChild(),
                    onFinished
            );
            case MOVED_TO_CASHIER -> {
                if (customer == null) {
                    onFinished.run();
                    return;
                }
                queueHelper.moveCustomerToCashier(customer, () -> {
                    updateCashiersLabel();
                    onFinished.run();
                });
                updateCashiersLabel();
            }
            case MOVED_TO_GATE_QUEUE -> {
                if (customer == null) {
                    onFinished.run();
                    return;
                }
                queueHelper.moveCustomerToGateQueue(customer, () -> {
                    updateCashiersLabel();
                    onFinished.run();
                });
                updateCashiersLabel();
            }
            case ENTERED_CENTER -> {
                peopleInside += event.getGroupSize();
                updateCenterLabels();
                if (customer == null) {
                    onFinished.run();
                    return;
                }
                queueHelper.moveCustomerThroughGate(customer, () -> {
                    updateCashiersLabel();
                    onFinished.run();
                });
                updateCashiersLabel();
            }
            case MOVED_TO_POOL_QUEUE -> {
                if (customer == null) {
                    onFinished.run();
                    return;
                }
                poolsHelper.moveCustomerToPoolQueue(customer, event.getTarget(), onFinished);
            }
            case ENTERED_POOL -> {
                addPoolPeople(event.getTarget(), event.getGroupSize());
                if (customer == null) {
                    onFinished.run();
                    return;
                }
                poolsHelper.moveCustomerToPool(customer, event.getTarget(), onFinished);
            }
            case LEFT_POOL -> {
                removePoolPeople(event.getTarget(), event.getGroupSize());
                if (customer != null) {
                    poolsHelper.leavePool(customer, event.getTarget());
                }
                onFinished.run();
            }
            case EXIT_CENTER -> {
                peopleInside -= event.getGroupSize();
                if (peopleInside < 0) {
                    peopleInside = 0;
                }
                updateCenterLabels();
                if (customer == null) {
                    onFinished.run();
                    return;
                }
                poolsHelper.moveCustomerToExit(customer, () -> {
                    entranceHelper.removeCustomer(event.getCustomerId());
                    onFinished.run();
                });
            }
            default -> onFinished.run();
        }
    }

    private void startSimulation() {
        startButton.setDisable(true);
        stopButton.setDisable(false);
        resetSimulationState();
        simulationRunning = true;
        cleaningStatusLabel.setText("Status sprzatania: symulacja uruchomiona");
        centerStatusLabel.setText("Status: otwarte");
        startCleaningTimer();

        try {
            CustomerGroupFactory customerGroupFactory = new CustomerGroupFactory();
            SwimmingCenter swimmingCenter = new SwimmingCenter();
            GuiSimulationEventListener eventListener = new GuiSimulationEventListener(this);

            int customerGroups = (Integer) customerCounterSpinner.getValue();
            long ticketDurationMs = secondsToMillis((Integer) ticketDurationSpinner.getValue());
            int cleaningIntervalMs = secondsToMillis((Integer) cleaningWaitTimeSpinner.getValue());
            int cleaningTimeMs = secondsToMillis((Integer) cleaningTimeSpinner.getValue());

            poolServiceThread = new Thread(new PoolService(
                    swimmingCenter,
                    eventListener,
                    cleaningIntervalMs,
                    cleaningTimeMs
            ));
            poolServiceThread.setDaemon(true);
            poolServiceThread.start();

            customerThreads = new ArrayList<>();
            for (int i = 0; i < customerGroups; i++) {
                CustomerGroup group = customerGroupFactory.generateGroup();
                Thread customerThread = new Thread(new CustomerProcess(
                        group,
                        swimmingCenter,
                        eventListener,
                        ticketDurationMs
                ));
                customerThread.setDaemon(true);
                customerThreads.add(customerThread);
                customerThread.start();
            }
            waitForSimulationEnd(customerThreads, poolServiceThread);
        } catch (IOException e) {
            cleaningStatusLabel.setText("Status sprzatania: blad startu symulacji");
            startButton.setDisable(false);
            stopButton.setDisable(true);
            simulationRunning = false;
            e.printStackTrace();
        }
    }

    private void waitForSimulationEnd(List<Thread> customerThreads, Thread poolServiceThread) {
        simulationWatcher = new Thread(() -> {
            try {
                for (Thread customerThread : customerThreads) {
                    customerThread.join();
                }

                poolServiceThread.interrupt();
                Platform.runLater(() -> {
                    if (!simulationRunning) {
                        return;
                    }
                    customerThreadsFinished = true;
                    finishSimulationIfReady();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        simulationWatcher.setDaemon(true);
        simulationWatcher.start();
    }

    private void stopSimulation() {
        simulationRunning = false;
        interruptSimulationThreads();
        resetSimulationState();
        cleaningStatusLabel.setText("Status sprzatania: symulacja zatrzymana");
        centerStatusLabel.setText("Status: zatrzymana");
        startButton.setDisable(false);
        stopButton.setDisable(true);
    }

    private void resetSimulationState() {
        stopCleaningTimer();
        animationPane.getChildren().removeIf(node -> node instanceof Circle);
        if (entranceHelper != null) {
            entranceHelper.clear();
        }
        customerEvents.clear();
        customerEventRunning.clear();
        customerThreadsFinished = false;
        customerThreads = new ArrayList<>();
        poolServiceThread = null;
        simulationWatcher = null;
        initializeHelpers();
        updateStaticLabels();
    }

    private void finishSimulationIfReady() {
        if (!simulationRunning) {
            return;
        }

        if (!customerThreadsFinished || !customerAnimationsFinished()) {
            return;
        }

        simulationRunning = false;
        stopCleaningTimer();
        cleaningStatusLabel.setText("Status sprzatania: symulacja zakonczona");
        centerStatusLabel.setText("Status: zakonczona");
        startButton.setDisable(false);
        stopButton.setDisable(true);
        interruptSimulationThreads();
    }

    private boolean customerAnimationsFinished() {
        for (Queue<SimulationEvent> events : customerEvents.values()) {
            if (!events.isEmpty()) {
                return false;
            }
        }

        for (Boolean running : customerEventRunning.values()) {
            if (running) {
                return false;
            }
        }

        return true;
    }

    private void initializeSpinners() {
        customerCounterSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1,
                        999,
                        config.getInt("simulation.customer.groups")
                )
        );
        ticketDurationSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1,
                        999,
                        millisToSeconds(config.getLong("ticket.duration.ms"))
                )
        );
        cleaningWaitTimeSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1,
                        999,
                        millisToSeconds(config.getLong("service.interval.time.ms"))
                )
        );
        cleaningTimeSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(
                        1,
                        999,
                        millisToSeconds(config.getLong("service.cleaning.time.ms"))
                )
        );
    }

    private void updateStaticLabels() {
        peopleInside = 0;
        olympicPoolPeople = 0;
        recreationalPoolPeople = 0;
        paddlingPoolPeople = 0;
        centerStatusLabel.setText("Status: czeka");
        updateCenterLabels();
        updatePoolLabels();
        updateCashiersLabel();
    }

    private void updateCenterLabels() {
        peopleInsideLabel.setText("Osoby w centrum: " + peopleInside + "/" + config.getInt("center.capacity"));
    }

    private void updatePoolLabels() {
        olympicPoolLabel.setText("Olimpijski: " + olympicPoolPeople + "/" + config.getInt("pool.olympic.capacity"));
        recreationalPoolLabel.setText("Rekreacyjny: " + recreationalPoolPeople + "/" + config.getInt("pool.recreational.capacity"));
        paddlingPoolLabel.setText("Brodzik: " + paddlingPoolPeople + "/" + config.getInt("pool.paddling.capacity"));
    }

    private void updateCashiersLabel() {
        if (queueHelper == null) {
            cashiersBusyLabel.setText("Kasy zajete: 0/" + config.getInt("cashier.count"));
            return;
        }

        cashiersBusyLabel.setText("Kasy zajete: "
                + queueHelper.getBusyCashiersCount()
                + "/"
                + queueHelper.getCashierCount());
    }

    private void addPoolPeople(String poolName, int groupSize) {
        if (poolName.equals(config.getString("pool.olympic.name"))) {
            olympicPoolPeople += groupSize;
        } else if (poolName.equals(config.getString("pool.recreational.name"))) {
            recreationalPoolPeople += groupSize;
        } else if (poolName.equals(config.getString("pool.paddling.name"))) {
            paddlingPoolPeople += groupSize;
        }

        updatePoolLabels();
    }

    private void removePoolPeople(String poolName, int groupSize) {
        if (poolName.equals(config.getString("pool.olympic.name"))) {
            olympicPoolPeople -= groupSize;
            if (olympicPoolPeople < 0) {
                olympicPoolPeople = 0;
            }
        } else if (poolName.equals(config.getString("pool.recreational.name"))) {
            recreationalPoolPeople -= groupSize;
            if (recreationalPoolPeople < 0) {
                recreationalPoolPeople = 0;
            }
        } else if (poolName.equals(config.getString("pool.paddling.name"))) {
            paddlingPoolPeople -= groupSize;
            if (paddlingPoolPeople < 0) {
                paddlingPoolPeople = 0;
            }
        }

        updatePoolLabels();
    }

    private int millisToSeconds(long millis) {
        return (int) (millis / 1000);
    }

    private int secondsToMillis(int seconds) {
        return seconds * 1000;
    }

    private void interruptSimulationThreads() {
        for (Thread customerThread : customerThreads) {
            if (customerThread != null && customerThread.isAlive()) {
                customerThread.interrupt();
            }
        }

        if (poolServiceThread != null && poolServiceThread.isAlive()) {
            poolServiceThread.interrupt();
        }

        if (simulationWatcher != null && simulationWatcher.isAlive()) {
            simulationWatcher.interrupt();
        }
    }

    private void resetCleaningTimerLabel() {
        secondsToCleaning = (Integer) cleaningWaitTimeSpinner.getValue();
        cleaningStatusLabel.setText("Status sprzatania: za " + secondsToCleaning + " s");
    }

    private void startCleaningTimer() {
        stopCleaningTimer();
        resetCleaningTimerLabel();

        cleaningTimer = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> updateCleaningTimer())
        );
        cleaningTimer.setCycleCount(Timeline.INDEFINITE);
        cleaningTimer.play();
    }

    private void updateCleaningTimer() {
        secondsToCleaning--;

        if (secondsToCleaning <= 0) {
            cleaningStatusLabel.setText("Status sprzatania: oczekiwanie na opuszczenie centrum");
            stopCleaningTimer();
            return;
        }

        cleaningStatusLabel.setText("Status sprzatania: za " + secondsToCleaning + " s");
    }

    private void stopCleaningTimer() {
        if (cleaningTimer != null) {
            cleaningTimer.stop();
            cleaningTimer = null;
        }
    }



}
