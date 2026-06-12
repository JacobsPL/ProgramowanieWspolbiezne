package org.example.events;

import javafx.application.Platform;
import org.example.gui.MainController;

public class GuiSimulationEventListener implements SimulationEventListener{


    private MainController controller;
    public GuiSimulationEventListener(MainController controller) {
        this.controller = controller;
    }

    @Override
    public void onEvent(SimulationEvent event) {
        Platform.runLater(()->controller.handleEvent(event));
    }
}
