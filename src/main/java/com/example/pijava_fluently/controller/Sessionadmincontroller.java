package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Session;
import com.example.pijava_fluently.services.Sessionservice;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.Node;
import javafx.stage.Modality;


public class Sessionadmincontroller {

    @FXML private VBox      rootPane;
    @FXML private Label     countSessionsLabel;
    @FXML private Label     statPlanifiee;
    @FXML private Label     statEnCours;
    @FXML private Label     statTerminee;
    @FXML private Label     statAnnulee;
    @FXML private TextField searchSessions;
    @FXML private ComboBox<String> filterStatutSession;
    @FXML private FlowPane  sessionsContainer;

    private final Sessionservice sessionservice = new Sessionservice();
    private ObservableList<Session> allSessions = FXCollections.observableArrayList();

    private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        filterStatutSession.setItems(FXCollections.observableArrayList(
                "Tous", "planifiée", "en cours", "terminée", "annulée"));
        filterStatutSession.setValue("Tous");
        filterStatutSession.setOnAction(e -> applyFilter());
        loadAll();
    }

    private void loadAll() {
        try {
            allSessions = FXCollections.observableArrayList(sessionservice.recuperer());
            updateStats();
            applyFilter();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private boolean isDarkMode() {
        if (rootPane == null || rootPane.getScene() == null) return false;
        return rootPane.getScene().getStylesheets().stream()
                .anyMatch(s -> s.contains("dark.css"));
    }

    private void updateStats() {
        long planifiee = allSessions.stream().filter(s -> "planifiée".equals(s.getStatut())).count();
        long enCours   = allSessions.stream().filter(s -> "en cours".equals(s.getStatut())).count();
        long terminee  = allSessions.stream().filter(s -> "terminée".equals(s.getStatut())).count();
        long annulee   = allSessions.stream().filter(s -> "annulée".equals(s.getStatut())).count();

        setStatCard(statPlanifiee, planifiee, "59,130,246",  "#DBEAFE", "#2563EB");
        setStatCard(statEnCours,   enCours,   "108,99,255",  "#E0E7FF", "#6C63FF");
        setStatCard(statTerminee,  terminee,  "16,185,129",  "#D1FAE5", "#059669");
        setStatCard(statAnnulee,   annulee,   "239,68,68",   "#FEE2E2", "#DC2626");

        if (countSessionsLabel != null)
            countSessionsLabel.setText(allSessions.size() + " session(s)");
    }

    private void setStatCard(Label lbl, long count, String rgbValues,
                             String borderColor, String textColor) {
        if (lbl == null) return;
        lbl.setText(String.valueOf(count));
        lbl.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:" + textColor + ";");
        if (lbl.getParent() != null) {
            boolean dark = isDarkMode();
            String bg = dark ? "#111827" : "white";
            double op = dark ? 0.28 : 0.14;
            lbl.getParent().setStyle(
                    "-fx-background-color:" + bg + ";" +
                            "-fx-background-radius:18;" +
                            "-fx-border-color:" + borderColor + ";" +
                            "-fx-border-radius:18;" +
                            "-fx-border-width:2;" +
                            "-fx-padding:22 14 22 14;" +
                            "-fx-effect:dropshadow(gaussian,rgba(" + rgbValues + "," + op + "),16,0,0,4);"
            );
        }
    }

    @FXML private void handleSearchSessions() { applyFilter(); }

    private void applyFilter() {
        String q  = searchSessions != null ? searchSessions.getText().toLowerCase().trim() : "";
        String st = filterStatutSession != null ? filterStatutSession.getValue() : "Tous";
        List<Session> filtered = allSessions.stream()
                .filter(s -> {
                    boolean mq = q.isEmpty()
                            || (s.getDescription() != null && s.getDescription().toLowerCase().contains(q))
                            || (s.getStatut() != null && s.getStatut().toLowerCase().contains(q));
                    boolean ms = "Tous".equals(st) || st.equals(s.getStatut());
                    return mq && ms;
                }).collect(Collectors.toList());
        renderSessions(filtered);
        if (countSessionsLabel != null)
            countSessionsLabel.setText(filtered.size() + " session(s)");
    }

    private void renderSessions(List<Session> list) {
        sessionsContainer.getChildren().clear();
        int i = 0;
        for (Session s : list)
            sessionsContainer.getChildren().add(buildCard(s, i++));
        if (list.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            Label icon = new Label("📭");
            icon.setStyle("-fx-font-size:48px;");
            Label msg = new Label("Aucune session trouvée");
            msg.setStyle("-fx-font-size:15px;-fx-text-fill:#64748B;");
            empty.getChildren().addAll(icon, msg);
            sessionsContainer.getChildren().add(empty);
        }
    }

    private VBox buildCard(Session s, int ci) {
        String c1, c2, rgb;
        switch (s.getStatut() != null ? s.getStatut() : "") {
            case "planifiée" -> { c1 = "#3B82F6"; c2 = "#2563EB"; rgb = "59,130,246"; }
            case "en cours"  -> { c1 = "#6C63FF"; c2 = "#8B5CF6"; rgb = "108,99,255"; }
            case "terminée"  -> { c1 = "#10B981"; c2 = "#059669"; rgb = "16,185,129"; }
            case "annulée"   -> { c1 = "#EF4444"; c2 = "#DC2626"; rgb = "239,68,68"; }
            default           -> { c1 = "#6366F1"; c2 = "#8B5CF6"; rgb = "99,102,241"; }
        }
        final String fc1 = c1, fc2 = c2, frgb = rgb;

        boolean dark = isDarkMode();
        String cardBg      = dark ? "#1E293B" : "white";
        String cardBgHover = dark ? "#243044" : "#F8FAFC";
        String descColor   = dark ? "#94A3B8" : "#64748B";
        String sepColor    = dark ? "#334155" : "#E2E8F0";
        String btnBg       = dark ? "#1D3461" : "#EFF6FF";
        String btnFg       = dark ? "#60A5FA" : "#2563EB";

        VBox card = new VBox(0);
        card.setPrefWidth(290);
        card.setMaxWidth(290);
        card.getStyleClass().add("content-card");
        card.setStyle(
                "-fx-background-color:" + cardBg + ";" +
                        "-fx-background-radius:20;" +
                        "-fx-border-color:" + c1 + ";" +
                        "-fx-border-radius:20;" +
                        "-fx-border-width:1.5;" +
                        "-fx-effect:dropshadow(gaussian,rgba(" + rgb + ",0.18),18,0,0,5);"
        );
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:" + cardBgHover + ";" +
                        "-fx-background-radius:20;" +
                        "-fx-border-color:" + fc1 + ";" +
                        "-fx-border-radius:20;" +
                        "-fx-border-width:2;" +
                        "-fx-effect:dropshadow(gaussian,rgba(" + frgb + ",0.36),24,0,0,8);" +
                        "-fx-scale-x:1.02;-fx-scale-y:1.02;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:" + cardBg + ";" +
                        "-fx-background-radius:20;" +
                        "-fx-border-color:" + fc1 + ";" +
                        "-fx-border-radius:20;" +
                        "-fx-border-width:1.5;" +
                        "-fx-effect:dropshadow(gaussian,rgba(" + frgb + ",0.18),18,0,0,5);"
        ));

        // ── Header gradient (toujours coloré)
        VBox header = new VBox(8);
        header.setPadding(new Insets(18, 20, 14, 20));
        header.setStyle(
                "-fx-background-color:linear-gradient(to bottom right," + c1 + "," + c2 + ");" +
                        "-fx-background-radius:18 18 0 0;"
        );
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(getStatutIcon(s.getStatut()));
        iconLbl.setStyle("-fx-font-size:18px;-fx-background-color:rgba(255,255,255,0.22);" +
                "-fx-background-radius:50;-fx-padding:6 8 6 8;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label idBadge = new Label("🔒 #" + s.getId());
        idBadge.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;" +
                "-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 9 3 9;");
        topRow.getChildren().addAll(iconLbl, sp, idBadge);
        Label dateLbl = new Label(s.getDateHeure() != null ? s.getDateHeure().format(FMT_DT) : "Date à définir");
        dateLbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        header.getChildren().addAll(topRow, dateLbl);

        // ── Body
        VBox body = new VBox(10);
        body.setPadding(new Insets(14, 18, 12, 18));
        body.setStyle("-fx-background-color:transparent;");

        Label statutBadge = buildStatutBadge(s.getStatut(), dark);

        String desc = s.getDescription() != null && !s.getDescription().isBlank()
                ? (s.getDescription().length() > 70 ? s.getDescription().substring(0, 67) + "…" : s.getDescription())
                : "Aucune description.";
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + descColor + ";-fx-wrap-text:true;");
        descLbl.setWrapText(true);

        HBox chips = new HBox(8);
        if (s.getDuree() != null) {
            String chipBg = dark ? "#1D3461" : "#EFF6FF";
            String chipFg = dark ? "#60A5FA" : "#2563EB";
            Label dl = new Label("⏱ " + s.getDuree() + " min");
            dl.setStyle("-fx-background-color:" + chipBg + ";-fx-text-fill:" + chipFg + ";" +
                    "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
            chips.getChildren().add(dl);
        }
        if (s.getPrix() != null) {
            String chipBg = dark ? "#064E3B" : "#ECFDF5";
            String chipFg = dark ? "#34D399" : "#059669";
            Label pl = new Label("💰 " + String.format("%.0f", s.getPrix()) + " TND");
            pl.setStyle("-fx-background-color:" + chipBg + ";-fx-text-fill:" + chipFg + ";" +
                    "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
            chips.getChildren().add(pl);
        }
        if (s.getCapaciteMax() != null) {
            String chipBg = dark ? "#431407" : "#FFF7ED";
            String chipFg = dark ? "#FB923C" : "#EA580C";
            Label cl = new Label("🪑 " + s.getCapaciteMax());
            cl.setStyle("-fx-background-color:" + chipBg + ";-fx-text-fill:" + chipFg + ";" +
                    "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
            chips.getChildren().add(cl);
        }
        body.getChildren().addAll(statutBadge, descLbl, chips);

        if (s.getLienReunion() != null && !s.getLienReunion().isBlank()) {
            String linkColor = dark ? "#818CF8" : "#6366F1";
            Label lienLbl = new Label("🔗 " + (s.getLienReunion().length() > 32
                    ? s.getLienReunion().substring(0, 29) + "…" : s.getLienReunion()));
            lienLbl.setStyle("-fx-font-size:11px;-fx-text-fill:" + linkColor + ";");
            body.getChildren().add(lienLbl);
        }

        // ── Footer
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:" + sepColor + ";-fx-border-color:" + sepColor + ";");
        VBox.setMargin(sep, new Insets(4, 0, 0, 0));

        HBox footer = new HBox(8);
        footer.setPadding(new Insets(10, 14, 14, 14));
        footer.setAlignment(Pos.CENTER);
        Button btnVoir = new Button("👁  Voir les détails");
        btnVoir.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnVoir, Priority.ALWAYS);
        btnVoir.setStyle("-fx-background-color:" + btnBg + ";-fx-text-fill:" + btnFg + ";" +
                "-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-background-radius:10;-fx-padding:9 12 9 12;-fx-cursor:hand;");
        btnVoir.setOnMouseEntered(e -> btnVoir.setStyle(
                "-fx-background-color:" + fc1 + ";-fx-text-fill:white;" +
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-background-radius:10;-fx-padding:9 12 9 12;-fx-cursor:hand;"));
        btnVoir.setOnMouseExited(e -> btnVoir.setStyle(
                "-fx-background-color:" + btnBg + ";-fx-text-fill:" + btnFg + ";" +
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-background-radius:10;-fx-padding:9 12 9 12;-fx-cursor:hand;"));
        btnVoir.setOnAction(e -> showDetails(s, fc1, fc2));
        footer.getChildren().add(btnVoir);

        card.getChildren().addAll(header, body, sep, footer);
        return card;
    }

    private void showDetails(Session s, String c1, String c2) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(null);
        dialog.setHeaderText(null);

        // Détection du mode sombre
        boolean isDarkMode = rootPane.getScene().getStylesheets().stream()
                .anyMatch(style -> style.contains("dark.css"));

        // Couleurs adaptées au mode
        String bgColor = isDarkMode ? "#1E293B" : "#F8FAFC";
        String textColor = isDarkMode ? "#F1F5F9" : "#1E293B";
        String labelColor = isDarkMode ? "#94A3B8" : "#64748B";
        String descBgColor = isDarkMode ? "#0F172A" : "#F8FAFC";
        String descBorderColor = isDarkMode ? "#334155" : "#E2E8F0";
        String separatorColor = isDarkMode ? "#334155" : "#E2E8F0";

        // Conteneur principal
        VBox root = new VBox(0);
        root.setPrefWidth(420);
        root.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 20, 0, 0, 8);"
        );

        // ─────────────────────────────────────────────────────────────────
        // HEADER (toujours avec dégradé)
        // ─────────────────────────────────────────────────────────────────
        VBox header = new VBox(8);
        header.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #3B82F6, #8B5CF6);" +
                        "-fx-background-radius: 20 20 0 0;" +
                        "-fx-padding: 18 22 14 22;"
        );

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label iconCircle = new Label(getStatutIcon(s.getStatut()));
        iconCircle.setStyle(
                "-fx-font-size: 28px;" +
                        "-fx-background-color: rgba(255,255,255,0.2);" +
                        "-fx-background-radius: 50;" +
                        "-fx-padding: 8;"
        );

        VBox titleBox = new VBox(2);
        Label titleLbl = new Label("Session #" + s.getId());
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label statutLbl = new Label(s.getStatut() != null ? s.getStatut().toUpperCase() : "");
        statutLbl.setStyle(
                "-fx-background-color: rgba(255,255,255,0.2);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 3 12 3 12;"
        );
        titleBox.getChildren().addAll(titleLbl, statutLbl);

        titleRow.getChildren().addAll(iconCircle, titleBox);

        Label dateLbl = new Label("📅  " + (s.getDateHeure() != null ?
                s.getDateHeure().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—"));
        dateLbl.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 12px;");

        header.getChildren().addAll(titleRow, dateLbl);

        // ─────────────────────────────────────────────────────────────────
        // CORPS
        // ─────────────────────────────────────────────────────────────────
        VBox body = new VBox(0);
        body.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-padding: 16 22 22 22;" +
                        "-fx-background-radius: 0 0 20 20;"
        );

        // Grille des détails
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 0 0 12 0;");

        ColumnConstraints colIcon = new ColumnConstraints();
        colIcon.setMinWidth(32);
        colIcon.setPrefWidth(32);
        ColumnConstraints colLabel = new ColumnConstraints();
        colLabel.setMinWidth(70);
        colLabel.setPrefWidth(70);
        ColumnConstraints colValue = new ColumnConstraints();
        colValue.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(colIcon, colLabel, colValue);

        // Données avec icônes COLORÉES
        Object[][] details = {
                {"🆔", "ID", String.valueOf(s.getId()), "#8B5CF6"},
                {"📌", "Statut", s.getStatut() != null ? s.getStatut() : "—", "#3B82F6"},
                {"🔗", "Lien", s.getLienReunion() != null ?
                        (s.getLienReunion().length() > 35 ? s.getLienReunion().substring(0, 32) + "..." : s.getLienReunion()) : "—", "#06B6D4"},
                {"👥", "Groupe", "#" + s.getIdGroupId(), "#10B981"},
                {"👤", "Prof", "#" + s.getIdUserId(), "#F59E0B"},
                {"⏱️", "Durée", s.getDuree() != null ? s.getDuree() + " min" : "—", "#3B82F6"},
                {"💰", "Prix", s.getPrix() != null ? String.format("%.0f TND", s.getPrix()) : "—", "#F59E0B"},
                {"🪑", "Places", s.getCapaciteMax() != null ? s.getCapaciteMax() + " places" : "Illimitée", "#EF4444"}
        };

        int row = 0;
        for (Object[] detail : details) {
            // Icône colorée
            Label iconLabel = new Label((String) detail[0]);
            String color = (String) detail[3];
            iconLabel.setStyle("-fx-font-size: 16px; -fx-min-width: 28; -fx-text-fill: " + color + ";");

            // Label
            Label keyLabel = new Label((String) detail[1]);
            keyLabel.setStyle(
                    "-fx-text-fill: " + labelColor + ";" +
                            "-fx-font-size: 11px;" +
                            "-fx-font-weight: bold;"
            );

            // Valeur
            Label valueLabel = new Label((String) detail[2]);
            valueLabel.setStyle(
                    "-fx-text-fill: " + textColor + ";" +
                            "-fx-font-size: 13px;" +
                            "-fx-font-weight: 500;" +
                            "-fx-wrap-text: true;"
            );
            valueLabel.setWrapText(true);

            grid.add(iconLabel, 0, row);
            grid.add(keyLabel, 1, row);
            grid.add(valueLabel, 2, row);
            row++;
        }

        body.getChildren().add(grid);

        // Séparateur
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: " + separatorColor + ";");
        VBox.setMargin(separator, new Insets(6, 0, 12, 0));
        body.getChildren().add(separator);

        // Section Description (CORRIGÉE pour mode sombre)
        if (s.getDescription() != null && !s.getDescription().isBlank()) {
            HBox descHeader = new HBox(6);
            descHeader.setAlignment(Pos.CENTER_LEFT);
            descHeader.setStyle("-fx-padding: 0 0 6 0;");

            Label descIcon = new Label("📝");
            descIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #3B82F6;");

            Label descTitle = new Label("Description");
            descTitle.setStyle(
                    "-fx-text-fill: " + labelColor + ";" +
                            "-fx-font-size: 11px;" +
                            "-fx-font-weight: bold;"
            );
            descHeader.getChildren().addAll(descIcon, descTitle);

            Label descValue = new Label(s.getDescription());
            descValue.setStyle(
                    "-fx-text-fill: " + textColor + ";" +
                            "-fx-font-size: 12px;" +
                            "-fx-wrap-text: true;" +
                            "-fx-background-color: " + descBgColor + ";" +
                            "-fx-background-radius: 12;" +
                            "-fx-padding: 10;" +
                            "-fx-border-color: " + descBorderColor + ";" +
                            "-fx-border-radius: 12;" +
                            "-fx-border-width: 1;"
            );
            descValue.setWrapText(true);

            body.getChildren().addAll(descHeader, descValue);
        }

        // Bouton fermer
        HBox buttonBox = new HBox();
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setStyle("-fx-padding: 18 0 4 0;");

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #3B82F6, #8B5CF6);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 25;" +
                        "-fx-padding: 8 24 8 24;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(59,130,246,0.3), 8, 0, 0, 2);"
        );
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #2563EB, #7C3AED);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 25;" +
                        "-fx-padding: 8 24 8 24;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(59,130,246,0.5), 12, 0, 0, 4);"
        ));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #3B82F6, #8B5CF6);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 25;" +
                        "-fx-padding: 8 24 8 24;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(59,130,246,0.3), 8, 0, 0, 2);"
        ));
        closeBtn.setOnAction(e -> dialog.close());

        buttonBox.getChildren().add(closeBtn);
        body.getChildren().add(buttonBox);

        root.getChildren().addAll(header, body);

        // Configuration du dialogue
        DialogPane dialogPane = new DialogPane();
        dialogPane.setContent(root);
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        Node closeButton = dialogPane.lookupButton(ButtonType.CLOSE);
        closeButton.setVisible(false);
        dialogPane.setStyle("-fx-background-color: transparent; -fx-padding: 0;");

        dialog.setDialogPane(dialogPane);
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (rootPane != null && rootPane.getScene() != null) {
            dialog.initOwner(rootPane.getScene().getWindow());
        }
        dialog.setResizable(false);

        dialog.showAndWait();
    }
    private Label buildStatutBadge(String statut, boolean dark) {
        String bg, fg, icon;
        switch (statut != null ? statut : "") {
            case "planifiée" -> { bg = dark ? "#1D3461" : "#DBEAFE"; fg = dark ? "#60A5FA" : "#2563EB"; icon = "📅"; }
            case "en cours"  -> { bg = dark ? "#2E1065" : "#EDE9FE"; fg = dark ? "#A78BFA" : "#7C3AED"; icon = "🔄"; }
            case "terminée"  -> { bg = dark ? "#064E3B" : "#D1FAE5"; fg = dark ? "#34D399" : "#059669"; icon = "✅"; }
            case "annulée"   -> { bg = dark ? "#450A0A" : "#FEE2E2"; fg = dark ? "#F87171" : "#DC2626"; icon = "❌"; }
            default           -> { bg = dark ? "#1E293B" : "#F1F5F9"; fg = dark ? "#94A3B8" : "#475569"; icon = "📌"; }
        }
        Label l = new Label(icon + "  " + (statut != null ? statut : "—"));
        l.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;");
        return l;
    }

    private Label chip(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                "-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:6 14 6 14;");
        return l;
    }

    private String getStatutIcon(String s) {
        return switch (s != null ? s : "") {
            case "planifiée" -> "📅";
            case "en cours"  -> "🔄";
            case "terminée"  -> "✅";
            case "annulée"   -> "❌";
            default          -> "📌";
        };
    }
}