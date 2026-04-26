package com.example.pijava_fluently.services;

import com.example.pijava_fluently.controller.HomeController;
import com.example.pijava_fluently.controller.ObjectifController;
import com.example.pijava_fluently.entites.Objectif;
import com.example.pijava_fluently.entites.Tache;
import com.example.pijava_fluently.entites.User;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class NotificationService {

    private static HomeController homeController;
    private static ObjectifController objectifController;
    private static ObjectifService objectifService = new ObjectifService();
    private static TacheService tacheService = new TacheService();

    private static List<NotificationItem> allNotifications = new ArrayList<>();
    private static Map<Integer, Set<String>> shownAlerts = new HashMap<>();

    public static class NotificationItem {
        public final String type;
        public final int id;
        public final String title;
        public final String message;
        public final String severity;
        public final LocalDate date;
        private boolean read;

        public NotificationItem(String type, int id, String title, String message, String severity, LocalDate date) {
            this.type = type;
            this.id = id;
            this.title = title;
            this.message = message;
            this.severity = severity;
            this.date = date;
            this.read = false;
        }

        public boolean isRead() { return read; }
        public void setRead(boolean read) { this.read = read; }
    }

    public static void setHomeController(HomeController hc) {
        homeController = hc;
    }

    public static void setObjectifController(ObjectifController oc) {
        objectifController = oc;
    }

    public static void checkAndNotify(User user, List<Objectif> objectifs, List<Tache> taches) {
        if (user == null) return;

        int userId = user.getId();
        Set<String> shownThisSession = shownAlerts.computeIfAbsent(userId, k -> new HashSet<>());
        LocalDate today = LocalDate.now();
        LocalDate seuilAlerte = today.plusDays(3);

        List<NotificationItem> nouvellesNotifications = new ArrayList<>();

        // Vérifier les objectifs
        for (Objectif obj : objectifs) {
            if (obj.getIdUserId() != userId) continue;
            if ("Terminé".equals(obj.getStatut())) continue;
            if (obj.getDateFin() == null) continue;

            String key = "obj-" + obj.getId();

            if (obj.getDateFin().isBefore(today)) {
                if (!shownThisSession.contains(key)) {
                    nouvellesNotifications.add(new NotificationItem(
                            "objectif", obj.getId(), obj.getTitre(),
                            "⚠️ OBJECTIF EN RETARD ! Date limite dépassée : " + obj.getDateFin(),
                            "urgent", LocalDate.now()
                    ));
                    shownThisSession.add(key);
                }
            } else if (!obj.getDateFin().isAfter(seuilAlerte)) {
                long jours = ChronoUnit.DAYS.between(today, obj.getDateFin());
                if (!shownThisSession.contains(key)) {
                    nouvellesNotifications.add(new NotificationItem(
                            "objectif", obj.getId(), obj.getTitre(),
                            "⏰ Objectif à terminer dans " + jours + " jour(s) (avant le " + obj.getDateFin() + ")",
                            "warning", LocalDate.now()
                    ));
                    shownThisSession.add(key);
                }
            }
        }

        Set<Integer> userObjectifIds = objectifs.stream()
                .filter(o -> o.getIdUserId() == userId)
                .map(Objectif::getId)
                .collect(Collectors.toSet());

        Map<Integer, String> objectifTitles = objectifs.stream()
                .collect(Collectors.toMap(Objectif::getId, Objectif::getTitre, (a, b) -> a));

        // Vérifier les tâches
        for (Tache tache : taches) {
            if (!userObjectifIds.contains(tache.getIdObjectifId())) continue;
            if ("Terminée".equals(tache.getStatut())) continue;
            if (tache.getDateLimite() == null) continue;

            String key = "tache-" + tache.getId();
            String objectifTitle = objectifTitles.getOrDefault(tache.getIdObjectifId(), "Objectif");

            if (tache.getDateLimite().isBefore(today)) {
                if (!shownThisSession.contains(key)) {
                    nouvellesNotifications.add(new NotificationItem(
                            "tache", tache.getId(), tache.getTitre(),
                            "⚠️ TÂCHE EN RETARD ! (" + objectifTitle + ") - Date limite dépassée: " + tache.getDateLimite(),
                            "urgent", LocalDate.now()
                    ));
                    shownThisSession.add(key);
                }
            } else if (!tache.getDateLimite().isAfter(seuilAlerte)) {
                long jours = ChronoUnit.DAYS.between(today, tache.getDateLimite());
                if (jours >= 0 && !shownThisSession.contains(key)) {
                    nouvellesNotifications.add(new NotificationItem(
                            "tache", tache.getId(), tache.getTitre(),
                            "⏰ Tâche à terminer dans " + jours + " jour(s) (" + objectifTitle + ") - Limite: " + tache.getDateLimite(),
                            "warning", LocalDate.now()
                    ));
                    shownThisSession.add(key);
                }
            }
        }

        if (!nouvellesNotifications.isEmpty()) {
            allNotifications.addAll(0, nouvellesNotifications);
            showPopupNotification(nouvellesNotifications, user.getPrenom());
        }
    }

    public static List<NotificationItem> getAllNotifications() {
        return new ArrayList<>(allNotifications);
    }

    public static int getUnreadCount() {
        return (int) allNotifications.stream().filter(n -> !n.isRead()).count();
    }

    public static void markAsRead(int notificationId, String type) {
        for (NotificationItem n : allNotifications) {
            if (n.id == notificationId && n.type.equals(type)) {
                n.setRead(true);
                break;
            }
        }
    }

    public static void markAllAsRead() {
        allNotifications.forEach(n -> n.setRead(true));
    }

    public static void clearAllNotifications() {
        allNotifications.clear();
    }

    public static void openNotificationCenter() {
        if (homeController == null) {
            System.err.println("NotificationService: homeController est null");
            return;
        }

        Stage stage = new Stage();
        stage.setTitle("Centre de notifications");
        stage.setResizable(false);
        stage.initStyle(StageStyle.UTILITY);

        VBox root = new VBox(10);
        root.setStyle("-fx-background-color: #F8FAFC; -fx-padding: 20;");
        root.setPrefWidth(550);
        root.setPrefHeight(500);

        Label titleLabel = new Label("Centre de notifications");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        HBox headerActions = new HBox();
        headerActions.setAlignment(Pos.CENTER_RIGHT);

        Button markAllReadBtn = new Button("✓ Tout marquer comme lu");
        markAllReadBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 11px; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 6 12 6 12; -fx-cursor: hand;");
        markAllReadBtn.setOnAction(e -> {
            markAllAsRead();
            openNotificationCenter();
        });

        headerActions.getChildren().add(markAllReadBtn);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox notificationsContainer = new VBox(8);
        notificationsContainer.setPadding(new Insets(10));

        if (allNotifications.isEmpty()) {
            VBox emptyBox = new VBox(15);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            Label icon = new Label("🔔");
            icon.setStyle("-fx-font-size: 48px;");
            Label msg = new Label("Aucune notification");
            msg.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B;");
            emptyBox.getChildren().addAll(icon, msg);
            notificationsContainer.getChildren().add(emptyBox);
        } else {
            for (NotificationItem notif : allNotifications) {
                VBox card = createNotificationCard(notif);
                notificationsContainer.getChildren().add(card);
            }
        }

        scrollPane.setContent(notificationsContainer);

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8 20 8 20; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> stage.close());

        root.getChildren().addAll(titleLabel, headerActions, scrollPane, closeBtn);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    private static VBox createNotificationCard(NotificationItem notif) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: " + (notif.isRead() ? "white" : "#F0F9FF") + "; " +
                "-fx-background-radius: 12; " +
                "-fx-border-color: #E2E8F0; -fx-border-radius: 12; -fx-border-width: 1; " +
                "-fx-cursor: hand;");

        if (!notif.isRead()) {
            card.setStyle(card.getStyle() + "-fx-border-color: #6C63FF; -fx-border-width: 0 0 0 4;");
        }

        String borderColor = notif.severity.equals("urgent") ? "#EF4444" : "#F59E0B";
        card.setStyle(card.getStyle() + "-fx-border-left-color: " + borderColor + ";");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(notif.type.equals("objectif") ? "🎯" : "📋");
        iconLabel.setStyle("-fx-font-size: 20px;");

        Label titleLabel = new Label(notif.title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label severityBadge = new Label(notif.severity.equals("urgent") ? "⚠️ URGENT" : "⏰ RAPPEL");
        severityBadge.setStyle("-fx-background-color: " + (notif.severity.equals("urgent") ? "#FEE2E2" : "#FEF3C7") + ";" +
                "-fx-text-fill: " + (notif.severity.equals("urgent") ? "#DC2626" : "#D97706") + ";" +
                "-fx-font-size: 9px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 2 8 2 8;");

        Label dateLabel = new Label(notif.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");

        header.getChildren().addAll(iconLabel, titleLabel, spacer, severityBadge, dateLabel);

        Label messageLabel = new Label(notif.message);
        messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-wrap-text: true;");
        messageLabel.setWrapText(true);

        HBox actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);
        actionsBox.setPadding(new Insets(8, 0, 0, 0));

        Button voirDetailsBtn = new Button("👁 Voir les détails");
        voirDetailsBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 8; " +
                "-fx-padding: 6 12 6 12; -fx-cursor: hand;");
        voirDetailsBtn.setOnAction(e -> {
            if (notif.type.equals("objectif")) {
                if (objectifController != null) {
                    objectifController.navigateToObjectifAndShowDetails(notif.id);
                }
            } else {
                try {
                    Tache tache = tacheService.recupererParId(notif.id);
                    if (tache != null && objectifController != null) {
                        objectifController.navigateToTacheAndShowDetails(tache);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (!notif.isRead()) {
                markAsRead(notif.id, notif.type);
            }
            Stage stage = (Stage) card.getScene().getWindow();
            if (stage != null) stage.close();
        });

        actionsBox.getChildren().add(voirDetailsBtn);
        card.getChildren().addAll(header, messageLabel, actionsBox);

        card.setOnMouseClicked(e -> {
            if (notif.type.equals("objectif")) {
                if (objectifController != null) {
                    objectifController.navigateToObjectifAndShowDetails(notif.id);
                }
            }
            if (!notif.isRead()) {
                markAsRead(notif.id, notif.type);
            }
            Stage stage = (Stage) card.getScene().getWindow();
            if (stage != null) stage.close();
        });

        return card;
    }

    private static void showPopupNotification(List<NotificationItem> notifications, String userName) {
        Stage stage = new Stage();
        stage.setTitle("Nouvelles notifications");
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setOpacity(0);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.45), 24, 0, 0, 8);");
        root.setPrefWidth(400);
        root.setMaxWidth(400);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 16, 14, 16));
        header.setStyle("-fx-background-color: linear-gradient(to right, #6C63FF, #8B5CF6); " +
                "-fx-background-radius: 20 20 0 0;");

        Label bell = new Label("🔔");
        bell.setStyle("-fx-font-size: 20px;");

        RotateTransition rotate = new RotateTransition(Duration.millis(500), bell);
        rotate.setByAngle(20);
        rotate.setCycleCount(3);
        rotate.setAutoReverse(true);
        rotate.play();

        VBox headerText = new VBox(3);
        Label titleLbl = new Label("Nouvelles notifications");
        titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label countLbl = new Label(notifications.size() + " notification(s)");
        countLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.75);");
        headerText.getChildren().addAll(titleLbl, countLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; " +
                "-fx-background-radius: 50; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
        closeBtn.setOnAction(e -> fadeOutAndClose(stage));

        header.getChildren().addAll(bell, headerText, spacer, closeBtn);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(350);

        VBox body = new VBox(8);
        body.setPadding(new Insets(12));

        int maxDisplay = Math.min(5, notifications.size());
        for (int i = 0; i < maxDisplay; i++) {
            NotificationItem n = notifications.get(i);
            VBox notifCard = createCompactNotificationCard(n);
            body.getChildren().add(notifCard);
        }

        if (notifications.size() > 5) {
            Label moreLabel = new Label("+ " + (notifications.size() - 5) + " autre(s) notification(s)");
            moreLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6C63FF; -fx-padding: 5 0 5 12; -fx-cursor: hand;");
            moreLabel.setOnMouseClicked(e -> {
                fadeOutAndClose(stage);
                openNotificationCenter();
            });
            body.getChildren().add(moreLabel);
        }

        scrollPane.setContent(body);

        HBox buttonsBox = new HBox(10);
        buttonsBox.setPadding(new Insets(12, 16, 16, 16));
        buttonsBox.setAlignment(Pos.CENTER);

        Button voirBtn = new Button("📋 Voir toutes");
        voirBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-size: 12px; " +
                "-fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        voirBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(voirBtn, Priority.ALWAYS);
        voirBtn.setOnAction(e -> {
            fadeOutAndClose(stage);
            openNotificationCenter();
        });

        Button okBtn = new Button("OK");
        okBtn.setStyle("-fx-background-color: #6C63FF; -fx-text-fill: white; -fx-font-size: 12px; " +
                "-fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 8 24 8 24; -fx-cursor: hand;");
        okBtn.setOnAction(e -> fadeOutAndClose(stage));

        buttonsBox.getChildren().addAll(voirBtn, okBtn);

        root.getChildren().addAll(header, scrollPane, buttonsBox);

        Scene scene = new Scene(root);
        scene.setFill(null);
        stage.setScene(scene);
        stage.show();

        stage.setX(javafx.stage.Screen.getPrimary().getVisualBounds().getWidth() - root.getWidth() - 20);
        stage.setY(javafx.stage.Screen.getPrimary().getVisualBounds().getHeight() - root.getHeight() - 60);

        javafx.animation.Timeline fadeIn = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.millis(300),
                        new javafx.animation.KeyValue(stage.opacityProperty(), 1.0))
        );
        fadeIn.play();

        PauseTransition delay = new PauseTransition(Duration.seconds(8));
        delay.setOnFinished(e -> fadeOutAndClose(stage));
        delay.play();
    }

    private static VBox createCompactNotificationCard(NotificationItem n) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 10; -fx-cursor: hand;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(n.type.equals("objectif") ? "🎯" : "📋");
        icon.setStyle("-fx-font-size: 14px;");

        Label title = new Label(n.title.length() > 30 ? n.title.substring(0, 27) + "..." : n.title);
        title.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label severity = new Label(n.severity.equals("urgent") ? "⚠️" : "⏰");
        severity.setStyle("-fx-font-size: 11px;");

        header.getChildren().addAll(icon, title, spacer, severity);

        Label message = new Label(n.message.length() > 60 ? n.message.substring(0, 57) + "..." : n.message);
        message.setStyle("-fx-font-size: 10px; -fx-text-fill: #CBD5E1; -fx-wrap-text: true;");
        message.setWrapText(true);

        card.getChildren().addAll(header, message);

        card.setOnMouseClicked(e -> {
            Stage stage = (Stage) card.getScene().getWindow();
            fadeOutAndClose(stage);
            if (homeController != null) {
                homeController.showObjectifs();
                if (objectifController != null && n.type.equals("objectif")) {
                    objectifController.navigateToObjectifAndShowDetails(n.id);
                }
            }
        });

        return card;
    }

    private static void fadeOutAndClose(Stage stage) {
        javafx.animation.Timeline fadeOut = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.millis(300),
                        new javafx.animation.KeyValue(stage.opacityProperty(), 0.0))
        );
        fadeOut.setOnFinished(e -> stage.close());
        fadeOut.play();
    }
}