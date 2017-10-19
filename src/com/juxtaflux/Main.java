package com.juxtaflux;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        final String UI_LAYOUT_FILENAME = "/ClipDashboard.fxml";
        final String ICON_FILENAME = "/ClipDashboardIcon.ico";
        System.out.println("Application starting. Loading UI layout from " + UI_LAYOUT_FILENAME);
        FXMLLoader loader = new FXMLLoader(getClass().getResource(UI_LAYOUT_FILENAME));
        Parent root;
        try {
            root = loader.load();
        } catch (Exception e) {
            System.out.println("ERROR: Problem loading UI layout: " + UI_LAYOUT_FILENAME);
            throw e;
        }

        System.out.println("Loading app icon: " + ICON_FILENAME);
        InputStream iconInputStream = getClass().getResourceAsStream(ICON_FILENAME);
        if (iconInputStream == null) {
            throw new Exception("Problem loading app icon: " + ICON_FILENAME);
        }
        primaryStage.getIcons().add(new Image(iconInputStream));
        primaryStage.setTitle(Config.APP_TITLE);
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


