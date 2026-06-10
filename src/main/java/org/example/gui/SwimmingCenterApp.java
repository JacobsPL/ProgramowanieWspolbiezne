package org.example.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SwimmingCenterApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/SwimingCenter.fxml"));

        Scene mainScene = new Scene(fxmlLoader.load());
        stage.setTitle("Swimming Center App");
        stage.setScene(mainScene);
        stage.show();
    }

    public static void main(String[] args){
        launch(args);
    }
}
