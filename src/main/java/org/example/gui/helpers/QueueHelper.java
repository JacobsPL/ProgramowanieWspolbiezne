package org.example.gui.helpers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class QueueHelper {

    private final List<Rectangle> cashierZones;
    private final Rectangle gateZone;
    private final PoolsHelper poolsHelper;

    public QueueHelper(List<Rectangle> cashierZones,
                       Rectangle gateZone,
                       PoolsHelper poolsHelper) {
        this.cashierZones = cashierZones;
        this.gateZone = gateZone;
        this.poolsHelper = poolsHelper;
    }

    public void moveCustomerToCashier(Circle customer) {
        Timeline timeline = new Timeline();
        int cashierRandomNumber = ThreadLocalRandom.current().nextInt(cashierZones.size());
        int delay = ThreadLocalRandom.current().nextInt(200, 500);

        KeyFrame keyFrame = new KeyFrame(
                Duration.millis(delay),
                event -> transitionToCashier(customer, cashierZones.get(cashierRandomNumber))
        );

        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    private void transitionToCashier(Circle customer, Rectangle cashier) {
        Point2D cashierPosition = AnimationHelper.centerOf(cashier);
        AnimationHelper.moveTo(customer, cashierPosition, () -> moveCustomerToGate(customer));
    }

    private void moveCustomerToGate(Circle customer) {
        Timeline timeline = new Timeline();
        int delay = ThreadLocalRandom.current().nextInt(200, 500);

        KeyFrame keyFrame = new KeyFrame(
                Duration.millis(delay),
                event -> transitionToGate(customer)
        );

        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    private void transitionToGate(Circle customer) {
        Point2D gatePosition = AnimationHelper.centerOf(gateZone);
        AnimationHelper.moveTo(customer, gatePosition,
                () -> poolsHelper.moveCustomerToPool(customer, poolsHelper.getRandomPool()));
    }
}
