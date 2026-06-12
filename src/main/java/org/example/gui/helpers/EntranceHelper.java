package org.example.gui.helpers;

import javafx.geometry.Point2D;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;
import java.util.Map;

public class EntranceHelper {

    private final Rectangle queueZone;
    private final Rectangle entranceZone;
    private final AnchorPane animationPane;
    private int lastRowTaken = 0;
    private int lastColumnTaken = 0;
    private Map <Integer, Circle> customers = new HashMap<>();
    public EntranceHelper(AnchorPane animationPane,
                          Rectangle queueZone,
                          Rectangle entranceZone
                          ) {
        this.animationPane = animationPane;
        this.queueZone = queueZone;
        this.entranceZone = entranceZone;
    }

    public void createCustomer(Integer customerID, boolean vipGroup, boolean childGroup, Runnable onFinished) {
        Point2D entrancePosition = AnimationHelper.centerOf(entranceZone);
        Point2D queuePosition = getNextQueuePosition();

        Circle customer = new Circle(5);
        customer.setFill(getCustomerColor(vipGroup, childGroup));
        customer.setStroke(Color.BLACK);
        customer.setLayoutX(entrancePosition.getX());
        customer.setLayoutY(entrancePosition.getY());
        customers.put(customerID,customer);

        animationPane.getChildren().add(customer);

        AnimationHelper.moveTo(customer, queuePosition, onFinished);
    }

    private Color getCustomerColor(boolean vipGroup, boolean childGroup) {
        if (vipGroup) {
            return Color.RED;
        }

        if (childGroup) {
            return Color.LIMEGREEN;
        }

        return Color.ORANGE;
    }

    public Point2D getNextQueuePosition (){
        int padding = 20;
        int spacing = 20;

        double startQueueX = queueZone.getLayoutX() + spacing;
        double startQueueY = queueZone.getLayoutY() + spacing;

        double queueWidth = queueZone.getWidth() - 2 * spacing;
        double queueHeight = queueZone.getHeight() - 2 * spacing;

        int columnAmount = (int) (queueWidth / spacing);
        int rowAmount = (int) (queueHeight / spacing);

        if(lastRowTaken<=rowAmount){
            if (lastColumnTaken>=columnAmount){
                lastRowTaken++;
                lastColumnTaken=0;
            }

                double x = startQueueX + lastColumnTaken * padding;
                double y = startQueueY + lastRowTaken * padding;
                lastColumnTaken++;
                return new Point2D(x, y);
        }else {
            //jeśli nie wszedł do ostatniego if to znaczy że skończyły się miejsca w kolejce i trzeba zacząć zapełniać od nowa
            lastColumnTaken=0;
            lastRowTaken=0;
            double x = startQueueX + lastColumnTaken * padding;
            double y = startQueueY + lastRowTaken * padding;
            lastColumnTaken++;
            return new Point2D(x, y);
        }
    }

    public Circle getCustomer(int customerId) {
        return customers.get(customerId);
    }

    public void removeCustomer(int customerId) {
        customers.remove(customerId);
    }

    public void clear() {
        customers.clear();
        lastRowTaken = 0;
        lastColumnTaken = 0;
    }
}
