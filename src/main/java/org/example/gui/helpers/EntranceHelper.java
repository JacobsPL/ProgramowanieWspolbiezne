package org.example.gui.helpers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class EntranceHelper {

    //Rectangles
    @FXML
    private Rectangle queueZone;
    @FXML
    private Rectangle entranceZone;

    //Panes
    @FXML
    private AnchorPane animationPane;

    private QueueHelper queueHelper;

    public EntranceHelper(AnchorPane animationPane,
                          Rectangle queueZone,
                          Rectangle entranceZone,
                          QueueHelper queueHelper) {
        this.animationPane = animationPane;
        this.queueZone = queueZone;
        this.entranceZone = entranceZone;
        this.queueHelper = queueHelper;
    }
    private void createCustomerWithDelay(double x, double y, int delay){
        Timeline timeline = new Timeline();

        KeyFrame keyFrame = new KeyFrame(
                Duration.millis(delay),
                event -> createSingleCustomer(x, y)
        );

        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    private void createSingleCustomer(double x, double y){

        double entranceX = entranceZone.getLayoutX() ;
        double entranceY = entranceZone.getLayoutY() ;

        Circle customer = new Circle(5);
        customer.setFill(Color.ORANGE);
        customer.setStroke(Color.BLACK);
        customer.setLayoutX(entranceX); // początkowa pozycja klienta
        customer.setLayoutY(entranceY);
        animationPane.getChildren().add(customer);

        TranslateTransition transition = new TranslateTransition(Duration.millis(500),customer);
        transition.setToX(x-entranceX); // przesunięcie o różnice między wejsciem i pozycją docelową
        transition.setToY(y-entranceY);

        transition.setOnFinished(event -> {
            customer.setLayoutX(x); // ustawienie na pozycji docelowej
            customer.setLayoutY(y);
            customer.setTranslateX(0);
            customer.setTranslateY(0);
            queueHelper.moveCustomerToCashier(customer);
        });



        transition.play();

    }



    public void createCustomer(int customerAmount){
        int padding = 20;
        int spacing = 20;

        double startQueueX = queueZone.getLayoutX() + spacing;
        double startQueueY = queueZone.getLayoutY() + spacing;

        double queueWidth = queueZone.getWidth()- 2 * spacing;
        double queueHeight = queueZone.getHeight()- 2 * spacing;

        int columnAmount = (int) (queueWidth/spacing);
        int rowAmount = (int) (queueHeight/spacing);

        int customersCreated = 0;
        for(int row = 0; row<rowAmount;row++){
            for(int column = 0; column <columnAmount; column++){
                if(customersCreated>= customerAmount) return;

                double x = startQueueX + column * padding;
                double y = startQueueY + row * padding;
                int delay = 1+ customersCreated * 150;
                createCustomerWithDelay(x,y,delay);

                customersCreated++;
            }

        }
    }
}
