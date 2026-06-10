package org.example.gui.helpers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class EntranceHelper {

    private final Rectangle queueZone;
    private final Rectangle entranceZone;
    private final AnchorPane animationPane;
    private final QueueHelper queueHelper;

    public EntranceHelper(AnchorPane animationPane,
                          Rectangle queueZone,
                          Rectangle entranceZone,
                          QueueHelper queueHelper) {
        this.animationPane = animationPane;
        this.queueZone = queueZone;
        this.entranceZone = entranceZone;
        this.queueHelper = queueHelper;
    }

    private void createCustomerWithDelay(Point2D queuePosition, int delay) {
        Timeline timeline = new Timeline();

        KeyFrame keyFrame = new KeyFrame(
                Duration.millis(delay),
                event -> createSingleCustomer(queuePosition)
        );

        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    private void createSingleCustomer(Point2D queuePosition) {
        Point2D entrancePosition = AnimationHelper.centerOf(entranceZone);

        Circle customer = new Circle(5);
        customer.setFill(Color.ORANGE);
        customer.setStroke(Color.BLACK);
        customer.setLayoutX(entrancePosition.getX());
        customer.setLayoutY(entrancePosition.getY());
        animationPane.getChildren().add(customer);

        AnimationHelper.moveTo(customer, queuePosition, () -> queueHelper.moveCustomerToCashier(customer));
    }

    public void createCustomer(int customerAmount) {
        int padding = 20;
        int spacing = 20;

        double startQueueX = queueZone.getLayoutX() + spacing;
        double startQueueY = queueZone.getLayoutY() + spacing;

        double queueWidth = queueZone.getWidth() - 2 * spacing;
        double queueHeight = queueZone.getHeight() - 2 * spacing;

        int columnAmount = (int) (queueWidth / spacing);
        int rowAmount = (int) (queueHeight / spacing);

        int customersCreated = 0;
        for (int row = 0; row < rowAmount; row++) {
            for (int column = 0; column < columnAmount; column++) {
                if (customersCreated >= customerAmount) return;

                double x = startQueueX + column * padding;
                double y = startQueueY + row * padding;
                Point2D queuePosition = new Point2D(x, y);
                int delay = 1 + customersCreated * 150;
                createCustomerWithDelay(queuePosition, delay);

                customersCreated++;
            }
        }
    }
}
