package org.example.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;
import org.example.config.AppConfig;
import org.example.gui.helpers.EntranceHelper;
import org.example.gui.helpers.QueueHelper;

import java.util.List;

public class MainController {
    AppConfig config = AppConfig.getInstance();

    //Buttons
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;

    // Spinners
    @FXML
    private Spinner customerCounterSpinner;


    //Labels
    @FXML
    private Label cleaningStatusLabel;

    //Rectangles
    @FXML
    private Rectangle queueZone;
    @FXML
    private Rectangle entranceZone;

    @FXML
    private Rectangle cashierZone1;
    @FXML
    private Rectangle cashierZone2;
    @FXML
    private Rectangle cashierZone3;

    @FXML
    private Rectangle gateZone;
    private List<Rectangle> cashierZones;

    //Panes
    @FXML
    private AnchorPane animationPane;

    @FXML
    private void initialize() {

        customerCounterSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory
                        (1,999,config.getInt("simulation.customer.groups")) {
        });

        cashierZones = List.of(cashierZone1,cashierZone2,cashierZone3);
        QueueHelper queueHelper = new QueueHelper(cashierZones,gateZone);
        EntranceHelper entranceHelper = new EntranceHelper(animationPane,queueZone,entranceZone,queueHelper);



        startButton.setOnAction(event ->{
                    //animationPane.getChildren().clear();
                    cleaningStatusLabel.setText("Status sprzatania: symulacja uruchomiona");
                    entranceHelper.createCustomer((Integer) customerCounterSpinner.getValue());
                }
                );

    }


}
