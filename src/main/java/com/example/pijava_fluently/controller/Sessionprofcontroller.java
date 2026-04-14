package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Reservation;
import com.example.pijava_fluently.entites.Session;
import com.example.pijava_fluently.services.Reservationservice;
import com.example.pijava_fluently.services.Sessionservice;
import com.example.pijava_fluently.utils.MyDatabase;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Sessionprofcontroller {

    public Sessionprofcontroller() {}

    // ── FXML ───────────────────────────────────────────────────────
    @FXML private VBox      rootPane;
    @FXML private Label     countLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatut;
    @FXML private FlowPane  cardsContainer;

    // Formulaire
    @FXML private VBox      formCard;
    @FXML private Label     formTitle;
    @FXML private Label     formTitleIcon;
    @FXML private DatePicker fieldDate;
    @FXML private TextField fieldHeure;
    @FXML private TextField fieldLien;
    @FXML private TextField fieldDuree;
    @FXML private TextField fieldPrix;
    @FXML private TextField fieldCapacite;
    @FXML private ComboBox<String> comboStatut;
    @FXML private ComboBox<String> comboGroupe;
    @FXML private TextArea  fieldDescription;

    // Labels d'erreur inline
    @FXML private Label errDate;
    @FXML private Label errHeure;
    @FXML private Label errLien;
    @FXML private Label errDuree;
    @FXML private Label errPrix;
    @FXML private Label errCapacite;
    @FXML private Label errStatut;
    @FXML private Label errGroupe;
    @FXML private Label errDescription;

    // ── Services ───────────────────────────────────────────────────
    private final Sessionservice     sessionservice     = new Sessionservice();
    private final Reservationservice reservationservice = new Reservationservice();

    private ObservableList<Session> allData = FXCollections.observableArrayList();
    private Session selectedSession = null;

    private int currentProfId = 4;

    private final Map<String, Integer> groupeMap = new LinkedHashMap<>();

    private static final String[] STATUTS = {"planifiée", "en cours", "terminée", "annulée"};
    private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_H  = DateTimeFormatter.ofPattern("HH:mm");

    private static final String[][] CARD_COLORS = {
            {"#3B82F6","#2563EB"}, {"#6C63FF","#8B5CF6"}, {"#10B981","#059669"},
            {"#F59E0B","#D97706"}, {"#EF4444","#DC2626"}, {"#06B6D4","#0891B2"},
            {"#EC4899","#DB2777"}, {"#8B5CF6","#7C3AED"},
    };

    private static final String ERR   = "-fx-border-color:#E11D48;-fx-border-width:2;-fx-border-radius:10;";
    private static final String VALID = "-fx-border-color:#10B981;-fx-border-width:2;-fx-border-radius:10;";
    private static final String NORM  = "-fx-border-color:#E2E8F0;-fx-border-width:1.5;-fx-border-radius:10;";

    // ── Méthodes utilitaires ──────────────────────────────────────

    private String getStatutIcon(String s) {
        return switch (s != null ? s : "") {
            case "planifiée" -> "📅";
            case "en cours"  -> "🔄";
            case "terminée"  -> "✅";
            case "annulée"   -> "❌";
            default          -> "📌";
        };
    }

    private Label buildStatutBadge(String statut) {
        String bg, fg, icon;
        switch (statut != null ? statut : "") {
            case "planifiée" -> { bg="#EFF6FF"; fg="#3B82F6"; icon="📅"; }
            case "en cours"  -> { bg="#EEF2FF"; fg="#6C63FF"; icon="🔄"; }
            case "terminée"  -> { bg="#ECFDF5"; fg="#059669"; icon="✅"; }
            case "annulée"   -> { bg="#FFF1F2"; fg="#E11D48"; icon="❌"; }
            default           -> { bg="#F8FAFC"; fg="#64748B"; icon="📌"; }
        }
        Label l = new Label(icon + "  " + (statut != null ? statut : "—"));
        l.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";-fx-font-size:11px;" +
                "-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;");
        return l;
    }

    private Button makeBtn(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";-fx-font-size:11px;" +
                "-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:7 10 7 10;-fx-cursor:hand;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    private void updateCount(int n) {
        if (countLabel != null) countLabel.setText(n + " session(s)");
    }

    @FXML
    private void handleSearch() {
        applyFilter();
    }

    private void loadGroupes() {
        groupeMap.clear();
        try {
            Connection cx = MyDatabase.getInstance().getConnection();
            ResultSet rs = cx.createStatement().executeQuery("SELECT id, nom FROM groupe ORDER BY nom");
            while (rs.next()) groupeMap.put(rs.getString("nom"), rs.getInt("id"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        comboGroupe.setItems(FXCollections.observableArrayList(groupeMap.keySet()));
    }

    // ── Validation ────────────────────────────────────────────────

    private void setupLiveValidation() {
        fieldDate.valueProperty().addListener((o, old, val) -> {
            if (val != null) validateDate(val);
        });
        fieldHeure.textProperty().addListener((o, old, val) -> {
            if (val != null && !val.trim().isEmpty()) validateHeure(val.trim());
        });
        fieldLien.textProperty().addListener((o, old, val) -> {
            if (val != null && !val.trim().isEmpty()) {
                if (!val.trim().startsWith("http://") && !val.trim().startsWith("https://"))
                    setError(errLien, "Le lien doit commencer par https://", fieldLien);
                else { clearError(errLien); setValidStyle(fieldLien); }
            }
        });
        fieldDuree.textProperty().addListener((o, old, val) -> {
            if (val != null && !val.trim().isEmpty()) {
                try {
                    int d = Integer.parseInt(val.trim());
                    if (d <= 0 || d > 480) setError(errDuree, "Entre 1 et 480 minutes", fieldDuree);
                    else { clearError(errDuree); setValidStyle(fieldDuree); }
                } catch (NumberFormatException e) {
                    setError(errDuree, "Nombre entier requis", fieldDuree);
                }
            }
        });
        fieldPrix.textProperty().addListener((o, old, val) -> {
            if (val != null && !val.trim().isEmpty()) {
                try {
                    double p = Double.parseDouble(val.trim());
                    if (p < 0) setError(errPrix, "Prix ne peut pas être négatif", fieldPrix);
                    else { clearError(errPrix); setValidStyle(fieldPrix); }
                } catch (NumberFormatException e) {
                    setError(errPrix, "Nombre requis (ex: 50 ou 49.99)", fieldPrix);
                }
            }
        });
        fieldCapacite.textProperty().addListener((o, old, val) -> {
            if (val != null && !val.trim().isEmpty()) {
                try {
                    int c = Integer.parseInt(val.trim());
                    if (c < 3 || c > 15) setError(errCapacite, "Entre 3 et 15 places", fieldCapacite);
                    else { clearError(errCapacite); setValidStyle(fieldCapacite); }
                } catch (NumberFormatException e) {
                    setError(errCapacite, "Nombre entier requis", fieldCapacite);
                }
            }
        });
        fieldDescription.textProperty().addListener((o, old, val) -> {
            if (val != null && !val.trim().isEmpty()) {
                if (val.trim().length() < 10)
                    setError(errDescription, "Min 10 caractères", fieldDescription);
                else if (val.trim().length() > 500)
                    setError(errDescription, "Max 500 caractères", fieldDescription);
                else { clearError(errDescription); setValidStyle(fieldDescription); }
            }
        });
        comboStatut.valueProperty().addListener((o, old, val) -> {
            if (val != null) { clearError(errStatut); setValidStyle(comboStatut); }
        });
        comboGroupe.valueProperty().addListener((o, old, val) -> {
            if (val != null) { clearError(errGroupe); setValidStyle(comboGroupe); }
        });
    }

    private boolean validateDate(LocalDate date) {
        if (date == null) {
            setError(errDate, "Date obligatoire", fieldDate);
            return false;
        }
        if (!date.isAfter(LocalDate.now())) {
            setError(errDate, "La date doit être dans le futur", fieldDate);
            return false;
        }
        clearError(errDate);
        setValidStyle(fieldDate);
        return true;
    }

    private boolean validateHeure(String h) {
        if (h.isEmpty()) {
            setError(errHeure, "Heure obligatoire (HH:mm)", fieldHeure);
            return false;
        }
        if (!h.matches("^([01]?\\d|2[0-3]):[0-5]\\d$")) {
            setError(errHeure, "Format invalide (ex: 14:30)", fieldHeure);
            return false;
        }
        clearError(errHeure);
        setValidStyle(fieldHeure);
        return true;
    }

    private boolean validateForm() {
        boolean ok = true;

        // Date
        ok &= validateDate(fieldDate.getValue());

        // Heure
        ok &= validateHeure(fieldHeure.getText().trim());

        // Lien réunion
        String lien = fieldLien.getText().trim();
        if (lien.isEmpty()) {
            setError(errLien, "Lien obligatoire", fieldLien);
            ok = false;
        } else if (!lien.startsWith("http://") && !lien.startsWith("https://")) {
            setError(errLien, "Le lien doit commencer par https://", fieldLien);
            ok = false;
        } else {
            clearError(errLien);
            setValidStyle(fieldLien);
        }

        // Durée
        String duree = fieldDuree.getText().trim();
        if (duree.isEmpty()) {
            setError(errDuree, "Durée obligatoire", fieldDuree);
            ok = false;
        } else {
            try {
                int d = Integer.parseInt(duree);
                if (d <= 0 || d > 480) {
                    setError(errDuree, "Durée entre 1 et 480 minutes", fieldDuree);
                    ok = false;
                } else {
                    clearError(errDuree);
                    setValidStyle(fieldDuree);
                }
            } catch (NumberFormatException e) {
                setError(errDuree, "Durée doit être un nombre entier", fieldDuree);
                ok = false;
            }
        }

        // Prix
        String prix = fieldPrix.getText().trim();
        if (prix.isEmpty()) {
            setError(errPrix, "Prix obligatoire", fieldPrix);
            ok = false;
        } else {
            try {
                double p = Double.parseDouble(prix);
                if (p < 0) {
                    setError(errPrix, "Le prix ne peut pas être négatif", fieldPrix);
                    ok = false;
                } else {
                    clearError(errPrix);
                    setValidStyle(fieldPrix);
                }
            } catch (NumberFormatException e) {
                setError(errPrix, "Prix doit être un nombre (ex: 50 ou 49.99)", fieldPrix);
                ok = false;
            }
        }

        // Capacité max
        String cap = fieldCapacite.getText().trim();
        if (cap.isEmpty()) {
            setError(errCapacite, "Capacité obligatoire", fieldCapacite);
            ok = false;
        } else {
            try {
                int c = Integer.parseInt(cap);
                if (c < 3 || c > 15) {
                    setError(errCapacite, "Capacité entre 3 et 15 places", fieldCapacite);
                    ok = false;
                } else {
                    clearError(errCapacite);
                    setValidStyle(fieldCapacite);
                }
            } catch (NumberFormatException e) {
                setError(errCapacite, "Capacité doit être un nombre entier", fieldCapacite);
                ok = false;
            }
        }

        // Statut
        if (comboStatut.getValue() == null) {
            setError(errStatut, "Statut obligatoire", comboStatut);
            ok = false;
        } else {
            clearError(errStatut);
            setValidStyle(comboStatut);
        }

        // Groupe
        if (comboGroupe.getValue() == null) {
            setError(errGroupe, "Groupe obligatoire", comboGroupe);
            ok = false;
        } else {
            clearError(errGroupe);
            setValidStyle(comboGroupe);
        }

        // Description
        String desc = fieldDescription.getText().trim();
        if (desc.isEmpty()) {
            setError(errDescription, "Description obligatoire", fieldDescription);
            ok = false;
        } else if (desc.length() < 10) {
            setError(errDescription, "Description trop courte (min 10 caractères)", fieldDescription);
            ok = false;
        } else if (desc.length() > 500) {
            setError(errDescription, "Description trop longue (max 500 caractères)", fieldDescription);
            ok = false;
        } else {
            clearError(errDescription);
            setValidStyle(fieldDescription);
        }

        return ok;
    }

    private void setError(Label lbl, String msg, Control ctrl) {
        if (lbl == null) return;
        lbl.setText("⚠  " + msg);
        lbl.setVisible(true);
        lbl.setManaged(true);
        lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#E11D48;-fx-font-weight:bold;-fx-padding:3 0 0 4;");
        if (ctrl != null) ctrl.setStyle(ctrl.getStyle().replace(VALID, "").replace(NORM, "") + ERR);
    }

    private void clearError(Label lbl) {
        if (lbl == null) return;
        lbl.setText("");
        lbl.setVisible(false);
        lbl.setManaged(false);
    }

    private void setValidStyle(Control c) {
        if (c == null) return;
        c.setStyle(c.getStyle().replace(ERR, "").replace(NORM, "") + VALID);
        new Timeline(new KeyFrame(Duration.seconds(2),
                e -> c.setStyle(c.getStyle().replace(VALID, NORM)))).play();
    }

    private void clearErrors() {
        Label[] lbls = {errDate, errHeure, errLien, errDuree, errPrix, errCapacite, errStatut, errGroupe, errDescription};
        Control[] ctrls = {fieldDate, fieldHeure, fieldLien, fieldDuree, fieldPrix, fieldCapacite, comboStatut, comboGroupe, fieldDescription};
        for (Label l : lbls) clearError(l);
        for (Control c : ctrls) if (c != null) c.setStyle(c.getStyle().replace(ERR, "").replace(VALID, "") + NORM);
    }

    // ── Dialogues ─────────────────────────────────────────────────

    private void showDetails(Session s) {
        int ci = (int)(s.getId() % CARD_COLORS.length);
        String c1 = CARD_COLORS[ci][0], c2 = CARD_COLORS[ci][1];

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Session #" + s.getId());
        dialog.setHeaderText(null);

        VBox root = new VBox(0);
        root.setPrefWidth(520);

        VBox header = new VBox(10);
        header.setPadding(new Insets(28,32,24,32));
        header.setStyle("-fx-background-color:linear-gradient(to bottom right,"+c1+","+c2+");");

        HBox topRow = new HBox(14); topRow.setAlignment(Pos.CENTER_LEFT);
        Label iconCircle = new Label(getStatutIcon(s.getStatut()));
        iconCircle.setStyle("-fx-font-size:26px;-fx-background-color:rgba(255,255,255,0.20);" +
                "-fx-background-radius:50;-fx-padding:10 13 10 13;");
        VBox titleBox = new VBox(4);
        Label titleLbl = new Label("Session #" + s.getId());
        titleLbl.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label statutLbl = new Label(s.getStatut() != null ? s.getStatut().toUpperCase() : "");
        statutLbl.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 12 3 12;");
        titleBox.getChildren().addAll(titleLbl, statutLbl);
        topRow.getChildren().addAll(iconCircle, titleBox);

        Label dateLbl = new Label("📅  " + (s.getDateHeure() != null ? s.getDateHeure().format(FMT_DT) : "—"));
        dateLbl.setStyle("-fx-font-size:14px;-fx-text-fill:rgba(255,255,255,0.90);-fx-font-weight:bold;");
        header.getChildren().addAll(topRow, dateLbl);

        HBox chips = new HBox(10);
        chips.setPadding(new Insets(16,32,12,32));
        chips.setStyle("-fx-background-color:#F8FAFC;");
        if (s.getDuree() != null) chips.getChildren().add(makeChip("⏱ "+s.getDuree()+" min","#EFF6FF","#3B82F6"));
        if (s.getPrix()  != null) chips.getChildren().add(makeChip("💰 "+String.format("%.0f",s.getPrix())+" TND","#F0FDF4","#16A34A"));
        if (s.getCapaciteMax() != null) chips.getChildren().add(makeChip("🪑 "+s.getCapaciteMax()+" places","#FFF7ED","#EA580C"));

        VBox body = new VBox(0);
        body.setStyle("-fx-background-color:#FFFFFF;");
        body.setPadding(new Insets(20,32,8,32));

        String[][] rows = {
                {"🆔","ID",           String.valueOf(s.getId())},
                {"📅","Date & Heure", s.getDateHeure()!=null?s.getDateHeure().format(FMT_DT):"—"},
                {"📌","Statut",       s.getStatut()!=null?s.getStatut():"—"},
                {"🔗","Lien réunion", s.getLienReunion()!=null?s.getLienReunion():"—"},
                {"👥","Groupe",       "#"+s.getIdGroupId()},
                {"👤","Professeur",   "#"+s.getIdUserId()},
        };

        for (int i = 0; i < rows.length; i++) {
            HBox row = new HBox(0);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(12,0,12,0));
            if (i < rows.length-1)
                row.setStyle("-fx-border-color:transparent transparent #F1F5F9 transparent;-fx-border-width:1;");
            Label emoji = new Label(rows[i][0]);
            emoji.setStyle("-fx-font-size:16px;-fx-min-width:32;");
            Label key = new Label(rows[i][1]);
            key.setStyle("-fx-font-size:12px;-fx-text-fill:#94A3B8;-fx-font-weight:bold;-fx-min-width:120;");
            Label val = new Label(rows[i][2]);
            val.setStyle("-fx-font-size:13px;-fx-text-fill:#1E293B;-fx-font-weight:bold;-fx-wrap-text:true;");
            val.setWrapText(true);
            HBox.setHgrow(val, Priority.ALWAYS);
            row.getChildren().addAll(emoji, key, val);
            body.getChildren().add(row);
        }

        if (s.getDescription() != null && !s.getDescription().isBlank()) {
            VBox descBox = new VBox(8);
            descBox.setPadding(new Insets(12,32,20,32));
            descBox.setStyle("-fx-background-color:#FFFFFF;");
            Label descTitle = new Label("📝  Description");
            descTitle.setStyle("-fx-font-size:12px;-fx-text-fill:#94A3B8;-fx-font-weight:bold;");
            Label descVal = new Label(s.getDescription());
            descVal.setStyle("-fx-font-size:13px;-fx-text-fill:#374151;-fx-wrap-text:true;" +
                    "-fx-background-color:#F8FAFC;-fx-background-radius:10;-fx-padding:12 14 12 14;");
            descVal.setWrapText(true);
            descBox.getChildren().addAll(descTitle, descVal);
            body.getChildren().add(descBox);
        }

        root.getChildren().addAll(header, chips, body);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;" +
                "-fx-background-radius:16;-fx-border-radius:16;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button closeBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeBtn.setText("✕  Fermer");
        closeBtn.setStyle("-fx-background-color:linear-gradient(to right,"+c1+","+c2+");" +
                "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-background-radius:10;-fx-padding:10 28 10 28;-fx-cursor:hand;");
        dialog.showAndWait();
    }

    private Label makeChip(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";-fx-font-size:12px;" +
                "-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:6 14 6 14;");
        return l;
    }

    private void openEditForm(Session s) {
        selectedSession = s;
        clearErrors();
        fieldDate.setValue(s.getDateHeure() != null ? s.getDateHeure().toLocalDate() : null);
        fieldHeure.setText(s.getDateHeure() != null ? s.getDateHeure().format(FMT_H) : "");
        fieldLien.setText(s.getLienReunion() != null ? s.getLienReunion() : "");
        fieldDuree.setText(s.getDuree() != null ? String.valueOf(s.getDuree()) : "");
        fieldPrix.setText(s.getPrix() != null ? String.valueOf(s.getPrix()) : "");
        fieldCapacite.setText(s.getCapaciteMax() != null ? String.valueOf(s.getCapaciteMax()) : "");
        comboStatut.setValue(s.getStatut());
        fieldDescription.setText(s.getDescription() != null ? s.getDescription() : "");
        groupeMap.entrySet().stream()
                .filter(e -> e.getValue() == s.getIdGroupId())
                .map(Map.Entry::getKey).findFirst().ifPresent(comboGroupe::setValue);
        formTitle.setText("Modifier la Session");
        formTitleIcon.setText("✎");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    private void handleDelete(Session s) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer la session du " + (s.getDateHeure() != null ? s.getDateHeure().format(FMT_DT) : "?"));
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette session ?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    sessionservice.supprimer(s.getId());
                    loadData();
                    showStyledAlert("success", "Session supprimée", "La session a été supprimée avec succès.");
                } catch (SQLException e) {
                    showStyledAlert("error", "Erreur", e.getMessage());
                }
            }
        });
    }

    private void showReservationsDialog(Session s, String c1, String c2) {
        try {
            List<Reservation> resaList = reservationservice.recupererParSession(s.getId());

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Réservations — Session #" + s.getId());
            dialog.setHeaderText(null);

            VBox root = new VBox(0);
            root.setPrefWidth(540);

            VBox header = new VBox(10);
            header.setPadding(new Insets(24,28,20,28));
            header.setStyle("-fx-background-color:linear-gradient(to right,"+c1+","+c2+");");
            Label title = new Label("📋  Réservations — Session #" + s.getId());
            title.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:white;");
            Label sub = new Label(s.getDateHeure()!=null?s.getDateHeure().format(FMT_DT):"");
            sub.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.80);");

            HBox statsRow = new HBox(10);
            long nbAtt = resaList.stream().filter(r->"en attente".equals(r.getStatut())).count();
            long nbAcc = resaList.stream().filter(r->"acceptée".equals(r.getStatut())).count();
            long nbRef = resaList.stream().filter(r->"refusée".equals(r.getStatut())).count();
            statsRow.getChildren().addAll(
                    makeWhiteChip("⏳ "+nbAtt+" attente"),
                    makeWhiteChip("✅ "+nbAcc+" acceptée(s)"),
                    makeWhiteChip("❌ "+nbRef+" refusée(s)")
            );
            header.getChildren().addAll(title, sub, statsRow);

            VBox list = new VBox(0);
            list.setStyle("-fx-background-color:#F8FAFC;");
            list.setPadding(new Insets(16,20,16,20));

            if (resaList.isEmpty()) {
                VBox empty = new VBox(10); empty.setAlignment(Pos.CENTER); empty.setPadding(new Insets(30));
                Label eIcon = new Label("📭"); eIcon.setStyle("-fx-font-size:36px;");
                Label eMsg = new Label("Aucune réservation pour cette session");
                eMsg.setStyle("-fx-font-size:13px;-fx-text-fill:#9CA3AF;");
                empty.getChildren().addAll(eIcon, eMsg);
                list.getChildren().add(empty);
            } else {
                for (Reservation r : resaList) {
                    HBox card = new HBox(14);
                    card.setAlignment(Pos.CENTER_LEFT);
                    card.setPadding(new Insets(14,16,14,16));
                    card.setStyle("-fx-background-color:#FFFFFF;-fx-background-radius:12;" +
                            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,3);");
                    VBox.setMargin(card, new Insets(0,0,10,0));

                    Label statIcon = new Label(getResaIcon(r.getStatut()));
                    statIcon.setStyle("-fx-font-size:22px;-fx-background-color:"+getResaBg(r.getStatut())+";" +
                            "-fx-background-radius:50;-fx-padding:8 10 8 10;");

                    VBox info = new VBox(4); HBox.setHgrow(info, Priority.ALWAYS);
                    Label resaId = new Label("Réservation #" + r.getId());
                    resaId.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1E293B;");
                    HBox meta = new HBox(8);
                    Label userLbl = new Label("👤 Utilisateur #" + r.getIdUserId());
                    userLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#64748B;");
                    Label dateLbl = new Label("📅 " + r.getDateReservation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    dateLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#64748B;");
                    meta.getChildren().addAll(userLbl, dateLbl);
                    info.getChildren().addAll(resaId, meta);

                    Label badge = new Label(r.getStatut().toUpperCase());
                    badge.setStyle("-fx-background-color:"+getResaBg(r.getStatut())+";-fx-text-fill:"+getResaFg(r.getStatut())+";" +
                            "-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 10 4 10;");

                    if ("en attente".equals(r.getStatut())) {
                        VBox btns = new VBox(6);
                        Button btnAcc = new Button("✅ Accepter");
                        btnAcc.setStyle("-fx-background-color:#ECFDF5;-fx-text-fill:#059669;" +
                                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;" +
                                "-fx-padding:5 10 5 10;-fx-cursor:hand;");
                        Button btnRef = new Button("❌ Refuser");
                        btnRef.setStyle("-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;" +
                                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;" +
                                "-fx-padding:5 10 5 10;-fx-cursor:hand;");
                        btnAcc.setOnAction(e -> {
                            try {
                                reservationservice.accepter(r.getId());
                                dialog.close();
                                loadData();
                                showStyledAlert("success","Acceptée !","Réservation #"+r.getId()+" acceptée.");
                            } catch (SQLException ex) { showStyledAlert("error","Erreur",ex.getMessage()); }
                        });
                        btnRef.setOnAction(e -> {
                            try {
                                reservationservice.refuser(r.getId(), "Refusé par le professeur");
                                dialog.close();
                                loadData();
                                showStyledAlert("warning","Refusée","Réservation #"+r.getId()+" refusée.");
                            } catch (SQLException ex) { showStyledAlert("error","Erreur",ex.getMessage()); }
                        });
                        btns.getChildren().addAll(btnAcc, btnRef);
                        card.getChildren().addAll(statIcon, info, btns);
                    } else {
                        card.getChildren().addAll(statIcon, info, badge);
                    }
                    list.getChildren().add(card);
                }
            }

            ScrollPane scroll = new ScrollPane(list);
            scroll.setFitToWidth(true);
            scroll.setPrefHeight(Math.min(resaList.size() * 90 + 40, 380));
            scroll.setStyle("-fx-background-color:#F8FAFC;-fx-background:#F8FAFC;");

            root.getChildren().addAll(header, scroll);
            dialog.getDialogPane().setContent(root);
            dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            Button closeBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
            closeBtn.setText("✕  Fermer");
            closeBtn.setStyle("-fx-background-color:linear-gradient(to right,"+c1+","+c2+");" +
                    "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;" +
                    "-fx-background-radius:10;-fx-padding:10 28 10 28;-fx-cursor:hand;");
            dialog.showAndWait();

        } catch (SQLException e) {
            showStyledAlert("error", "Erreur", e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Label makeWhiteChip(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:rgba(255,255,255,0.20);-fx-text-fill:white;" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;");
        return l;
    }

    private String getResaIcon(String s) {
        return switch(s!=null?s:"") {
            case "en attente"->"⏳"; case "acceptée"->"✅";
            case "refusée"->"❌"; case "annulée"->"🚫"; default->"📋";
        };
    }

    private String getResaBg(String s) {
        return switch(s!=null?s:"") {
            case "en attente"->"#FFFBEB"; case "acceptée"->"#ECFDF5";
            case "refusée"->"#FFF1F2"; case "annulée"->"#F8FAFC"; default->"#F1F5F9";
        };
    }

    private String getResaFg(String s) {
        return switch(s!=null?s:"") {
            case "en attente"->"#D97706"; case "acceptée"->"#059669";
            case "refusée"->"#E11D48"; case "annulée"->"#64748B"; default->"#475569";
        };
    }

    private void showStyledAlert(String type, String title, String message) {
        Alert.AlertType alertType = switch (type) {
            case "success" -> Alert.AlertType.INFORMATION;
            case "error"   -> Alert.AlertType.ERROR;
            case "warning" -> Alert.AlertType.WARNING;
            default        -> Alert.AlertType.INFORMATION;
        };
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ── Cartes ────────────────────────────────────────────────────

    private VBox buildCard(Session s, int ci) {
        String c1 = CARD_COLORS[ci][0], c2 = CARD_COLORS[ci][1];
        int nbResas = 0;
        try {
            nbResas = reservationservice.recupererParSession(s.getId()).size();
        } catch (SQLException ignored) {}

        VBox card = new VBox(0);
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        card.setStyle("-fx-background-color:#FFFFFF;-fx-background-radius:18;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),20,0,0,5);-fx-cursor:hand;");
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),26,0,0,8);" +
                        "-fx-scale-x:1.02;-fx-scale-y:1.02;-fx-cursor:hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),20,0,0,5);-fx-cursor:hand;"));

        VBox header = new VBox(8);
        header.setPadding(new Insets(20,20,16,20));
        header.setStyle("-fx-background-color:linear-gradient(to bottom right," + c1 + "," + c2 + ");-fx-background-radius:18 18 0 0;");

        HBox htop = new HBox(8); htop.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(getStatutIcon(s.getStatut()));
        iconLbl.setStyle("-fx-font-size:18px;-fx-background-color:rgba(255,255,255,0.22);-fx-background-radius:50;-fx-padding:6 8 6 8;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label resaBadge = new Label("👥 " + nbResas + " résa");
        resaBadge.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 9 3 9;");
        htop.getChildren().addAll(iconLbl, sp, resaBadge);

        Label dateLabel = new Label(s.getDateHeure() != null ? s.getDateHeure().format(FMT_DT) : "—");
        dateLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        header.getChildren().addAll(htop, dateLabel);

        VBox body = new VBox(10);
        body.setPadding(new Insets(14,18,10,18));
        String desc = s.getDescription() != null && !s.getDescription().isBlank()
                ? (s.getDescription().length() > 75 ? s.getDescription().substring(0,72)+"…" : s.getDescription())
                : "Aucune description.";
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;-fx-wrap-text:true;");
        descLabel.setWrapText(true);

        Label statutBadge = buildStatutBadge(s.getStatut());

        HBox infoRow = new HBox(8);
        if (s.getDuree() != null) {
            Label dureeL = new Label("⏱ " + s.getDuree() + " min");
            dureeL.setStyle("-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
            infoRow.getChildren().add(dureeL);
        }
        if (s.getPrix() != null) {
            Label prixL = new Label("💰 " + String.format("%.0f", s.getPrix()) + " TND");
            prixL.setStyle("-fx-background-color:#F0FDF4;-fx-text-fill:#16A34A;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
            infoRow.getChildren().add(prixL);
        }
        if (s.getCapaciteMax() != null) {
            Label capL = new Label("🪑 Max " + s.getCapaciteMax());
            capL.setStyle("-fx-background-color:#FFF7ED;-fx-text-fill:#EA580C;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
            infoRow.getChildren().add(capL);
        }

        body.getChildren().addAll(descLabel, statutBadge, infoRow);
        if (s.getLienReunion() != null && !s.getLienReunion().isBlank()) {
            Label lienL = new Label("🔗 " + (s.getLienReunion().length() > 35 ? s.getLienReunion().substring(0,32)+"…" : s.getLienReunion()));
            lienL.setStyle("-fx-font-size:11px;-fx-text-fill:#6C63FF;-fx-wrap-text:true;");
            lienL.setWrapText(true);
            body.getChildren().add(lienL);
        }

        Separator sep = new Separator();
        VBox.setMargin(sep, new Insets(4,0,0,0));

        HBox row1 = new HBox(6); row1.setAlignment(Pos.CENTER);
        Button btnVoir = makeBtn("👁 Détails", "#EFF6FF", "#3B82F6");
        Button btnEdit = makeBtn("✏ Modifier", c1+"22", c1);
        Button btnDel  = makeBtn("🗑 Supprimer", "#FFF1F2", "#E11D48");
        HBox.setHgrow(btnVoir, Priority.ALWAYS); btnVoir.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnEdit, Priority.ALWAYS); btnEdit.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDel,  Priority.ALWAYS); btnDel.setMaxWidth(Double.MAX_VALUE);
        btnVoir.setOnAction(e -> showDetails(s));
        btnEdit.setOnAction(e -> openEditForm(s));
        btnDel.setOnAction(e -> handleDelete(s));
        row1.getChildren().addAll(btnVoir, btnEdit, btnDel);

        final int nb = nbResas;
        Button btnResas = new Button("📋  Gérer les réservations (" + nb + ")");
        btnResas.setMaxWidth(Double.MAX_VALUE);
        btnResas.setStyle("-fx-background-color:linear-gradient(to right,"+c1+","+c2+");-fx-text-fill:white;" +
                "-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:10;-fx-padding:9 0 9 0;-fx-cursor:hand;");
        btnResas.setOnAction(e -> showReservationsDialog(s, c1, c2));

        VBox actions = new VBox(8);
        actions.setPadding(new Insets(10,14,14,14));
        actions.getChildren().addAll(row1, btnResas);

        card.getChildren().addAll(header, body, sep, actions);
        return card;
    }

    // ── Rendu ─────────────────────────────────────────────────────

    private void renderCards(List<Session> list) {
        cardsContainer.getChildren().clear();
        int i = 0;
        for (Session s : list) {
            cardsContainer.getChildren().add(buildCard(s, i++ % CARD_COLORS.length));
        }
        if (list.isEmpty()) {
            VBox empty = new VBox(12); empty.setAlignment(Pos.CENTER); empty.setPadding(new Insets(60));
            Label icon = new Label("📭"); icon.setStyle("-fx-font-size:48px;");
            Label msg = new Label("Aucune session trouvée");
            msg.setStyle("-fx-font-size:15px;-fx-text-fill:#9CA3AF;");
            empty.getChildren().addAll(icon, msg);
            cardsContainer.getChildren().add(empty);
        }
    }

    private void applyFilter() {
        String q = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        String statut = filterStatut != null ? filterStatut.getValue() : "Tous";
        List<Session> filtered = allData.stream()
                .filter(s -> {
                    boolean matchQ = q.isEmpty()
                            || (s.getDescription() != null && s.getDescription().toLowerCase().contains(q))
                            || (s.getStatut() != null && s.getStatut().toLowerCase().contains(q))
                            || (s.getLienReunion() != null && s.getLienReunion().toLowerCase().contains(q));
                    boolean matchS = "Tous".equals(statut) || statut.equals(s.getStatut());
                    return matchQ && matchS;
                })
                .collect(Collectors.toList());
        renderCards(filtered);
        updateCount(filtered.size());
    }

    // ── Cycle de vie ──────────────────────────────────────────────

    @FXML
    public void initialize() {
        comboStatut.setItems(FXCollections.observableArrayList(STATUTS));
        filterStatut.setItems(FXCollections.observableArrayList(
                "Tous", "planifiée", "en cours", "terminée", "annulée"));
        filterStatut.setValue("Tous");
        filterStatut.setOnAction(e -> handleSearch());
        loadGroupes();
        setupLiveValidation();
    }

    public void setProfId(int id) {
        this.currentProfId = id;
        System.out.println("=== setProfId appelé avec id=" + id);
        loadData();
    }

    private void loadData() {
        System.out.println("=== loadData() appelé, profId=" + currentProfId);
        try {
            List<Session> sessions = sessionservice.recuperer();
            System.out.println("=== Sessions trouvées: " + sessions.size());
            allData = FXCollections.observableArrayList(sessions);
            applyFilter();
        } catch (SQLException e) {
            System.out.println("=== ERREUR SQL: " + e.getMessage());
            e.printStackTrace();
            showStyledAlert("error", "Erreur de chargement", e.getMessage());
        }
    }

    @FXML
    private void handleAjouter() {
        selectedSession = null;
        clearErrors();
        fieldDate.setValue(null);
        fieldHeure.setText("");
        fieldLien.setText("");
        fieldDuree.setText("");
        fieldPrix.setText("");
        fieldCapacite.setText("");
        comboStatut.setValue(null);
        comboGroupe.setValue(null);
        fieldDescription.setText("");
        formTitle.setText("Nouvelle Session");
        formTitleIcon.setText("✚");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    @FXML
    private void handleCancel() {
        formCard.setVisible(false);
        formCard.setManaged(false);
        selectedSession = null;
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;
        try {
            LocalDateTime dt = LocalDateTime.of(
                    fieldDate.getValue(),
                    java.time.LocalTime.parse(fieldHeure.getText().trim())
            );
            Integer groupeId = groupeMap.get(comboGroupe.getValue());
            Session s = new Session(
                    dt,
                    comboStatut.getValue(),
                    fieldLien.getText().trim(),
                    groupeId != null ? groupeId : 0,
                    currentProfId,
                    Integer.parseInt(fieldDuree.getText().trim()),
                    Double.parseDouble(fieldPrix.getText().trim()),
                    fieldDescription.getText().trim(),
                    Integer.parseInt(fieldCapacite.getText().trim())
            );
            if (selectedSession != null) {
                s.setId(selectedSession.getId());
                sessionservice.modifier(s);
                showStyledAlert("success", "Session modifiée", "La session a été modifiée.");
            } else {
                sessionservice.ajouter(s);
                showStyledAlert("success", "Session créée", "La session a été créée.");
            }
            handleCancel();
            loadData();
        } catch (Exception e) {
            showStyledAlert("error", "Erreur", e.getMessage());
        }
    }
}