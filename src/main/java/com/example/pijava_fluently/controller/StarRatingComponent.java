package com.example.pijava_fluently.controller;

import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;

public class StarRatingComponent extends HBox {

    private int selectedRating = 0;
    private final ToggleButton[] stars = new ToggleButton[5];

    public StarRatingComponent() {
        setSpacing(4);
        setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < 5; i++) {
            final int value = i + 1;
            ToggleButton btn = new ToggleButton("☆");
            btn.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;" +
                    "-fx-font-size:22px;-fx-text-fill:#F59E0B;-fx-cursor:hand;-fx-padding:0 2 0 2;");
            btn.setOnAction(e -> {
                selectedRating = value;
                updateStars(value);
            });
            stars[i] = btn;
            getChildren().add(btn);
        }
    }

    private void updateStars(int count) {
        for (int i = 0; i < 5; i++) {
            stars[i].setText(i < count ? "★" : "☆");
            stars[i].setSelected(i < count);
        }
    }

    public int getRating() { return selectedRating; }

    public void setRating(int rating) {
        selectedRating = rating;
        updateStars(rating);
    }
}