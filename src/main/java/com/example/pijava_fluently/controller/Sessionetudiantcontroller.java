package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Reservation;
import com.example.pijava_fluently.entites.Session;
import com.example.pijava_fluently.services.Reservationservice;
import com.example.pijava_fluently.services.Sessionservice;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class Sessionetudiantcontroller {

    @FXML private VBox      rootPane;
    @FXML private Label     countLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatut;
    @FXML private FlowPane  sessionsContainer;
    @FXML private FlowPane  mesResasContainer;
    @FXML private Label     mesResasCount;
    @FXML private TabPane   tabPane;

    private final Sessionservice     sessionservice     = new Sessionservice();
    private final Reservationservice reservationservice = new Reservationservice();

    private ObservableList<Session>     allSessions = FXCollections.observableArrayList();
    private ObservableList<Reservation> mesResas    = FXCollections.observableArrayList();

    // ID étudiant connecté — à injecter via setUserId()
    private int currentUserId = 3;

    // ── BUG FIX: track whether initialize() already ran so we can safely call loadAll()
    private boolean initialized = false;

    private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[][] CARD_COLORS = {
            {"#3B82F6","#2563EB"}, {"#6C63FF","#8B5CF6"}, {"#10B981","#059669"},
            {"#F59E0B","#D97706"}, {"#06B6D4","#0891B2"}, {"#EC4899","#DB2777"},
    };

    // ── BUG FIX: setUserId now safely triggers load after initialize()
    public void setUserId(int id) {
        this.currentUserId = id;
        if (initialized) {
            loadAll();
        }
    }

    @FXML
    public void initialize() {
        // ── BUG FIX 1: default to "Toutes" not "planifiée"
        filterStatut.setItems(FXCollections.observableArrayList(
                "Toutes", "planifiée", "en cours"));
        filterStatut.setValue("Toutes");   // was missing / wrong value before
        filterStatut.setOnAction(e -> applyFilter());

        initialized = true;

        // ── BUG FIX 2: always load on initialize so data shows even without setUserId()
        loadAll();
    }

    private void loadAll() {
        loadSessions();
        loadMesReservations();
    }

    // ── BUG FIX 3: removed redundant future-date re-filter (recupererDisponibles() already does it)
    private void loadSessions() {
        try {
            List<Session> sessions = sessionservice.recupererDisponibles();
            allSessions = FXCollections.observableArrayList(sessions);
            applyFilter();
        } catch (SQLException e) {
            showStyledAlert("error", "Erreur de chargement", e.getMessage());
        }
    }

    private void loadMesReservations() {
        try {
            mesResas = FXCollections.observableArrayList(
                    reservationservice.recupererParEtudiant(currentUserId));
            renderMesResas(mesResas);
            if (mesResasCount != null) mesResasCount.setText(mesResas.size() + " réservation(s)");
        } catch (SQLException e) {
            showStyledAlert("error", "Erreur", e.getMessage());
        }
    }

    @FXML private void handleSearch() { applyFilter(); }

    private void applyFilter() {
        String q      = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        String statut = filterStatut != null ? filterStatut.getValue() : "Toutes";
        List<Session> filtered = allSessions.stream()
                .filter(s -> {
                    boolean mq = q.isEmpty()
                            || (s.getDescription()!=null && s.getDescription().toLowerCase().contains(q))
                            || (s.getStatut()!=null && s.getStatut().toLowerCase().contains(q));
                    boolean ms = "Toutes".equals(statut) || statut == null || statut.equals(s.getStatut());
                    return mq && ms;
                }).collect(Collectors.toList());
        renderSessions(filtered);
        if (countLabel != null) countLabel.setText(filtered.size() + " session(s) disponible(s)");
    }

    // ── Rendu sessions disponibles ─────────────────────────────────
    private void renderSessions(List<Session> list) {
        sessionsContainer.getChildren().clear();
        int i = 0;
        for (Session s : list)
            sessionsContainer.getChildren().add(buildSessionCard(s, i++ % CARD_COLORS.length));
        if (list.isEmpty()) {
            VBox empty = new VBox(12); empty.setAlignment(Pos.CENTER); empty.setPadding(new Insets(60));
            Label icon = new Label("📭"); icon.setStyle("-fx-font-size:48px;");
            Label msg  = new Label("Aucune session disponible");
            msg.setStyle("-fx-font-size:15px;-fx-text-fill:#9CA3AF;");
            empty.getChildren().addAll(icon, msg);
            sessionsContainer.getChildren().add(empty);
        }
    }

    private VBox buildSessionCard(Session s, int ci) {
        String c1 = CARD_COLORS[ci][0], c2 = CARD_COLORS[ci][1];
        boolean dejaResa = false;
        int nbPlaces = 0;
        try {
            dejaResa = reservationservice.dejaReserve(currentUserId, s.getId());
            int nbAcc = sessionservice.compterReservationsAcceptees(s.getId());
            if (s.getCapaciteMax() != null) nbPlaces = s.getCapaciteMax() - nbAcc;
        } catch (SQLException e) { /* ignore */ }
        final boolean alreadyBooked = dejaResa;
        final int placesRestantes = nbPlaces;

        VBox card = new VBox(0);
        card.setPrefWidth(290); card.setMaxWidth(290);
        card.setStyle("-fx-background-color:#FFFFFF;-fx-background-radius:18;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),20,0,0,5);-fx-cursor:hand;");
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),26,0,0,8);" +
                        "-fx-scale-x:1.02;-fx-scale-y:1.02;-fx-cursor:hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),20,0,0,5);-fx-cursor:hand;"));

        // Header gradient
        VBox header = new VBox(8); header.setPadding(new Insets(20,20,16,20));
        header.setStyle("-fx-background-color:linear-gradient(to bottom right," + c1 + "," + c2 + ");-fx-background-radius:18 18 0 0;");
        HBox htop = new HBox(8); htop.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(getStatutIcon(s.getStatut()));
        iconLbl.setStyle("-fx-font-size:18px;-fx-background-color:rgba(255,255,255,0.22);-fx-background-radius:50;-fx-padding:6 8 6 8;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        if (alreadyBooked) {
            Label bookedBadge = new Label("✅ Déjà réservé");
            bookedBadge.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 9 3 9;");
            htop.getChildren().addAll(iconLbl, sp, bookedBadge);
        } else if (s.getCapaciteMax() != null) {
            Label placesLbl = new Label(placesRestantes + " place(s)");
            placesLbl.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 9 3 9;");
            htop.getChildren().addAll(iconLbl, sp, placesLbl);
        } else {
            htop.getChildren().addAll(iconLbl, sp);
        }
        Label dateLabel = new Label(s.getDateHeure()!=null ? s.getDateHeure().format(FMT_DT) : "Date à définir");
        dateLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        header.getChildren().addAll(htop, dateLabel);

        // Corps
        VBox body = new VBox(10); body.setPadding(new Insets(14,18,10,18));
        String desc = s.getDescription()!=null && !s.getDescription().isBlank()
                ? (s.getDescription().length()>75 ? s.getDescription().substring(0,72)+"…" : s.getDescription())
                : "Aucune description.";
        Label descL = new Label(desc);
        descL.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;-fx-wrap-text:true;"); descL.setWrapText(true);

        HBox infoRow = new HBox(8);
        if (s.getDuree() != null) {
            Label dl = new Label("⏱ " + s.getDuree() + " min");
            dl.setStyle("-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
            infoRow.getChildren().add(dl);
        }
        if (s.getPrix() != null) {
            Label pl = new Label("💰 " + String.format("%.0f",s.getPrix()) + " TND");
            pl.setStyle("-fx-background-color:#F0FDF4;-fx-text-fill:#16A34A;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
            infoRow.getChildren().add(pl);
        }
        body.getChildren().addAll(descL, infoRow);

        if (s.getLienReunion() != null && !s.getLienReunion().isBlank()) {
            Label lienL = new Label("🔗 " + (s.getLienReunion().length()>30
                    ? s.getLienReunion().substring(0,27)+"…" : s.getLienReunion()));
            lienL.setStyle("-fx-font-size:11px;-fx-text-fill:#6C63FF;");
            body.getChildren().add(lienL);
        }

        // Alerte places limitées
        if (s.getCapaciteMax() != null && placesRestantes <= 2 && placesRestantes >= 0 && !alreadyBooked) {
            Label alertL = new Label("⚠ " + (placesRestantes == 0 ? "Session complète !" : "Dernières places !"));
            alertL.setStyle("-fx-background-color:#FFF7ED;-fx-text-fill:#EA580C;-fx-font-size:11px;" +
                    "-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:4 10 4 10;");
            body.getChildren().add(alertL);
        }

        Separator sep = new Separator(); VBox.setMargin(sep, new Insets(4,0,0,0));

        // Boutons
        HBox actions = new HBox(8); actions.setPadding(new Insets(10,14,14,14)); actions.setAlignment(Pos.CENTER);
        Button btnDetails = makeBtn("👁 Détails","#EFF6FF","#3B82F6");
        HBox.setHgrow(btnDetails, Priority.ALWAYS); btnDetails.setMaxWidth(Double.MAX_VALUE);
        btnDetails.setOnAction(e -> showSessionDetails(s));

        if (alreadyBooked) {
            Label done = new Label("✅ Réservation effectuée");
            done.setStyle("-fx-font-size:12px;-fx-text-fill:#059669;-fx-font-weight:bold;");
            actions.getChildren().addAll(btnDetails, done);
        } else if (s.getCapaciteMax() != null && placesRestantes <= 0) {
            Label full = new Label("🚫 Session complète");
            full.setStyle("-fx-font-size:12px;-fx-text-fill:#E11D48;-fx-font-weight:bold;");
            actions.getChildren().addAll(btnDetails, full);
        } else {
            Button btnResa = new Button("📋  Réserver");
            btnResa.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnResa, Priority.ALWAYS);
            btnResa.setStyle("-fx-background-color:linear-gradient(to right,"+c1+","+c2+");-fx-text-fill:white;" +
                    "-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:10;-fx-padding:9 0 9 0;-fx-cursor:hand;");
            btnResa.setOnAction(e -> handleReserver(s));
            actions.getChildren().addAll(btnDetails, btnResa);
        }

        card.getChildren().addAll(header, body, sep, actions);
        return card;
    }

    private void handleReserver(Session s) {
        Dialog<ButtonType> confirm = new Dialog<>();
        confirm.setTitle("Confirmation de réservation");
        confirm.setHeaderText(null);
        VBox content = new VBox(0); content.setPrefWidth(420);
        int ci = (int)(s.getId() % CARD_COLORS.length);
        String c1 = CARD_COLORS[ci][0], c2 = CARD_COLORS[ci][1];
        VBox hdr = new VBox(8); hdr.setPadding(new Insets(22,26,18,26));
        hdr.setStyle("-fx-background-color:linear-gradient(to right,"+c1+","+c2+");");
        Label tl = new Label("📋  Confirmer la réservation");
        tl.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label sl = new Label("Session du " + (s.getDateHeure()!=null?s.getDateHeure().format(FMT_DT):"?"));
        sl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.85);");
        hdr.getChildren().addAll(tl, sl);
        VBox bdy = new VBox(14); bdy.setPadding(new Insets(18,26,22,26));
        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(8);
        grid.setStyle("-fx-background-color:#F8FAFC;-fx-background-radius:10;-fx-padding:14;");
        ColumnConstraints cc1b = new ColumnConstraints(100);
        ColumnConstraints cc2b = new ColumnConstraints(); cc2b.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc1b, cc2b);
        String ls="-fx-font-size:11px;-fx-text-fill:#9CA3AF;-fx-font-weight:bold;";
        String vs="-fx-font-size:13px;-fx-text-fill:#1F2937;-fx-font-weight:bold;";
        addGridRow(grid,0,"📅 Date",s.getDateHeure()!=null?s.getDateHeure().format(FMT_DT):"—",ls,vs);
        if(s.getDuree()!=null) addGridRow(grid,1,"⏱ Durée",s.getDuree()+" min",ls,vs);
        if(s.getPrix()!=null) addGridRow(grid,2,"💰 Prix",String.format("%.0f TND",s.getPrix()),ls,vs);
        Label infoLbl = new Label("ℹ️  Votre demande sera en attente de validation par le professeur.");
        infoLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#3730A3;-fx-background-color:#EEF2FF;" +
                "-fx-background-radius:8;-fx-padding:10 14 10 14;-fx-wrap-text:true;");
        infoLbl.setWrapText(true);
        bdy.getChildren().addAll(grid, infoLbl);
        content.getChildren().addAll(hdr, bdy);
        confirm.getDialogPane().setContent(content);
        confirm.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");
        ButtonType btnOui = new ButtonType("Oui, je réserve !", ButtonBar.ButtonData.OK_DONE);
        confirm.getDialogPane().getButtonTypes().addAll(btnOui, ButtonType.CANCEL);
        Button ouiBtn = (Button) confirm.getDialogPane().lookupButton(btnOui);
        ouiBtn.setStyle("-fx-background-color:linear-gradient(to right,"+c1+","+c2+");" +
                "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-padding:8 20 8 20;");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == btnOui) {
                try {
                    Reservation r = new Reservation(LocalDate.now(), "en attente", s.getId(), currentUserId);
                    reservationservice.ajouter(r);
                    loadAll();
                    showStyledAlert("success", "Réservation envoyée ! 🎉",
                            "Votre demande pour la session du " +
                                    (s.getDateHeure()!=null?s.getDateHeure().format(FMT_DT):"?") +
                                    " est en attente de validation.");
                } catch (SQLException e) { showStyledAlert("error", "Erreur BD", e.getMessage()); }
            }
        });
    }

    // ── Mes réservations ───────────────────────────────────────────
    private void renderMesResas(List<Reservation> list) {
        if (mesResasContainer == null) return;
        mesResasContainer.getChildren().clear();
        int i = 0;
        for (Reservation r : list)
            mesResasContainer.getChildren().add(buildResaCard(r, i++ % CARD_COLORS.length));
        if (list.isEmpty()) {
            VBox empty = new VBox(12); empty.setAlignment(Pos.CENTER); empty.setPadding(new Insets(40));
            Label icon = new Label("📭"); icon.setStyle("-fx-font-size:40px;");
            Label msg  = new Label("Vous n'avez aucune réservation");
            msg.setStyle("-fx-font-size:14px;-fx-text-fill:#9CA3AF;");
            empty.getChildren().addAll(icon, msg);
            mesResasContainer.getChildren().add(empty);
        }
    }

    private VBox buildResaCard(Reservation r, int ci) {
        String c1 = CARD_COLORS[ci][0], c2 = CARD_COLORS[ci][1];
        DateTimeFormatter fmtD = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        VBox card = new VBox(0);
        card.setPrefWidth(270); card.setMaxWidth(270);
        card.setStyle("-fx-background-color:#FFFFFF;-fx-background-radius:16;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),16,0,0,4);");
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:16;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),22,0,0,7);" +
                        "-fx-scale-x:1.02;-fx-scale-y:1.02;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:16;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),16,0,0,4);"));

        VBox header = new VBox(6); header.setPadding(new Insets(16,18,12,18));
        header.setStyle("-fx-background-color:linear-gradient(to right,"+c1+","+c2+");-fx-background-radius:16 16 0 0;");
        Label statusBadge = new Label(getResaStatutIcon(r.getStatut()) + "  " + r.getStatut().toUpperCase());
        statusBadge.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;");
        Label idLbl = new Label("Réservation #" + r.getId());
        idLbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;");
        header.getChildren().addAll(statusBadge, idLbl);

        VBox body = new VBox(8); body.setPadding(new Insets(12,16,10,16));
        Label dateLbl = new Label("📅 Réservé le " + r.getDateReservation().format(fmtD));
        dateLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#64748B;");
        Label sessLbl = new Label("🎓 Session #" + r.getIdSessionId());
        sessLbl.setStyle("-fx-background-color:#EEF2FF;-fx-text-fill:#6C63FF;-fx-font-size:11px;" +
                "-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
        body.getChildren().addAll(dateLbl, sessLbl);

        if (r.getCommentaire() != null && !r.getCommentaire().isBlank()) {
            Label commentL = new Label("💬 " + r.getCommentaire());
            commentL.setStyle("-fx-font-size:11px;-fx-text-fill:#6B7280;-fx-wrap-text:true;"); commentL.setWrapText(true);
            body.getChildren().add(commentL);
        }
        if (r.getDateConfirmation() != null) {
            Label confL = new Label("✔ Confirmé le " + r.getDateConfirmation().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            confL.setStyle("-fx-font-size:11px;-fx-text-fill:#059669;-fx-font-weight:bold;");
            body.getChildren().add(confL);
        }

        Separator sep = new Separator(); VBox.setMargin(sep, new Insets(4,0,0,0));

        HBox actions = new HBox(8); actions.setPadding(new Insets(10,14,12,14)); actions.setAlignment(Pos.CENTER);
        if ("en attente".equals(r.getStatut())) {
            Button btnAnn = new Button("🚫  Annuler ma réservation");
            btnAnn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnAnn, Priority.ALWAYS);
            btnAnn.setStyle("-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;-fx-font-size:12px;" +
                    "-fx-font-weight:bold;-fx-background-radius:10;-fx-padding:9 0 9 0;-fx-cursor:hand;");
            btnAnn.setOnAction(e -> handleAnnulerResa(r));
            actions.getChildren().add(btnAnn);
        } else {
            Label info = new Label(getResaStatutIcon(r.getStatut()) + " " + r.getStatut());
            info.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#64748B;");
            actions.getChildren().add(info);
        }

        card.getChildren().addAll(header, body, sep, actions);
        return card;
    }

    private void handleAnnulerResa(Reservation r) {
        Dialog<ButtonType> confirm = new Dialog<>();
        confirm.setTitle("Annuler la réservation");
        confirm.setHeaderText(null);
        VBox content = new VBox(0); content.setPrefWidth(400);
        VBox hdr = new VBox(6); hdr.setPadding(new Insets(20,24,16,24));
        hdr.setStyle("-fx-background-color:linear-gradient(to right,#F59E0B,#D97706);");
        Label tl = new Label("🚫  Annuler la réservation #" + r.getId());
        tl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:white;");
        hdr.getChildren().add(tl);
        VBox bdy = new VBox(10); bdy.setPadding(new Insets(18,24,22,24));
        Label warn = new Label("Êtes-vous sûr de vouloir annuler cette réservation ?\nCette action ne peut pas être annulée.");
        warn.setStyle("-fx-font-size:13px;-fx-text-fill:#374151;-fx-wrap-text:true;"); warn.setWrapText(true);
        bdy.getChildren().add(warn);
        content.getChildren().addAll(hdr, bdy);
        confirm.getDialogPane().setContent(content);
        confirm.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");
        ButtonType btnOui = new ButtonType("Confirmer l'annulation", ButtonBar.ButtonData.OK_DONE);
        confirm.getDialogPane().getButtonTypes().addAll(btnOui, ButtonType.CANCEL);
        Button ouiBtn = (Button) confirm.getDialogPane().lookupButton(btnOui);
        ouiBtn.setStyle("-fx-background-color:#F59E0B;-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 18 8 18;");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == btnOui) {
                try {
                    reservationservice.annuler(r.getId());
                    loadAll();
                    showStyledAlert("warning", "Réservation annulée", "Votre réservation #" + r.getId() + " a été annulée.");
                } catch (SQLException e) { showStyledAlert("error", "Erreur BD", e.getMessage()); }
            }
        });
    }

    // ── Détails session ────────────────────────────────────────────
    private void showSessionDetails(Session s) {
        int ci = (int)(s.getId() % CARD_COLORS.length);
        String c1 = CARD_COLORS[ci][0], c2 = CARD_COLORS[ci][1];
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails — Session #" + s.getId());
        dialog.setHeaderText(null);
        VBox content = new VBox(0); content.setPrefWidth(480);

        VBox header = new VBox(6); header.setPadding(new Insets(22,26,18,26));
        header.setStyle("-fx-background-color:linear-gradient(to right,"+c1+","+c2+");");
        Label tl = new Label("📅  Session du " + (s.getDateHeure()!=null?s.getDateHeure().format(FMT_DT):"—"));
        tl.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        Label sl = new Label(s.getStatut()!=null ? s.getStatut().toUpperCase() : "");
        sl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.85);-fx-font-weight:bold;");
        header.getChildren().addAll(tl, sl);

        VBox body = new VBox(14); body.setPadding(new Insets(18,26,22,26));
        GridPane grid = new GridPane(); grid.setHgap(16); grid.setVgap(12);
        grid.setStyle("-fx-background-color:#F8F9FD;-fx-background-radius:12;-fx-padding:16;");
        ColumnConstraints cc1 = new ColumnConstraints(120);
        ColumnConstraints cc2 = new ColumnConstraints(); cc2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc1, cc2);
        String ls="-fx-font-size:11px;-fx-text-fill:#9CA3AF;-fx-font-weight:bold;";
        String vs="-fx-font-size:13px;-fx-text-fill:#1F2937;-fx-font-weight:bold;";
        addGridRow(grid,0,"Statut",     s.getStatut()!=null?s.getStatut():"—",ls,vs);
        addGridRow(grid,1,"Date",       s.getDateHeure()!=null?s.getDateHeure().format(FMT_DT):"—",ls,vs);
        addGridRow(grid,2,"Durée",      s.getDuree()!=null?s.getDuree()+" min":"—",ls,vs);
        addGridRow(grid,3,"Prix",       s.getPrix()!=null?String.format("%.2f TND",s.getPrix()):"Gratuit",ls,vs);
        addGridRow(grid,4,"Capacité",   s.getCapaciteMax()!=null?String.valueOf(s.getCapaciteMax()):"Illimitée",ls,vs);
        addGridRow(grid,5,"Lien réunion",s.getLienReunion()!=null?s.getLienReunion():"—",ls,vs);
        body.getChildren().add(grid);
        if (s.getDescription()!=null && !s.getDescription().isBlank()) {
            TextArea da = new TextArea(s.getDescription());
            da.setEditable(false); da.setWrapText(true); da.setPrefHeight(65);
            da.setStyle("-fx-background-color:#F9FAFB;-fx-border-color:#E5E7EB;-fx-border-radius:10;-fx-font-size:13px;");
            body.getChildren().add(da);
        }
        content.getChildren().addAll(header, body);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button close = (Button)dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        close.setText("Fermer");
        close.setStyle("-fx-background-color:"+c1+";-fx-text-fill:white;-fx-font-size:13px;" +
                "-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 24 8 24;-fx-cursor:hand;");
        dialog.showAndWait();
    }

    // ── Alertes stylées ────────────────────────────────────────────
    private void showStyledAlert(String type, String title, String message) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        VBox content = new VBox(0); content.setPrefWidth(420);
        String c1, c2, icon;
        switch (type) {
            case "success" -> { c1="#10B981"; c2="#059669"; icon="✅"; }
            case "error"   -> { c1="#EF4444"; c2="#DC2626"; icon="❌"; }
            case "warning" -> { c1="#F59E0B"; c2="#D97706"; icon="⚠️"; }
            default        -> { c1="#3B82F6"; c2="#2563EB"; icon="ℹ️"; }
        }
        VBox header = new VBox(8); header.setPadding(new Insets(22,26,18,26));
        header.setStyle("-fx-background-color:linear-gradient(to right,"+c1+","+c2+");");
        HBox titleRow = new HBox(12); titleRow.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:26px;-fx-background-color:rgba(255,255,255,0.22);-fx-background-radius:50;-fx-padding:6 8 6 8;");
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        titleLbl.setWrapText(true);
        titleRow.getChildren().addAll(iconLbl, titleLbl);
        header.getChildren().add(titleRow);
        VBox body = new VBox(10); body.setPadding(new Insets(16,26,22,26));
        Label msgLbl = new Label(message);
        msgLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#374151;-fx-wrap-text:true;");
        msgLbl.setWrapText(true);
        body.getChildren().add(msgLbl);
        content.getChildren().addAll(header, body);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("OK");
        okBtn.setStyle("-fx-background-color:"+c1+";-fx-text-fill:white;-fx-font-size:13px;" +
                "-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 28 8 28;-fx-cursor:hand;");
        dialog.showAndWait();
    }

    // ── Utilitaires ────────────────────────────────────────────────
    private String getStatutIcon(String s) {
        return switch (s!=null?s:"") {
            case "planifiée" -> "📅"; case "en cours" -> "🔄";
            case "terminée"  -> "✅"; case "annulée"  -> "❌"; default -> "📌";
        };
    }

    private String getResaStatutIcon(String s) {
        return switch (s!=null?s:"") {
            case "en attente" -> "⏳"; case "acceptée" -> "✅";
            case "refusée"    -> "❌"; case "annulée"  -> "🚫"; default -> "📋";
        };
    }

    private Button makeBtn(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";-fx-font-size:11px;" +
                "-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:7 10 7 10;-fx-cursor:hand;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e  -> btn.setOpacity(1.0));
        return btn;
    }

    private void addGridRow(GridPane g, int row, String l, String v, String ls, String vs) {
        Label ll=new Label(l); ll.setStyle(ls); Label vv=new Label(v); vv.setStyle(vs);
        g.add(ll,0,row); g.add(vv,1,row);
    }
}