package com.example.pijava_fluently.utils;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class TranslateButton {

    public static void attachTo(Scene scene) {
        Parent existingRoot = scene.getRoot();
        HBox floatingBtn = buildFloatingButton(scene);

        if (existingRoot instanceof StackPane sp) {
            // Remove old translate button if already added
            sp.getChildren().removeIf(n -> "translateOverlay".equals(n.getId()));
            floatingBtn.setId("translateOverlay");
            sp.getChildren().add(floatingBtn);
        } else {
            StackPane wrapper = new StackPane();
            wrapper.setId("translateWrapper");
            wrapper.getChildren().add(existingRoot);
            floatingBtn.setId("translateOverlay");
            wrapper.getChildren().add(floatingBtn);
            scene.setRoot(wrapper);
        }
    }

    private static HBox buildFloatingButton(Scene scene) {
        ComboBox<String> langBox = new ComboBox<>();
        langBox.getItems().addAll("🇫🇷 Français", "🇬🇧 English");
        langBox.setValue("🇫🇷 Français");
        langBox.setStyle("""
            -fx-font-size: 12px;
            -fx-background-color: #3498db;
            -fx-text-fill: white;
            -fx-border-radius: 20;
            -fx-background-radius: 20;
            -fx-padding: 4 10;
            -fx-cursor: hand;
        """);
        langBox.setPrefWidth(145);

        langBox.setOnAction(e -> {
            String selected = langBox.getValue();
            String lang = switch (selected) {
                case "🇬🇧 English" -> "en";
                default -> "fr";
            };
            new Thread(() -> {
                Parent root = scene.getRoot();
                TranslationManager.translateScene(root, lang);
            }).start();
        });

        HBox container = new HBox(langBox);
        container.setAlignment(Pos.BOTTOM_RIGHT);
        container.setStyle("-fx-padding: 0 20 20 0;");
        container.setPickOnBounds(false);
        container.setViewOrder(-100);
        StackPane.setAlignment(container, Pos.BOTTOM_RIGHT);

        return container;
    }
}