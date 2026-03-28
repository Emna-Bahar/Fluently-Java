package com.example.pijava_fluently;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        MyDatabase.getConnection();
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("admin-dashboard.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 650);
        scene.getStylesheets().add(HelloApplication.class.getResource("fluently.css").toExternalForm());
        stage.setTitle("Fluently");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();

    }

    public static void main(String[] args) {
        launch();
    }
}