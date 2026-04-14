package com.example.pijava_fluently.controller;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;

import java.awt.*;
import java.net.URI;

public class HomeContentController {

    private HomeController homeController;

    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
    }

    @FXML
    private void onStartLearning() {
        if (homeController != null) {
            homeController.showMesTests();
        }
    }

    @FXML
    private void onExploreCourses() {
        if (homeController != null) {
            homeController.showLangues();
        }
    }

    @FXML
    private void onTestsClick() {
        if (homeController != null) {
            homeController.showMesTests();
        }
    }

    @FXML
    private void onGroupesClick() {
        if (homeController != null) {
            homeController.showGroupes();
        }
    }

    @FXML
    private void onObjectifsClick() {
        if (homeController != null) {
            homeController.showObjectifs();
        }
    }

    @FXML
    private void onSessionsClick() {
        if (homeController != null) {
            homeController.showSessions();
        }
    }

    @FXML
    private void onCertificatsClick() {
        System.out.println("Navigation vers les certificats");
        if (homeController != null) {
            // homeController.showCertificats();
        }
    }

    @FXML
    private void onStatsClick() {
        System.out.println("Navigation vers les statistiques");
        if (homeController != null) {
            // homeController.showStats();
        }
    }

    @FXML
    private void onViewAllLanguages() {
        if (homeController != null) {
            homeController.showLangues();
        }
    }

    @FXML
    private void openFacebook() {
        openUrl("https://facebook.com/fluently");
    }

    @FXML
    private void openTwitter() {
        openUrl("https://twitter.com/fluently");
    }

    @FXML
    private void openInstagram() {
        openUrl("https://instagram.com/fluently");
    }

    @FXML
    private void openLinkedin() {
        openUrl("https://linkedin.com/company/fluently");
    }

    private void openUrl(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            System.err.println("Impossible d'ouvrir l'URL: " + url);
            e.printStackTrace();
        }
    }
}