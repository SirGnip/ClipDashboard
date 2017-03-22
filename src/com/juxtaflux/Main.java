package com.juxtaflux;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("../../ClipDashboard.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("NEW" + Config.APP_TITLE);
        primaryStage.setScene(new Scene(root, Config.APP_WIDTH, Config.APP_HEIGHT));
        primaryStage.show();
        Controller controller = loader.getController();
        controller.onReady(primaryStage);
        AppFramework.dump(root);
    }

    public static void main(String[] args) {
        launch(args);
    }
}


