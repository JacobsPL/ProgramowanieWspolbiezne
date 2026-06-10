package org.example.gui.helpers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class QueueHelper {


    private List<Rectangle> cashierZones;

    private Rectangle gateZone;

    public QueueHelper (List<Rectangle> cashierZones, Rectangle gateZone){
            this.cashierZones=cashierZones;
            this.gateZone=gateZone;
    }

    public void moveCustomerToCashier(Circle customer){
        Timeline timeline = new Timeline();
        int cashierRandomNumber = ThreadLocalRandom.current().nextInt(cashierZones.size()); // trzeba potem zmienić na medote z backendu pewnie
        int delay = ThreadLocalRandom.current().nextInt(200,500);

        KeyFrame keyFrame = new KeyFrame(
                Duration.millis(delay),
                event -> transitionToCashier(customer, cashierZones.get(cashierRandomNumber))
        );

        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    private void transitionToCashier (Circle customer, Rectangle cashier){

        double cashierX = cashier.getLayoutX() ;
        double cashierY = cashier.getLayoutY() ;

        double customerX = customer.getLayoutX() ;
        double customerY = customer.getLayoutY() ;

        TranslateTransition transition = new TranslateTransition(Duration.millis(500),customer);
        transition.setToX(cashierX-customerX); // przesunięcie o różnice między wejsciem i pozycją docelową
        transition.setToY(cashierY-customerY);

        transition.setOnFinished(event -> {
            customer.setLayoutX(cashierX); // ustawienie na pozycji docelowej
            customer.setLayoutY(cashierY);
            customer.setTranslateX(0);
            customer.setTranslateY(0);

            moveCustomerToGate(customer);
        });

        transition.play();
    }

    private void moveCustomerToGate(Circle customer){
        Timeline timeline = new Timeline();
        int delay = ThreadLocalRandom.current().nextInt(200,500);

        KeyFrame keyFrame = new KeyFrame(
                Duration.millis(delay),
                event -> transitionToGate(customer)
        );

        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    private void transitionToGate(Circle customer){
        double gateZoneX = gateZone.getLayoutX() ;
        double gateZoneY = gateZone.getLayoutY() ;

        double customerX = customer.getLayoutX() ;
        double customerY = customer.getLayoutY() ;

        TranslateTransition transition = new TranslateTransition(Duration.millis(500),customer);
        transition.setToX(gateZoneX-customerX); // przesunięcie o różnice między wejsciem i pozycją docelową
        transition.setToY(gateZoneY-customerY);

        transition.setOnFinished(event -> {
            customer.setLayoutX(gateZoneX); // ustawienie na pozycji docelowej
            customer.setLayoutY(gateZoneY);
            customer.setTranslateX(0);
            customer.setTranslateY(0);
        });

        transition.play();
    }
}
