package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Reservation;
import com.example.pijava_fluently.services.Reservationservice;
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

public class ReservationAdminController {

    @FXML private VBox rootPane;
    @FXML private FlowPane resasContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatut;
    @FXML private Label countLabel;
    @FXML private Label countEnAttente;
    @FXML private Label countAcceptee;
    @FXML private Label countRefusee;
    @FXML private Label countAnnulee;

    private final Reservationservice service = new Reservationservice();
    private ObservableList<Reservation> allResas = FXCollections.observableArrayList();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        filterStatut.setItems(FXCollections.observableArrayList(
                "Tous", "en attente", "acceptée", "refusée", "annulée"));
        filterStatut.setValue("Tous");
        filterStatut.setOnAction(e -> applyFilter());
        loadAll();
    }

    private void loadAll() {
        try {
            allResas = FXCollections.observableArrayList(service.recuperer());
            updateStats();
            applyFilter();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private boolean isDarkMode() {
        if (rootPane == null || rootPane.getScene() == null) return false;
        return rootPane.getScene().getStylesheets().stream()
                .anyMatch(s -> s.contains("dark.css"));
    }

    private void updateStats() {
        long enAttente = allResas.stream().filter(r -> "en attente".equals(r.getStatut())).count();
        long acceptee  = allResas.stream().filter(r -> "acceptée".equals(r.getStatut())).count();
        long refusee   = allResas.stream().filter(r -> "refusée".equals(r.getStatut())).count();
        long annulee   = allResas.stream().filter(r -> "annulée".equals(r.getStatut())).count();

        setStatCard(countEnAttente, enAttente, "245,158,11",  "#FEF3C7", "#D97706", "#F59E0B");
        setStatCard(countAcceptee,  acceptee,  "16,185,129",  "#D1FAE5", "#059669", "#10B981");
        setStatCard(countRefusee,   refusee,   "239,68,68",   "#FEE2E2", "#DC2626", "#EF4444");
        setStatCard(countAnnulee,   annulee,   "100,116,139", "#E2E8F0", "#475569", "#64748B");

        if (countLabel != null) countLabel.setText(allResas.size() + " réservation(s)");
    }

    /**
     * Met à jour une stat card selon le mode (clair/sombre).
     */
    private void setStatCard(Label lbl, long count, String rgbValues,
                             String borderColor, String textColor, String labelColor) {
        if (lbl == null) return;
        lbl.setText(String.valueOf(count));
        lbl.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:" + textColor + ";");

        if (lbl.getParent() != null) {
            boolean dark = isDarkMode();
            String bg     = dark ? "#111827" : "white";
            double opacity = dark ? 0.3 : 0.15;
            lbl.getParent().setStyle(
                    "-fx-background-color:" + bg + ";" +
                            "-fx-background-radius:18;" +
                            "-fx-border-color:" + borderColor + ";" +
                            "-fx-border-radius:18;" +
                            "-fx-border-width:2;" +
                            "-fx-padding:22 14 22 14;" +
                            "-fx-effect:dropshadow(gaussian,rgba(" + rgbValues + "," + opacity + "),16,0,0,4);"
            );
        }
    }

    @FXML private void handleSearch() { applyFilter(); }

    private void applyFilter() {
        String q  = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        String st = filterStatut != null ? filterStatut.getValue() : "Tous";
        List<Reservation> filtered = allResas.stream()
                .filter(r -> {
                    boolean mq = q.isEmpty()
                            || r.getStatut().toLowerCase().contains(q)
                            || String.valueOf(r.getIdSessionId()).contains(q)
                            || String.valueOf(r.getIdUserId()).contains(q);
                    boolean ms = "Tous".equals(st) || st.equals(r.getStatut());
                    return mq && ms;
                }).collect(Collectors.toList());
        render(filtered);
    }

    private void render(List<Reservation> list) {
        resasContainer.getChildren().clear();
        int i = 0;
        for (Reservation r : list)
            resasContainer.getChildren().add(buildCard(r, i++));
        if (list.isEmpty()) {
            VBox empty = new VBox(14);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            Label icon = new Label("📭"); icon.setStyle("-fx-font-size:52px;");
            Label msg  = new Label("Aucune réservation trouvée");
            msg.setStyle("-fx-font-size:15px;-fx-text-fill:#9CA3AF;");
            empty.getChildren().addAll(icon, msg);
            resasContainer.getChildren().add(empty);
        }
    }

    private VBox buildCard(Reservation r, int ci) {
        // Couleurs selon statut
        String c1, c2, rgb;
        switch (r.getStatut() != null ? r.getStatut() : "") {
            case "en attente" -> { c1 = "#F59E0B"; c2 = "#D97706"; rgb = "245,158,11"; }
            case "acceptée"   -> { c1 = "#10B981"; c2 = "#059669"; rgb = "16,185,129"; }
            case "refusée"    -> { c1 = "#EF4444"; c2 = "#DC2626"; rgb = "239,68,68"; }
            case "annulée"    -> { c1 = "#64748B"; c2 = "#475569"; rgb = "100,116,139"; }
            default           -> { c1 = "#6366F1"; c2 = "#8B5CF6"; rgb = "99,102,241"; }
        }
        final String fc1 = c1, fc2 = c2;

        boolean dark = isDarkMode();
        String cardBg      = dark ? "#1E293B"       : "white";
        String cardBgHover = dark ? "#243044"       : "#F8FAFC";
        String bodyBg      = dark ? "transparent"   : "transparent";
        String descColor   = dark ? "#94A3B8"       : "#64748B";
        String userColor   = dark ? "#9AA6B9"       : "#6B7280";
        String footerBg    = dark ? "transparent"   : "transparent";
        String lockColor   = dark ? "#64748B"       : "#94A3B8";
        String sepColor    = dark ? "#334155"       : "#E2E8F0";

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
                        "-fx-effect:dropshadow(gaussian,rgba(" + rgb + ",0.35),22,0,0,8);" +
                        "-fx-scale-x:1.02;-fx-scale-y:1.02;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:" + cardBg + ";" +
                        "-fx-background-radius:20;" +
                        "-fx-border-color:" + fc1 + ";" +
                        "-fx-border-radius:20;" +
                        "-fx-border-width:1.5;" +
                        "-fx-effect:dropshadow(gaussian,rgba(" + rgb + ",0.18),18,0,0,5);"
        ));

        // ── Header gradient (identique en clair et sombre — toujours coloré)
        VBox header = new VBox(6);
        header.setPadding(new Insets(18, 20, 14, 20));
        header.setStyle(
                "-fx-background-color:linear-gradient(to bottom right," + c1 + "," + c2 + ");" +
                        "-fx-background-radius:18 18 0 0;"
        );

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(getIcon(r.getStatut()));
        iconLbl.setStyle(
                "-fx-font-size:18px;" +
                        "-fx-background-color:rgba(255,255,255,0.22);" +
                        "-fx-background-radius:50;" +
                        "-fx-padding:6 8 6 8;"
        );
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label idBadge = new Label("#" + r.getId());
        idBadge.setStyle(
                "-fx-background-color:rgba(255,255,255,0.22);" +
                        "-fx-text-fill:white;" +
                        "-fx-font-size:10px;-fx-font-weight:bold;" +
                        "-fx-background-radius:20;-fx-padding:3 9 3 9;"
        );
        topRow.getChildren().addAll(iconLbl, sp, idBadge);

        Label statutLbl = new Label(r.getStatut() != null ? r.getStatut().toUpperCase() : "—");
        statutLbl.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label dateLbl = new Label("📅 " + (r.getDateReservation() != null ? r.getDateReservation().format(FMT) : "—"));
        dateLbl.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.88);");
        header.getChildren().addAll(topRow, statutLbl, dateLbl);

        // ── Body
        VBox body = new VBox(10);
        body.setPadding(new Insets(14, 18, 12, 18));
        body.setStyle("-fx-background-color:" + bodyBg + ";");

        // Session chip
        HBox sessionRow = new HBox(8);
        sessionRow.setAlignment(Pos.CENTER_LEFT);
        Label sessIcon = new Label("🎓");
        sessIcon.setStyle("-fx-font-size:14px;");
        Label sessLbl = new Label("Session #" + r.getIdSessionId());
        String sessBg = dark ? "#1E293B" : "#EEF2FF";
        String sessColor = dark ? "#818CF8" : "#4338CA";
        sessLbl.setStyle(
                "-fx-background-color:" + sessBg + ";" +
                        "-fx-text-fill:" + sessColor + ";" +
                        "-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-background-radius:20;-fx-padding:4 12 4 12;"
        );
        sessionRow.getChildren().addAll(sessIcon, sessLbl);

        // User chip
        HBox userRow = new HBox(8);
        userRow.setAlignment(Pos.CENTER_LEFT);
        Label userIcon = new Label("👤");
        userIcon.setStyle("-fx-font-size:14px;");
        Label userLbl = new Label("Utilisateur #" + r.getIdUserId());
        userLbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + userColor + ";-fx-font-weight:500;");
        userRow.getChildren().addAll(userIcon, userLbl);

        body.getChildren().addAll(sessionRow, userRow);

        if (r.getCommentaire() != null && !r.getCommentaire().isBlank()) {
            String cBg = dark ? "#0F1420" : "#F8FAFC";
            String cFg = dark ? "#9AA6B9" : "#6B7280";
            Label cl = new Label("💬 " + r.getCommentaire());
            cl.setStyle(
                    "-fx-font-size:11px;-fx-text-fill:" + cFg + ";-fx-wrap-text:true;" +
                            "-fx-background-color:" + cBg + ";-fx-background-radius:8;-fx-padding:6 10 6 10;"
            );
            cl.setWrapText(true);
            body.getChildren().add(cl);
        }

        if (r.getDateConfirmation() != null) {
            String confBg = dark ? "#0F1420" : "#F0FDF4";
            Label confLbl = new Label("✔ Confirmé le " + r.getDateConfirmation().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            confLbl.setStyle(
                    "-fx-font-size:11px;-fx-text-fill:#10B981;-fx-font-weight:bold;" +
                            "-fx-background-color:" + confBg + ";-fx-background-radius:8;-fx-padding:5 10 5 10;"
            );
            body.getChildren().add(confLbl);
        }

        // ── Footer
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:" + sepColor + ";-fx-border-color:" + sepColor + ";");
        VBox.setMargin(sep, new Insets(2, 0, 0, 0));

        HBox footer = new HBox();
        footer.setPadding(new Insets(10, 16, 14, 16));
        footer.setAlignment(Pos.CENTER);
        Label lockLbl = new Label("🔒  Lecture seule — admin");
        lockLbl.setStyle("-fx-font-size:11px;-fx-text-fill:" + lockColor + ";-fx-font-weight:bold;");
        footer.getChildren().add(lockLbl);

        card.getChildren().addAll(header, body, sep, footer);
        return card;
    }

    private String getIcon(String statut) {
        return switch (statut != null ? statut : "") {
            case "en attente" -> "⏳";
            case "acceptée"   -> "✅";
            case "refusée"    -> "❌";
            case "annulée"    -> "🚫";
            default           -> "📋";
        };
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}