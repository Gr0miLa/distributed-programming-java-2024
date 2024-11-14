package org.example.battleship;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class BattleShipApp extends Application {
    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(BattleShipApp.class.getResource("main_scene.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            stage.setTitle("Battle Ship");
            stage.setScene(scene);

            stage.setOnCloseRequest(event -> {
                SceneController sceneController = fxmlLoader.getController();
                sceneController.handleCloseRequest();
            });

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
