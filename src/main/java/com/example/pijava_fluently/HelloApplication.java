package com.example.pijava_fluently;

import com.example.pijava_fluently.utils.MyDatabase;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.example.pijava_fluently.utils.TranslateButton;
import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        MyDatabase.getInstance().getConnection();

        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource(
                        "/com/example/pijava_fluently/fxml/login.fxml"
                )
        );


        Scene scene = new Scene(fxmlLoader.load(), 1000, 650);
        scene.getStylesheets().add(
                getClass().getResource(
                        "/com/example/pijava_fluently/css/fluently-admin.css"
                ).toExternalForm()
        );
        var css = HelloApplication.class.getResource(
                "/com/example/pijava_fluently/css/fluently.css"
        );
        if (css != null)
            scene.getStylesheets().add(css.toExternalForm());

        stage.setTitle("Fluently");
        stage.setScene(scene);
        TranslateButton.attachTo(scene);
        // Auto-attach translate button on every scene/root change
        scene.rootProperty().addListener((obs, oldRoot, newRoot) -> {
            if (newRoot != null) {
                javafx.application.Platform.runLater(() -> TranslateButton.attachTo(scene));
            }
        });
        stage.setResizable(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}