package org.example.gui.helpers;

import javafx.animation.TranslateTransition;
import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class AnimationHelper {

    private static final int DEFAULT_MOVE_TIME_MS = 500;

    private AnimationHelper() {
    }

    public static Point2D centerOf(Rectangle rectangle) {
        return new Point2D(
                rectangle.getLayoutX() + rectangle.getWidth() / 2,
                rectangle.getLayoutY() + rectangle.getHeight() / 2
        );
    }

    public static void moveTo(Circle customer, Point2D target, Runnable onFinished) {
        Point2D current = new Point2D(
                customer.getLayoutX() + customer.getTranslateX(),
                customer.getLayoutY() + customer.getTranslateY()
        );

        TranslateTransition transition = new TranslateTransition(Duration.millis(DEFAULT_MOVE_TIME_MS), customer);
        transition.setFromX(current.getX() - customer.getLayoutX());
        transition.setFromY(current.getY() - customer.getLayoutY());
        transition.setToX(target.getX() - customer.getLayoutX());
        transition.setToY(target.getY() - customer.getLayoutY());

        transition.setOnFinished(event -> {
            customer.setLayoutX(target.getX());
            customer.setLayoutY(target.getY());
            customer.setTranslateX(0);
            customer.setTranslateY(0);

            if (onFinished != null) {
                onFinished.run();
            }
        });

        transition.play();
    }
}
