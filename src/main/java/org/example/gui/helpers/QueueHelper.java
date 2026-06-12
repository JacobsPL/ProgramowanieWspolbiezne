package org.example.gui.helpers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

public class QueueHelper {

    private static final int GATE_SPACING = 16;

    private final List<Rectangle> cashierZones;
    private final Rectangle gateZone;
    private final boolean[] occupiedCashiers;
    private final Queue<CashierRequest> waitingForCashier = new ArrayDeque<>();
    private final Map<Circle, Integer> customerCashierSlots = new IdentityHashMap<>();
    private final Map<Circle, Integer> customerGateSlots = new IdentityHashMap<>();
    private final java.util.List<Boolean> occupiedGateSlots = new java.util.ArrayList<>();
    private Runnable onStateChanged;

    public QueueHelper(List<Rectangle> cashierZones, Rectangle gateZone) {
        this.cashierZones = cashierZones;
        this.gateZone = gateZone;
        this.occupiedCashiers = new boolean[cashierZones.size()];
    }

    public void moveCustomerToCashier(Circle customer, Runnable onFinished) {
        waitingForCashier.add(new CashierRequest(customer, onFinished));
        moveWaitingCustomersToFreeCashiers();
    }

    public void moveCustomerToGateQueue(Circle customer, Runnable onFinished) {
        releaseCashierSlot(customer);
        moveWaitingCustomersToFreeCashiers();

        int gateSlot = reserveGateSlotForCustomer(customer);
        Point2D gatePosition = getGateQueuePosition(gateSlot);

        moveWithShortDelay(customer, gatePosition, onFinished);
    }

    public void moveCustomerThroughGate(Circle customer, Runnable onFinished) {
        releaseGateSlot(customer);
        AnimationHelper.moveTo(customer, AnimationHelper.centerOf(gateZone), onFinished);
    }

    public void releaseGateSlot(Circle customer) {
        Integer gateSlot = customerGateSlots.remove(customer);
        if (gateSlot != null) {
            occupiedGateSlots.set(gateSlot, false);
        }
    }

    public int getBusyCashiersCount() {
        return customerCashierSlots.size();
    }

    public int getCashierCount() {
        return occupiedCashiers.length;
    }

    public void setOnStateChanged(Runnable onStateChanged) {
        this.onStateChanged = onStateChanged;
    }

    private void moveWaitingCustomersToFreeCashiers() {
        while (!waitingForCashier.isEmpty()) {
            int cashierSlot = reserveCashierSlot();
            if (cashierSlot < 0) {
                return;
            }

            CashierRequest request = waitingForCashier.poll();
            customerCashierSlots.put(request.customer, cashierSlot);
            notifyStateChanged();

            Point2D cashierPosition = AnimationHelper.centerOf(cashierZones.get(cashierSlot));
            moveWithShortDelay(request.customer, cashierPosition, request.onFinished);
        }
    }

    private void moveWithShortDelay(Circle customer, Point2D target, Runnable onFinished) {
        Timeline timeline = new Timeline();
        int delay = ThreadLocalRandom.current().nextInt(200, 500);

        KeyFrame keyFrame = new KeyFrame(
                Duration.millis(delay),
                event -> AnimationHelper.moveTo(customer, target, onFinished)
        );

        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    private int reserveCashierSlot() {
        for (int i = 0; i < occupiedCashiers.length; i++) {
            if (!occupiedCashiers[i]) {
                occupiedCashiers[i] = true;
                return i;
            }
        }
        return -1;
    }

    private Point2D getGateQueuePosition(int gateSlot) {
        Point2D gateCenter = AnimationHelper.centerOf(gateZone);

        return new Point2D(
                gateCenter.getX(),
                gateZone.getLayoutY() - GATE_SPACING * (gateSlot + 1)
        );
    }

    private void releaseCashierSlot(Circle customer) {
        Integer cashierSlot = customerCashierSlots.remove(customer);
        if (cashierSlot != null) {
            occupiedCashiers[cashierSlot] = false;
            notifyStateChanged();
        }
    }

    private int reserveGateSlotForCustomer(Circle customer) {
        Integer existingGateSlot = customerGateSlots.get(customer);
        if (existingGateSlot != null) {
            return existingGateSlot;
        }

        for (int i = 0; i < occupiedGateSlots.size(); i++) {
            if (!occupiedGateSlots.get(i)) {
                occupiedGateSlots.set(i, true);
                customerGateSlots.put(customer, i);
                return i;
            }
        }

        occupiedGateSlots.add(true);
        int gateSlot = occupiedGateSlots.size() - 1;
        customerGateSlots.put(customer, gateSlot);
        return gateSlot;
    }

    private void notifyStateChanged() {
        if (onStateChanged != null) {
            onStateChanged.run();
        }
    }

    private static class CashierRequest {
        private final Circle customer;
        private final Runnable onFinished;

        private CashierRequest(Circle customer, Runnable onFinished) {
            this.customer = customer;
            this.onFinished = onFinished;
        }
    }
}
