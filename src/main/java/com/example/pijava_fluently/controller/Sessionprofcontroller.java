package com.example.pijava_fluently.controller;
import com.example.pijava_fluently.utils.NotificationBell;
import com.example.pijava_fluently.entites.Reservation;
import com.example.pijava_fluently.entites.Session;
import com.example.pijava_fluently.services.Reservationservice;
import com.example.pijava_fluently.services.Sessionservice;
import com.example.pijava_fluently.utils.JitsiUtil;
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
import javafx.scene.control.ButtonBar;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.fxml.FXMLLoader;
import com.example.pijava_fluently.utils.ReminderScheduler;
public class Sessionprofcontroller {

    public Sessionprofcontroller() {
    }

    @FXML
    private VBox rootPane;
    @FXML
    private Label countLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterStatut;
    @FXML
    private FlowPane cardsContainer;
    @FXML
    private VBox formCard;
    @FXML
    private Label formTitle;
    @FXML
    private Label formTitleIcon;
    @FXML
    private TextField fieldNom;
    @FXML
    private DatePicker fieldDate;
    @FXML
    private TextField fieldHeure;
    @FXML
    private TextField fieldLien;
    @FXML
    private TextField fieldDuree;
    @FXML
    private TextField fieldPrix;
    @FXML
    private TextField fieldCapacite;
    @FXML
    private ComboBox<String> comboStatut;
    @FXML
    private ComboBox<String> comboGroupe;
    @FXML
    private TextArea fieldDescription;
    @FXML
    private Label errNom, errDate, errHeure, errLien, errDuree, errPrix,
            errCapacite, errStatut, errGroupe, errDescription;
    @FXML private StackPane       bellContainer;
    private      NotificationBell notifBell;


    private final Sessionservice sessionservice = new Sessionservice();
    private final Reservationservice reservationservice = new Reservationservice();
    private ObservableList<Session> allData = FXCollections.observableArrayList();
    private Session selectedSession = null;
    private int currentProfId = 4;
    private final Map<String, Integer> groupeMap = new LinkedHashMap<>();

    private static final String[] STATUTS = {"planifiée", "en cours", "terminée", "annulée"};
    private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_H = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[][] CARD_COLORS = {
            {"#3B82F6", "#2563EB"}, {"#6C63FF", "#8B5CF6"}, {"#10B981", "#059669"},
            {"#F59E0B", "#D97706"}, {"#EF4444", "#DC2626"}, {"#06B6D4", "#0891B2"},
            {"#EC4899", "#DB2777"}, {"#8B5CF6", "#7C3AED"},
    };

    // ══════════════════════════════════════════════════════════════
    // STYLES DE VALIDATION — CENTRALISÉS
    // ══════════════════════════════════════════════════════════════

    /**
     * Bordure rouge : champ invalide
     */
    private static final String ERR = "-fx-border-color:#E11D48;-fx-border-width:2;-fx-border-radius:10;";
    /**
     * Bordure verte : champ valide
     */
    private static final String VALID = "-fx-border-color:#10B981;-fx-border-width:2;-fx-border-radius:10;";
    /**
     * Bordure grise neutre : état initial
     */
    private static final String NORM = "-fx-border-color:#E2E8F0;-fx-border-width:1.5;-fx-border-radius:10;";

    // ══════════════════════════════════════════════════════════════
    // RÈGLES MÉTIER — CONSTANTES
    // ══════════════════════════════════════════════════════════════

    // Nom
    private static final int NOM_MIN = 3;
    private static final int NOM_MAX = 50;
    // Durée (minutes)
    private static final int DUREE_MIN = 30;
    private static final int DUREE_MAX = 120;
    // Prix (TND)
    private static final double PRIX_MIN = 10.0;
    private static final double PRIX_MAX = 100.0;
    // Capacité
    private static final int CAP_MIN = 3;
    private static final int CAP_MAX = 20;
    // Description
    private static final int DESC_MIN = 3;
    private static final int DESC_MAX = 255;

    // ══════════════════════════════════════════════════════════════
    // HELPERS UI
    // ══════════════════════════════════════════════════════════════

    private String getStatutIcon(String s) {
        return switch (s != null ? s : "") {
            case "planifiée" -> "📅";
            case "en cours" -> "🔄";
            case "terminée" -> "✅";
            case "annulée" -> "❌";
            default -> "📌";
        };
    }

    private Label buildStatutBadge(String statut) {
        String bg, fg, icon;
        switch (statut != null ? statut : "") {
            case "planifiée" -> {
                bg = "#EFF6FF";
                fg = "#3B82F6";
                icon = "📅";
            }
            case "en cours" -> {
                bg = "#EEF2FF";
                fg = "#6C63FF";
                icon = "🔄";
            }
            case "terminée" -> {
                bg = "#ECFDF5";
                fg = "#059669";
                icon = "✅";
            }
            case "annulée" -> {
                bg = "#FFF1F2";
                fg = "#E11D48";
                icon = "❌";
            }
            default -> {
                bg = "#F8FAFC";
                fg = "#64748B";
                icon = "📌";
            }
        }
        Label l = new Label(icon + "  " + (statut != null ? statut : "—"));
        l.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg +
                ";-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-background-radius:20;-fx-padding:4 12 4 12;");
        return l;
    }

    private Button makeBtn(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-text-fill:" + fg + ";" +
                        "-fx-font-size:12px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-background-radius:10;" +
                        "-fx-padding:9 14 9 14;" +
                        "-fx-cursor:hand;" +
                        "-fx-border-color:transparent;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),6,0,0,2);"
        );
        btn.setOnMouseEntered(e -> {
            btn.setScaleX(1.04);
            btn.setScaleY(1.04);
            btn.setOpacity(0.92);
        });
        btn.setOnMouseExited(e -> {
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
            btn.setOpacity(1.0);
        });
        return btn;
    }

    private void updateCount(int n) {
        if (countLabel != null) countLabel.setText(n + " session(s)");
    }

    @FXML
    private void handleSearch() {
        applyFilter();
    }

    @FXML
    private void handleGenererLien() {
        String tempLink = "https://meet.jit.si/fluently-new-" + System.currentTimeMillis();
        fieldLien.setText(tempLink);
        applyValidStyle(fieldLien, true);
        new Timeline(new KeyFrame(Duration.seconds(1.5),
                e -> applyValidStyle(fieldLien, true))).play();
    }

    // ══════════════════════════════════════════════════════════════
    // MÉTHODES DE STYLE — CENTRALISÉES ET COMPLÈTES
    // ══════════════════════════════════════════════════════════════

    /**
     * Applique le style vert (valide) ou rouge (invalide) sur un Control.
     * Nettoie les anciens styles avant d'appliquer le nouveau.
     */
    private void applyValidStyle(Control ctrl, boolean valid) {
        if (ctrl == null) return;
        // Récupérer le style de base sans les anciens états
        String base = ctrl.getStyle()
                .replace(ERR, "")
                .replace(VALID, "")
                .replace(NORM, "");
        ctrl.setStyle(base + (valid ? VALID : ERR));
    }

    /**
     * Remet le style neutre sur un Control.
     */
    private void applyNormStyle(Control ctrl) {
        if (ctrl == null) return;
        String base = ctrl.getStyle()
                .replace(ERR, "")
                .replace(VALID, "")
                .replace(NORM, "");
        ctrl.setStyle(base + NORM);
    }

    /**
     * Affiche le message d'erreur et colore le champ en rouge.
     */
    private void setError(Label lbl, String msg, Control ctrl) {
        if (lbl != null) {
            lbl.setText("⚠ " + msg);
            lbl.setVisible(true);
            lbl.setManaged(true);
        }
        applyValidStyle(ctrl, false);
    }

    /**
     * Efface le message d'erreur (label invisible).
     */
    private void clearError(Label lbl) {
        if (lbl != null) {
            lbl.setText("");
            lbl.setVisible(false);
            lbl.setManaged(false);
        }
    }

    /**
     * Marque un champ comme valide : vert + efface l'erreur.
     */
    private void markValid(Label lbl, Control ctrl) {
        clearError(lbl);
        applyValidStyle(ctrl, true);
    }

    /**
     * Remet tous les champs à l'état neutre et efface tous les messages.
     */
    private void clearErrors() {
        Label[] labels = {errNom, errDate, errHeure, errLien, errDuree,
                errPrix, errCapacite, errStatut, errGroupe, errDescription};
        Control[] controls = {fieldNom, fieldDate, fieldHeure, fieldLien,
                fieldDuree, fieldPrix, fieldCapacite,
                comboStatut, comboGroupe, fieldDescription};
        for (Label l : labels) clearError(l);
        for (Control c : controls) applyNormStyle(c);
    }

    // ══════════════════════════════════════════════════════════════
    // FONCTIONS DE VALIDATION INDIVIDUELLES
    // ══════════════════════════════════════════════════════════════

    /**
     * Valide le nom : lettres (avec accents), chiffres, espaces, tirets.
     * Longueur entre NOM_MIN et NOM_MAX caractères.
     */
    private boolean isNomValide(String v) {
        if (v == null || v.isBlank()) return false;
        String t = v.trim();
        return t.length() >= NOM_MIN
                && t.length() <= NOM_MAX
                && t.matches("[\\p{L}\\d\\s\\-–—']+");
    }

    /**
     * Valide la date : doit être dans le futur (strictement après aujourd'hui).
     */
    private boolean isDateValide(LocalDate d) {
        return d != null && d.isAfter(LocalDate.now());
    }

    /**
     * Valide le format HH:mm.
     */
    private boolean isHeureValide(String h) {
        if (h == null || h.isBlank()) return false;
        try {
            LocalTime.parse(h.trim(), FMT_H);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Valide la durée : entier entre DUREE_MIN et DUREE_MAX inclus.
     */
    private boolean isDureeValide(String v) {
        if (v == null || v.isBlank()) return false;
        try {
            int n = Integer.parseInt(v.trim());
            return n >= DUREE_MIN && n <= DUREE_MAX;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Valide le prix : double entre PRIX_MIN et PRIX_MAX inclus (0 interdit).
     */
    private boolean isPrixValide(String v) {
        if (v == null || v.isBlank()) return false;
        try {
            double n = Double.parseDouble(v.trim());
            return n >= PRIX_MIN && n <= PRIX_MAX;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Valide la capacité : entier entre CAP_MIN et CAP_MAX inclus.
     */
    private boolean isCapaciteValide(String v) {
        if (v == null || v.isBlank()) return false;
        try {
            int n = Integer.parseInt(v.trim());
            return n >= CAP_MIN && n <= CAP_MAX;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Valide la description : contient au moins une lettre alphabétique,
     * longueur entre DESC_MIN et DESC_MAX.
     */
    private boolean isDescriptionValide(String v) {
        if (v == null || v.isBlank()) return false;
        String t = v.trim();
        return t.length() >= DESC_MIN
                && t.length() <= DESC_MAX
                && t.matches(".*\\p{L}.*");  // au moins une lettre
    }

    // ══════════════════════════════════════════════════════════════
    // VALIDATION EN TEMPS RÉEL (live)
    // ══════════════════════════════════════════════════════════════

    private void setupLiveValidation() {

        // ── NOM ──────────────────────────────────────────────────
        if (fieldNom != null)
            fieldNom.textProperty().addListener((o, old, nw) -> {
                if (nw == null || nw.isBlank()) {
                    // Champ vide : neutre (pas encore touché)
                    applyNormStyle(fieldNom);
                    clearError(errNom);
                } else if (!isNomValide(nw)) {
                    String msg;
                    String t = nw.trim();
                    if (t.length() < NOM_MIN)
                        msg = "Minimum " + NOM_MIN + " caractères requis.";
                    else if (t.length() > NOM_MAX)
                        msg = "Maximum " + NOM_MAX + " caractères autorisés.";
                    else
                        msg = "Caractères invalides (lettres, chiffres, tirets uniquement).";
                    setError(errNom, msg, fieldNom);
                } else {
                    markValid(errNom, fieldNom);
                }
            });

        // ── HEURE ────────────────────────────────────────────────
        if (fieldHeure != null)
            fieldHeure.textProperty().addListener((o, old, nw) -> {
                if (nw == null || nw.isBlank()) {
                    applyNormStyle(fieldHeure);
                    clearError(errHeure);
                } else if (!isHeureValide(nw)) {
                    setError(errHeure, "Format invalide — utilisez HH:mm (ex : 14:30).", fieldHeure);
                } else {
                    markValid(errHeure, fieldHeure);
                }
            });

        // ── DATE (DatePicker) ─────────────────────────────────────
        if (fieldDate != null)
            fieldDate.valueProperty().addListener((o, old, nw) -> {
                if (nw == null) {
                    applyNormStyle(fieldDate);
                    clearError(errDate);
                } else if (!isDateValide(nw)) {
                    setError(errDate, "La date doit être dans le futur.", fieldDate);
                } else {
                    markValid(errDate, fieldDate);
                }
            });

        // ── DURÉE ────────────────────────────────────────────────
        if (fieldDuree != null)
            fieldDuree.textProperty().addListener((o, old, nw) -> {
                if (nw == null || nw.isBlank()) {
                    applyNormStyle(fieldDuree);
                    clearError(errDuree);
                } else if (!isDureeValide(nw)) {
                    String msg;
                    try {
                        int v = Integer.parseInt(nw.trim());
                        msg = (v < DUREE_MIN)
                                ? "Minimum " + DUREE_MIN + " minutes."
                                : "Maximum " + DUREE_MAX + " minutes.";
                    } catch (NumberFormatException ex) {
                        msg = "Entier requis (entre " + DUREE_MIN + " et " + DUREE_MAX + " min).";
                    }
                    setError(errDuree, msg, fieldDuree);
                } else {
                    markValid(errDuree, fieldDuree);
                }
            });

        // ── PRIX ─────────────────────────────────────────────────
        if (fieldPrix != null)
            fieldPrix.textProperty().addListener((o, old, nw) -> {
                if (nw == null || nw.isBlank()) {
                    applyNormStyle(fieldPrix);
                    clearError(errPrix);
                } else if (!isPrixValide(nw)) {
                    String msg;
                    try {
                        double v = Double.parseDouble(nw.trim());
                        if (v == 0)
                            msg = "Le prix 0 n'est pas autorisé.";
                        else if (v < PRIX_MIN)
                            msg = "Minimum " + (int) PRIX_MIN + " TND.";
                        else
                            msg = "Maximum " + (int) PRIX_MAX + " TND.";
                    } catch (NumberFormatException ex) {
                        msg = "Nombre décimal requis (entre " + (int) PRIX_MIN + " et " + (int) PRIX_MAX + " TND).";
                    }
                    setError(errPrix, msg, fieldPrix);
                } else {
                    markValid(errPrix, fieldPrix);
                }
            });

        // ── CAPACITÉ ─────────────────────────────────────────────
        if (fieldCapacite != null)
            fieldCapacite.textProperty().addListener((o, old, nw) -> {
                if (nw == null || nw.isBlank()) {
                    applyNormStyle(fieldCapacite);
                    clearError(errCapacite);
                } else if (!isCapaciteValide(nw)) {
                    String msg;
                    try {
                        int v = Integer.parseInt(nw.trim());
                        msg = (v < CAP_MIN)
                                ? "Minimum " + CAP_MIN + " places."
                                : "Maximum " + CAP_MAX + " places.";
                    } catch (NumberFormatException ex) {
                        msg = "Entier requis (entre " + CAP_MIN + " et " + CAP_MAX + ").";
                    }
                    setError(errCapacite, msg, fieldCapacite);
                } else {
                    markValid(errCapacite, fieldCapacite);
                }
            });

        // ── DESCRIPTION ───────────────────────────────────────────
        if (fieldDescription != null)
            fieldDescription.textProperty().addListener((o, old, nw) -> {
                if (nw == null || nw.isBlank()) {
                    applyNormStyle(fieldDescription);
                    clearError(errDescription);
                } else if (!isDescriptionValide(nw)) {
                    String msg;
                    String t = nw.trim();
                    if (t.length() < DESC_MIN)
                        msg = "Minimum " + DESC_MIN + " caractères requis.";
                    else if (t.length() > DESC_MAX)
                        msg = "Maximum " + DESC_MAX + " caractères autorisés.";
                    else
                        msg = "La description doit contenir du texte alphabétique.";
                    setError(errDescription, msg, fieldDescription);
                } else {
                    markValid(errDescription, fieldDescription);
                }
            });

        // ── STATUT (ComboBox) ─────────────────────────────────────
        if (comboStatut != null)
            comboStatut.valueProperty().addListener((o, old, nw) -> {
                if (nw == null || nw.isBlank()) {
                    applyNormStyle(comboStatut);
                    clearError(errStatut);
                } else {
                    markValid(errStatut, comboStatut);
                }
            });

        // ── GROUPE (ComboBox) ─────────────────────────────────────
        if (comboGroupe != null)
            comboGroupe.valueProperty().addListener((o, old, nw) -> {
                if (nw == null || nw.isBlank()) {
                    applyNormStyle(comboGroupe);
                    clearError(errGroupe);
                } else {
                    markValid(errGroupe, comboGroupe);
                }
            });
    }

    // ══════════════════════════════════════════════════════════════
    // VALIDATION COMPLÈTE À LA SOUMISSION
    // ══════════════════════════════════════════════════════════════

    /**
     * Valide tous les champs selon les règles métier.
     * Colore en vert les valides, rouge les invalides.
     * Retourne true uniquement si tout est correct.
     */
    private boolean validateForm() {
        clearErrors();
        boolean ok = true;

        // ── NOM ──────────────────────────────────────────────────
        String nom = fieldNom != null ? fieldNom.getText() : "";
        if (!isNomValide(nom)) {
            String msg;
            if (nom == null || nom.isBlank())
                msg = "Le nom de la session est obligatoire.";
            else {
                String t = nom.trim();
                if (t.length() < NOM_MIN)
                    msg = "Minimum " + NOM_MIN + " caractères alphabétiques requis.";
                else if (t.length() > NOM_MAX)
                    msg = "Maximum " + NOM_MAX + " caractères autorisés.";
                else
                    msg = "Caractères invalides (lettres, chiffres, tirets uniquement).";
            }
            setError(errNom, msg, fieldNom);
            ok = false;
        } else {
            markValid(errNom, fieldNom);
        }

        // ── DATE ─────────────────────────────────────────────────
        LocalDate date = fieldDate != null ? fieldDate.getValue() : null;
        if (date == null) {
            setError(errDate, "La date est obligatoire.", fieldDate);
            ok = false;
        } else if (!isDateValide(date)) {
            setError(errDate, "La date doit être dans le futur.", fieldDate);
            ok = false;
        } else {
            markValid(errDate, fieldDate);
        }

        // ── HEURE ────────────────────────────────────────────────
        String heureText = fieldHeure != null ? fieldHeure.getText().trim() : "";
        if (heureText.isEmpty()) {
            setError(errHeure, "L'heure est obligatoire (format HH:mm).", fieldHeure);
            ok = false;
        } else if (!isHeureValide(heureText)) {
            setError(errHeure, "Format invalide — utilisez HH:mm (ex : 14:30).", fieldHeure);
            ok = false;
        } else {
            markValid(errHeure, fieldHeure);
        }

        // ── STATUT ───────────────────────────────────────────────
        if (comboStatut == null || comboStatut.getValue() == null || comboStatut.getValue().isBlank()) {
            setError(errStatut, "Le statut est obligatoire.", comboStatut);
            ok = false;
        } else {
            markValid(errStatut, comboStatut);
        }

        // ── GROUPE ───────────────────────────────────────────────
        if (comboGroupe == null || comboGroupe.getValue() == null || comboGroupe.getValue().isBlank()) {
            setError(errGroupe, "Le groupe est obligatoire.", comboGroupe);
            ok = false;
        } else {
            markValid(errGroupe, comboGroupe);
        }

        // ── DURÉE ────────────────────────────────────────────────
        String dureeText = fieldDuree != null ? fieldDuree.getText().trim() : "";
        if (dureeText.isEmpty()) {
            setError(errDuree, "La durée est obligatoire.", fieldDuree);
            ok = false;
        } else if (!isDureeValide(dureeText)) {
            String msg;
            try {
                int v = Integer.parseInt(dureeText);
                msg = (v < DUREE_MIN)
                        ? "Minimum " + DUREE_MIN + " minutes."
                        : "Maximum " + DUREE_MAX + " minutes.";
            } catch (NumberFormatException ex) {
                msg = "Entier requis (entre " + DUREE_MIN + " et " + DUREE_MAX + " min).";
            }
            setError(errDuree, msg, fieldDuree);
            ok = false;
        } else {
            markValid(errDuree, fieldDuree);
        }

        // ── PRIX ─────────────────────────────────────────────────
        String prixText = fieldPrix != null ? fieldPrix.getText().trim() : "";
        if (prixText.isEmpty()) {
            setError(errPrix, "Le prix est obligatoire.", fieldPrix);
            ok = false;
        } else if (!isPrixValide(prixText)) {
            String msg;
            try {
                double v = Double.parseDouble(prixText);
                if (v == 0) msg = "Le prix 0 n'est pas autorisé.";
                else if (v < PRIX_MIN) msg = "Minimum " + (int) PRIX_MIN + " TND.";
                else msg = "Maximum " + (int) PRIX_MAX + " TND.";
            } catch (NumberFormatException ex) {
                msg = "Nombre décimal requis (entre " + (int) PRIX_MIN + " et " + (int) PRIX_MAX + " TND).";
            }
            setError(errPrix, msg, fieldPrix);
            ok = false;
        } else {
            markValid(errPrix, fieldPrix);
        }

        // ── CAPACITÉ ─────────────────────────────────────────────
        String capText = fieldCapacite != null ? fieldCapacite.getText().trim() : "";
        if (capText.isEmpty()) {
            setError(errCapacite, "La capacité maximale est obligatoire.", fieldCapacite);
            ok = false;
        } else if (!isCapaciteValide(capText)) {
            String msg;
            try {
                int v = Integer.parseInt(capText);
                msg = (v < CAP_MIN)
                        ? "Minimum " + CAP_MIN + " places."
                        : "Maximum " + CAP_MAX + " places.";
            } catch (NumberFormatException ex) {
                msg = "Entier requis (entre " + CAP_MIN + " et " + CAP_MAX + ").";
            }
            setError(errCapacite, msg, fieldCapacite);
            ok = false;
        } else {
            markValid(errCapacite, fieldCapacite);
        }

        // ── DESCRIPTION ───────────────────────────────────────────
        String desc = fieldDescription != null ? fieldDescription.getText() : "";
        if (!isDescriptionValide(desc)) {
            String msg;
            if (desc == null || desc.isBlank())
                msg = "La description est obligatoire.";
            else {
                String t = desc.trim();
                if (t.length() < DESC_MIN)
                    msg = "Minimum " + DESC_MIN + " caractères requis.";
                else if (t.length() > DESC_MAX)
                    msg = "Maximum " + DESC_MAX + " caractères autorisés.";
                else
                    msg = "La description doit contenir du texte alphabétique.";
            }
            setError(errDescription, msg, fieldDescription);
            ok = false;
        } else {
            markValid(errDescription, fieldDescription);
        }

        return ok;
    }

    // ══════════════════════════════════════════════════════════════
    // RATING AVANCÉ
    // ══════════════════════════════════════════════════════════════

    private VBox buildRatingAvanceBlock(Session s) {
        VBox block = new VBox(6);
        try {
            Object[] stats = sessionservice.getStatistiquesSession(s.getId());
            double scorePondere = (double) stats[0];
            String[] classif = (String[]) stats[1];
            double[] rep = (double[]) stats[2];
            String etoiles = (String) stats[3];

            if (scorePondere > 0) {
                HBox scoreRow = new HBox(8);
                scoreRow.setAlignment(Pos.CENTER_LEFT);
                Label etoilesL = new Label(etoiles);
                etoilesL.setStyle("-fx-font-size:15px;-fx-text-fill:#F59E0B;");
                Label scoreL = new Label(String.format("%.2f/5", scorePondere));
                scoreL.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#374151;");
                scoreRow.getChildren().addAll(etoilesL, scoreL);

                Label classifBadge = new Label(classif[1] + "  " + classif[0]);
                classifBadge.setStyle("-fx-background-color:" + classif[2] +
                        ";-fx-text-fill:" + classif[3] +
                        ";-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-background-radius:20;-fx-padding:3 10 3 10;");
                block.getChildren().addAll(scoreRow, classifBadge);

                if (rep[0] > 0) {
                    int nbPRep = (int) Math.round(rep[0]);
                    String etoilesRep = "★".repeat(Math.max(0, Math.min(5, nbPRep)))
                            + "☆".repeat(Math.max(0, 5 - nbPRep));
                    Label repL = new Label(String.format(
                            "👥 Réputation groupe : %s %.2f/5", etoilesRep, rep[0]));
                    repL.setStyle("-fx-background-color:#EEF2FF;-fx-text-fill:#4338CA;" +
                            "-fx-font-size:10px;-fx-font-weight:bold;" +
                            "-fx-background-radius:20;-fx-padding:3 10 3 10;-fx-wrap-text:true;");
                    repL.setWrapText(true);
                    block.getChildren().add(repL);
                }
            } else {
                Label nonNotee = new Label("☆☆☆☆☆  Pas encore notée");
                nonNotee.setStyle("-fx-background-color:#F8FAFC;-fx-text-fill:#94A3B8;" +
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-background-radius:20;-fx-padding:4 12 4 12;");
                block.getChildren().add(nonNotee);
            }
        } catch (SQLException ignored) {
        }
        return block;
    }

    // ══════════════════════════════════════════════════════════════
    // CARDS
    // ══════════════════════════════════════════════════════════════

    private VBox buildCard(Session s, int ci) {
        String c1 = CARD_COLORS[ci][0], c2 = CARD_COLORS[ci][1];

        int nbResas = 0;
        try {
            nbResas = (int) reservationservice.recupererParSession(s.getId()).stream()
                    .filter(r -> "acceptée".equals(r.getStatut()) || "refusée".equals(r.getStatut()))
                    .count();
        } catch (SQLException ignored) {
        }

        VBox card = new VBox(0);
        card.setPrefWidth(310);
        card.setMaxWidth(310);
        card.setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.13),22,0,0,6);" +
                        "-fx-cursor:hand;"
        );
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.22),30,0,0,10);" +
                        "-fx-scale-x:1.02;-fx-scale-y:1.02;-fx-cursor:hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.13),22,0,0,6);-fx-cursor:hand;"));

        VBox header = new VBox(6);
        header.setPadding(new Insets(20, 20, 16, 20));
        header.setStyle("-fx-background-color:linear-gradient(to bottom right,"
                + c1 + "," + c2 + ");-fx-background-radius:18 18 0 0;");

        HBox htop = new HBox(8);
        htop.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(getStatutIcon(s.getStatut()));
        iconLbl.setStyle("-fx-font-size:18px;-fx-background-color:rgba(255,255,255,0.22);" +
                "-fx-background-radius:50;-fx-padding:6 8 6 8;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label resaBadge = new Label("👥 " + nbResas + " traitée(s)");
        resaBadge.setStyle("-fx-background-color:rgba(255,255,255,0.22);" +
                "-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:bold;" +
                "-fx-background-radius:20;-fx-padding:3 9 3 9;");
        htop.getChildren().addAll(iconLbl, sp, resaBadge);

        String nomAffiche = (s.getNom() != null && !s.getNom().isBlank())
                ? s.getNom() : "Session #" + s.getId();
        Label nomLabel = new Label(nomAffiche);
        nomLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        nomLabel.setWrapText(true);
        Label dateLabel = new Label(s.getDateHeure() != null ? s.getDateHeure().format(FMT_DT) : "—");
        dateLabel.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.85);");
        header.getChildren().addAll(htop, nomLabel, dateLabel);

        VBox body = new VBox(10);
        body.setPadding(new Insets(14, 18, 10, 18));
        String desc = s.getDescription() != null && !s.getDescription().isBlank()
                ? (s.getDescription().length() > 75
                ? s.getDescription().substring(0, 72) + "…"
                : s.getDescription())
                : "Aucune description.";
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;-fx-wrap-text:true;");
        descLabel.setWrapText(true);
        Label statutBadge = buildStatutBadge(s.getStatut());

        HBox infoRow = new HBox(8);
        if (s.getDuree() != null) {
            Label dl = new Label("⏱ " + s.getDuree() + " min");
            dl.setStyle("-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;" +
                    "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
            infoRow.getChildren().add(dl);
        }
        if (s.getPrix() != null) {
            Label pl = new Label("💰 " + String.format("%.0f", s.getPrix()) + " TND");
            pl.setStyle("-fx-background-color:#F0FDF4;-fx-text-fill:#16A34A;" +
                    "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
            infoRow.getChildren().add(pl);
        }
        if (s.getCapaciteMax() != null) {
            Label cl = new Label("🪑 Max " + s.getCapaciteMax());
            cl.setStyle("-fx-background-color:#FFF7ED;-fx-text-fill:#EA580C;" +
                    "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;");
            infoRow.getChildren().add(cl);
        }
        body.getChildren().addAll(descLabel, statutBadge, infoRow);
        body.getChildren().add(buildRatingAvanceBlock(s));

        if (s.getLienReunion() != null && !s.getLienReunion().isBlank()) {
            Label lienL = new Label("🔗 " + (s.getLienReunion().length() > 38
                    ? s.getLienReunion().substring(0, 35) + "…"
                    : s.getLienReunion()));
            lienL.setStyle("-fx-font-size:11px;-fx-text-fill:#6C63FF;-fx-wrap-text:true;");
            lienL.setWrapText(true);
            body.getChildren().add(lienL);
        }

        if ("terminée".equals(s.getStatut())) {
            try {
                if (reservationservice.peutNoterCommeProf(s.getId(), currentProfId)) {
                    Button btnNoter = new Button("⭐  Évaluer cette session");
                    btnNoter.setMaxWidth(Double.MAX_VALUE);
                    btnNoter.setStyle(
                            "-fx-background-color:#FFFBEB;-fx-text-fill:#D97706;" +
                                    "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;" +
                                    "-fx-padding:6 10 6 10;-fx-cursor:hand;");
                    btnNoter.setOnAction(e -> showRatingDialogProf(s));
                    body.getChildren().add(btnNoter);
                }
            } catch (SQLException ignored) {
            }
        }

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#F1F5F9;");
        VBox.setMargin(sep, new Insets(4, 0, 0, 0));

        HBox row1 = new HBox(6);
        row1.setAlignment(Pos.CENTER);
        row1.setPadding(new Insets(10, 12, 4, 12));

        Button btnVoir = makeBtn("👁 Détails", "#EFF6FF", "#1D4ED8");
        HBox.setHgrow(btnVoir, Priority.ALWAYS);
        btnVoir.setMaxWidth(Double.MAX_VALUE);
        btnVoir.setOnAction(e -> showDetails(s));

        Button btnEdit = makeBtn("✏ Modifier", c1, "#FFFFFF");
        HBox.setHgrow(btnEdit, Priority.ALWAYS);
        btnEdit.setMaxWidth(Double.MAX_VALUE);
        btnEdit.setOnAction(e -> openEditForm(s));

        Button btnDel = makeBtn("🗑", "#FFF1F2", "#DC2626");
        btnDel.setPrefWidth(42);
        btnDel.setMinWidth(42);
        btnDel.setOnAction(e -> handleDelete(s));

        row1.getChildren().addAll(btnVoir, btnEdit, btnDel);

        Button btnMeet;
        if (s.getLienReunion() != null && !s.getLienReunion().isBlank()) {
            btnMeet = makeBtn("🎥  Rejoindre Jitsi", "#EEF2FF", "#4F46E5");
            btnMeet.setOnAction(e -> JitsiUtil.ouvrirDansAppDesktop(s.getLienReunion()));
        } else {
            btnMeet = makeBtn("🔗  Aucun lien", "#F8FAFC", "#94A3B8");
            btnMeet.setDisable(true);
        }
        btnMeet.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnMeet, Priority.ALWAYS);
        HBox rowJitsi = new HBox(btnMeet);
        rowJitsi.setPadding(new Insets(2, 12, 4, 12));

        int nbEnAttente = 0;
        try {
            nbEnAttente = (int) reservationservice.recupererParSession(s.getId()).stream()
                    .filter(r -> "en attente".equals(r.getStatut())).count();
        } catch (SQLException ignored) {
        }

        String labelBtn = nbEnAttente > 0
                ? "📋  Gérer réservations  ⏳ " + nbEnAttente + " en attente"
                : "📋  Gérer les réservations";
        Button btnResas = new Button(labelBtn);
        btnResas.setMaxWidth(Double.MAX_VALUE);
        btnResas.setStyle(
                "-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");" +
                        "-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;" +
                        "-fx-background-radius:10;-fx-padding:11 0 11 0;-fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),8,0,0,3);");
        btnResas.setOnMouseEntered(e -> btnResas.setOpacity(0.88));
        btnResas.setOnMouseExited(e -> btnResas.setOpacity(1.0));
        btnResas.setOnAction(e -> showReservationsDialog(s, c1, c2));

        VBox actions = new VBox(6);
        actions.setPadding(new Insets(8, 12, 14, 12));
        actions.getChildren().addAll(row1, rowJitsi, btnResas);
        card.getChildren().addAll(header, body, sep, actions);
        return card;
    }

    // ══════════════════════════════════════════════════════════════
    // DÉTAILS SESSION
    // ══════════════════════════════════════════════════════════════

    private void showDetails(Session s) {
        int ci = (int) (s.getId() % CARD_COLORS.length);
        String c1 = CARD_COLORS[ci][0], c2 = CARD_COLORS[ci][1];

        Dialog<Void> dialog = new Dialog<>();
        String nomAffiche = (s.getNom() != null && !s.getNom().isBlank())
                ? s.getNom() : "Session #" + s.getId();
        dialog.setTitle("Détails — " + nomAffiche);
        dialog.setHeaderText(null);

        VBox content = new VBox(0);
        content.setPrefWidth(500);

        VBox header = new VBox(8);
        header.setPadding(new Insets(24, 28, 18, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");");
        Label nomLbl = new Label(nomAffiche);
        nomLbl.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        nomLbl.setWrapText(true);

        HBox badgeRow = new HBox(8);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        Label statutBadge = new Label(getStatutIcon(s.getStatut()) + "  " +
                (s.getStatut() != null ? s.getStatut().toUpperCase() : "—"));
        statutBadge.setStyle("-fx-background-color:rgba(255,255,255,0.22);" +
                "-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-background-radius:20;-fx-padding:4 14 4 14;");
        Label dateBadge = new Label("📅 " + (s.getDateHeure() != null
                ? s.getDateHeure().format(FMT_DT) : "Date non définie"));
        dateBadge.setStyle("-fx-background-color:rgba(255,255,255,0.18);" +
                "-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;" +
                "-fx-background-radius:20;-fx-padding:4 14 4 14;");
        badgeRow.getChildren().addAll(statutBadge, dateBadge);
        header.getChildren().addAll(nomLbl, badgeRow);

        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 28, 22, 28));

        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(12);
        grid.setStyle("-fx-background-color:#F8F9FD;-fx-background-radius:14;-fx-padding:18;");
        ColumnConstraints cc1 = new ColumnConstraints(140);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc1, cc2);

        String ls = "-fx-font-size:11px;-fx-text-fill:#94A3B8;-fx-font-weight:bold;";
        String vs = "-fx-font-size:13px;-fx-text-fill:#1F2937;-fx-font-weight:bold;";
        int row = 0;
        addGridRow(grid, row++, "🏷  Nom", nomAffiche, ls, vs);
        addGridRow(grid, row++, "📌  Statut", s.getStatut() != null ? s.getStatut() : "—", ls, vs);
        addGridRow(grid, row++, "📅  Date / Heure", s.getDateHeure() != null ? s.getDateHeure().format(FMT_DT) : "—", ls, vs);
        addGridRow(grid, row++, "⏱  Durée", s.getDuree() != null ? s.getDuree() + " min" : "—", ls, vs);
        addGridRow(grid, row++, "💰  Prix", s.getPrix() != null ? String.format("%.2f TND", s.getPrix()) : "Gratuit", ls, vs);
        addGridRow(grid, row++, "🪑  Capacité max", s.getCapaciteMax() != null ? String.valueOf(s.getCapaciteMax()) : "Illimitée", ls, vs);
        addGridRow(grid, row++, "🔗  Lien réunion", s.getLienReunion() != null && !s.getLienReunion().isBlank()
                ? s.getLienReunion() : "Aucun lien", ls, vs);

        try {
            Object[] stats = sessionservice.getStatistiquesSession(s.getId());
            double scorePondere = (double) stats[0];
            String[] classif = (String[]) stats[1];
            double[] rep = (double[]) stats[2];
            String etoiles = (String) stats[3];
            if (scorePondere > 0) {
                addGridRow(grid, row++, "⭐  Note pondérée",
                        String.format("%.2f/5 — %s %s", scorePondere, classif[1], classif[0]), ls, vs);
                addGridRow(grid, row++, "🌟  Étoiles", etoiles, ls,
                        "-fx-font-size:18px;-fx-text-fill:#F59E0B;");
            } else {
                addGridRow(grid, row++, "⭐  Note", "Pas encore notée", ls, vs);
            }
            if (rep[0] > 0) {
                int nbP = (int) Math.round(rep[0]);
                String etoilesRep = "★".repeat(Math.max(0, Math.min(5, nbP)))
                        + "☆".repeat(Math.max(0, 5 - nbP));
                addGridRow(grid, row, "👥  Réputation groupe",
                        String.format("%s %.2f/5 (%d/%d sessions notées)",
                                etoilesRep, rep[0], (int) rep[2], (int) rep[1]), ls, vs);
            }
        } catch (SQLException ignored) {
        }

        body.getChildren().add(grid);

        if (s.getDescription() != null && !s.getDescription().isBlank()) {
            VBox descBox = new VBox(6);
            descBox.setStyle("-fx-background-color:#F8FAFC;-fx-background-radius:12;" +
                    "-fx-padding:14 16 14 16;-fx-border-color:#E5E7EB;" +
                    "-fx-border-radius:12;-fx-border-width:1;");
            Label descTitle = new Label("📝  Description");
            descTitle.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#6B7280;");
            TextArea da = new TextArea(s.getDescription());
            da.setEditable(false);
            da.setWrapText(true);
            da.setPrefHeight(80);
            da.setStyle("-fx-background-color:transparent;-fx-border-color:transparent;" +
                    "-fx-font-size:13px;-fx-text-fill:#374151;");
            descBox.getChildren().addAll(descTitle, da);
            body.getChildren().add(descBox);
        }

        if (s.getLienReunion() != null && !s.getLienReunion().isBlank()) {
            Button btnJitsi = new Button("🎥  Rejoindre la réunion Jitsi");
            btnJitsi.setMaxWidth(Double.MAX_VALUE);
            btnJitsi.setStyle("-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");" +
                    "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;" +
                    "-fx-background-radius:10;-fx-padding:11 0 11 0;-fx-cursor:hand;");
            btnJitsi.setOnAction(e -> JitsiUtil.ouvrirDansAppDesktop(s.getLienReunion()));
            body.getChildren().add(btnJitsi);
        }

        content.getChildren().addAll(header, body);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color:#FFFFFF;-fx-background:#FFFFFF;-fx-border-color:transparent;");
        scrollPane.setMaxHeight(580);
        scrollPane.setPrefHeight(520);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefWidth(540);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button closeBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeBtn.setText("Fermer");
        closeBtn.setStyle("-fx-background-color:" + c1 + ";-fx-text-fill:white;" +
                "-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-padding:10 28 10 28;-fx-cursor:hand;");
        dialog.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════
    // GESTION RÉSERVATIONS
    // ══════════════════════════════════════════════════════════════

    private void showReservationsDialog(Session s, String c1, String c2) {
        Dialog<Void> dialog = new Dialog<>();
        String nomAffiche = (s.getNom() != null && !s.getNom().isBlank())
                ? s.getNom() : "Session #" + s.getId();
        dialog.setTitle("Réservations — " + nomAffiche);
        dialog.setHeaderText(null);

        VBox mainContent = new VBox(0);
        mainContent.setPrefWidth(540);
        VBox header = new VBox(8);
        header.setPadding(new Insets(20, 26, 16, 26));
        header.setStyle("-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");");
        Label titleLbl = new Label("📋  Réservations — " + nomAffiche);
        titleLbl.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        titleLbl.setWrapText(true);
        header.getChildren().add(titleLbl);

        VBox body = new VBox(10);
        body.setPadding(new Insets(18, 26, 22, 26));
        try {
            List<Reservation> resas = reservationservice.recupererParSession(s.getId());
            if (resas.isEmpty()) {
                Label empty = new Label("📭  Aucune réservation pour cette session.");
                empty.setStyle("-fx-font-size:14px;-fx-text-fill:#94A3B8;-fx-padding:20 0 20 0;");
                body.getChildren().add(empty);
            } else {
                for (Reservation r : resas)
                    body.getChildren().add(buildResaRow(r, true, dialog, s));
            }
        } catch (SQLException e) {
            Label err = new Label("❌  Erreur : " + e.getMessage());
            err.setStyle("-fx-font-size:12px;-fx-text-fill:#E11D48;");
            body.getChildren().add(err);
        }

        mainContent.getChildren().addAll(header, body);
        ScrollPane sp = new ScrollPane(mainContent);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle("-fx-background-color:#FFFFFF;-fx-background:#FFFFFF;-fx-border-color:transparent;");
        sp.setMaxHeight(520);
        sp.setPrefHeight(460);

        dialog.getDialogPane().setContent(sp);
        dialog.getDialogPane().setPrefWidth(580);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button closeBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeBtn.setText("Fermer");
        closeBtn.setStyle("-fx-background-color:" + c1 + ";-fx-text-fill:white;" +
                "-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-padding:10 28 10 28;-fx-cursor:hand;");
        dialog.showAndWait();
    }

    private HBox buildResaRow(Reservation r, boolean avecActions, Dialog<?> dialog, Session s) {
        String bg = getResaBg(r.getStatut());
        String fg = getResaFg(r.getStatut());
        String ico = getResaIcon(r.getStatut());

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 14, 12, 14));
        row.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:12;" +
                "-fx-border-color:#E2E8F0;-fx-border-radius:12;-fx-border-width:1;");

        Label statusIco = new Label(ico);
        statusIco.setStyle("-fx-font-size:18px;");
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label idLbl = new Label("Réservation #" + r.getId() + "  —  " + r.getStatut().toUpperCase());
        idLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + fg + ";");
        Label dateLbl = new Label("📅 " + r.getDateReservation().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dateLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#64748B;");
        info.getChildren().addAll(idLbl, dateLbl);
        row.getChildren().addAll(statusIco, info);

        if (avecActions && "en attente".equals(r.getStatut())) {
            Button btnAcc = new Button("✅ Accepter");
            btnAcc.setStyle("-fx-background-color:#ECFDF5;-fx-text-fill:#059669;" +
                    "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;" +
                    "-fx-padding:6 12 6 12;-fx-cursor:hand;");
            btnAcc.setOnAction(e -> {
                try {
                    reservationservice.mettreAJourStatut(r.getId(), "acceptée");
                    showReservationsDialog(s,
                            CARD_COLORS[(int) (s.getId() % CARD_COLORS.length)][0],
                            CARD_COLORS[(int) (s.getId() % CARD_COLORS.length)][1]);
                    if (dialog != null) dialog.close();
                } catch (SQLException ex) {
                    showStyledAlert("error", "Erreur", ex.getMessage());
                }
            });
            Button btnRef = new Button("❌ Refuser");
            btnRef.setStyle("-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;" +
                    "-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;" +
                    "-fx-padding:6 12 6 12;-fx-cursor:hand;");
            btnRef.setOnAction(e -> {
                try {
                    reservationservice.mettreAJourStatut(r.getId(), "refusée");
                    showReservationsDialog(s,
                            CARD_COLORS[(int) (s.getId() % CARD_COLORS.length)][0],
                            CARD_COLORS[(int) (s.getId() % CARD_COLORS.length)][1]);
                    if (dialog != null) dialog.close();
                } catch (SQLException ex) {
                    showStyledAlert("error", "Erreur", ex.getMessage());
                }
            });
            row.getChildren().addAll(btnAcc, btnRef);
        }
        return row;
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS DIVERS
    // ══════════════════════════════════════════════════════════════

    private String getResaIcon(String s) {
        return switch (s != null ? s : "") {
            case "en attente" -> "⏳";
            case "acceptée" -> "✅";
            case "refusée" -> "❌";
            case "annulée" -> "🚫";
            default -> "📋";
        };
    }

    private String getResaBg(String s) {
        return switch (s != null ? s : "") {
            case "en attente" -> "#FFFBEB";
            case "acceptée" -> "#ECFDF5";
            case "refusée" -> "#FFF1F2";
            case "annulée" -> "#F8FAFC";
            default -> "#F1F5F9";
        };
    }

    private String getResaFg(String s) {
        return switch (s != null ? s : "") {
            case "en attente" -> "#D97706";
            case "acceptée" -> "#059669";
            case "refusée" -> "#E11D48";
            case "annulée" -> "#64748B";
            default -> "#475569";
        };
    }

    private void showStyledAlert(String type, String title, String message) {
        Alert.AlertType t = switch (type) {
            case "success" -> Alert.AlertType.INFORMATION;
            case "error" -> Alert.AlertType.ERROR;
            case "warning" -> Alert.AlertType.WARNING;
            default -> Alert.AlertType.INFORMATION;
        };
        Alert a = new Alert(t);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private void addGridRow(GridPane g, int row, String l, String v, String ls, String vs) {
        Label ll = new Label(l);
        ll.setStyle(ls);
        Label vv = new Label(v);
        vv.setStyle(vs);
        vv.setWrapText(true);
        g.add(ll, 0, row);
        g.add(vv, 1, row);
    }

    // ══════════════════════════════════════════════════════════════
    // FORMULAIRE — OUVRIR / FERMER
    // ══════════════════════════════════════════════════════════════

    private void openEditForm(Session s) {
        selectedSession = s;
        clearErrors();
        fieldNom.setText(s.getNom() != null ? s.getNom() : "");
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
                .map(Map.Entry::getKey)
                .findFirst()
                .ifPresent(comboGroupe::setValue);
        if (formTitle != null) formTitle.setText("Modifier la Session");
        if (formTitleIcon != null) formTitleIcon.setText("✎");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    private void handleDelete(Session s) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        String nomAffiche = (s.getNom() != null && !s.getNom().isBlank())
                ? s.getNom()
                : "session du " + (s.getDateHeure() != null ? s.getDateHeure().format(FMT_DT) : "?");
        confirm.setContentText("Supprimer la session « " + nomAffiche + " » ?\n\n" +
                "⚠ Toutes les réservations associées seront supprimées.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    sessionservice.supprimer(s.getId());
                    loadData();
                    showStyledAlert("success", "Supprimée ✅", "Session supprimée avec succès.");
                } catch (SQLException e) {
                    showStyledAlert("error", "Erreur", e.getMessage());
                }
            }
        });
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
        if (comboGroupe != null)
            comboGroupe.setItems(FXCollections.observableArrayList(groupeMap.keySet()));
    }

    // ══════════════════════════════════════════════════════════════
    // CYCLE DE VIE
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        if (comboStatut != null)
            comboStatut.setItems(FXCollections.observableArrayList(STATUTS));
        if (filterStatut != null) {
            filterStatut.setItems(FXCollections.observableArrayList(
                    "Tous", "planifiée", "en cours", "terminée", "annulée"));
            filterStatut.setValue("Tous");
            filterStatut.setOnAction(e -> handleSearch());
        }
        loadGroupes();
        setupLiveValidation();
    }

    public void setProfId(int id) {
        this.currentProfId = id;
        loadData();

        javafx.application.Platform.runLater(() -> {
            if (rootPane != null) {
                notifBell = new NotificationBell(id, true, rootPane);
                if (bellContainer != null)
                    bellContainer.getChildren().setAll(notifBell.getBellRoot());
                ReminderScheduler.getInstance().start(id, true, rootPane, notifBell);
            }
        });
    }
    private void loadData() {
        try {
            List<Session> sessions = sessionservice.recuperer();
            allData = FXCollections.observableArrayList(sessions);
            applyFilter();
        } catch (SQLException e) {
            showStyledAlert("error", "Erreur de chargement", e.getMessage());
        }
    }

    private void renderCards(List<Session> list) {
        cardsContainer.getChildren().clear();
        int i = 0;
        for (Session s : list)
            cardsContainer.getChildren().add(buildCard(s, i++ % CARD_COLORS.length));
        if (list.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            Label icon = new Label("📭");
            icon.setStyle("-fx-font-size:48px;");
            Label msg = new Label("Aucune session trouvée");
            msg.setStyle("-fx-font-size:15px;-fx-text-fill:#9CA3AF;");
            empty.getChildren().addAll(icon, msg);
            cardsContainer.getChildren().add(empty);
        }
    }

    private void applyFilter() {
        String q = searchField != null ? searchField.getText().toLowerCase().trim() : "";
        String statut = filterStatut != null ? filterStatut.getValue() : "Tous";
        List<Session> filtered = allData.stream().filter(s -> {
            boolean mq = q.isEmpty()
                    || (s.getDescription() != null && s.getDescription().toLowerCase().contains(q))
                    || (s.getStatut() != null && s.getStatut().toLowerCase().contains(q))
                    || (s.getLienReunion() != null && s.getLienReunion().toLowerCase().contains(q))
                    || (s.getNom() != null && s.getNom().toLowerCase().contains(q));
            boolean ms = "Tous".equals(statut) || statut.equals(s.getStatut());
            return mq && ms;
        }).collect(Collectors.toList());
        renderCards(filtered);
        updateCount(filtered.size());
    }

    @FXML
    private void handleAjouter() {
        selectedSession = null;
        clearErrors();
        fieldNom.setText("");
        fieldDate.setValue(null);
        fieldHeure.setText("");
        fieldLien.setText("");
        fieldDuree.setText("");
        fieldPrix.setText("");
        fieldCapacite.setText("");
        comboStatut.setValue(null);
        comboGroupe.setValue(null);
        fieldDescription.setText("");
        if (formTitle != null) formTitle.setText("Nouvelle Session");
        if (formTitleIcon != null) formTitleIcon.setText("✚");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    @FXML
    private void handleCancel() {
        formCard.setVisible(false);
        formCard.setManaged(false);
        selectedSession = null;
        clearErrors();
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;

        try {
            LocalDate date = fieldDate.getValue();
            LocalTime heure = LocalTime.parse(fieldHeure.getText().trim(), FMT_H);
            LocalDateTime dt = LocalDateTime.of(date, heure);

            Integer groupeId = groupeMap.get(comboGroupe.getValue());
            if (groupeId == null) {
                showStyledAlert("error", "Groupe introuvable",
                        "Le groupe sélectionné est introuvable.");
                return;
            }

            Integer duree = Integer.parseInt(fieldDuree.getText().trim());
            Double prix = Double.parseDouble(fieldPrix.getText().trim());
            Integer capacite = Integer.parseInt(fieldCapacite.getText().trim());
            String lien = fieldLien.getText().trim();

            Session s = new Session(
                    dt,
                    comboStatut.getValue(),
                    lien,
                    groupeId,
                    currentProfId,
                    duree,
                    prix,
                    fieldDescription.getText().trim(),
                    capacite,
                    fieldNom.getText().trim()
            );

            if (selectedSession != null) {
                s.setId(selectedSession.getId());
                if (lien.isEmpty() && selectedSession.getLienReunion() != null)
                    s.setLienReunion(selectedSession.getLienReunion());
                sessionservice.modifier(s);
                showStyledAlert("success", "Session modifiée ✅",
                        "La session a été modifiée avec succès.");
            } else {
                sessionservice.ajouter(s);
                int newId = sessionservice.getLastInsertId();
                if (newId > 0) {
                    String jitsiLink = JitsiUtil.genererLienJitsi(newId);
                    sessionservice.mettreAJourLienReunion(newId, jitsiLink);
                    s.setId(newId);
                    s.setLienReunion(jitsiLink);
                }
                showStyledAlert("success", "Session créée ✅",
                        "Session créée avec succès.\n🔗 Lien Jitsi généré automatiquement.");
            }

            handleCancel();
            loadData();

        } catch (DateTimeParseException e) {
            setError(errHeure, "Format d'heure invalide (HH:mm).", fieldHeure);
        } catch (NumberFormatException e) {
            showStyledAlert("error", "Erreur de saisie",
                    "Vérifiez les champs numériques (durée, prix, capacité).");
        } catch (Exception e) {
            showStyledAlert("error", "Erreur inattendue", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // NOTATION PROF
    // ══════════════════════════════════════════════════════════════

    private void showRatingDialogProf(Session s) {
        int ci = (int) (s.getId() % CARD_COLORS.length);
        String c1 = CARD_COLORS[ci][0], c2 = CARD_COLORS[ci][1];

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Évaluer la session");
        dialog.setHeaderText(null);

        VBox content = new VBox(0);
        content.setPrefWidth(420);

        VBox header = new VBox(8);
        header.setPadding(new Insets(20, 24, 16, 24));
        header.setStyle("-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");");
        String titreSession = (s.getNom() != null && !s.getNom().isBlank())
                ? s.getNom() : "Session #" + s.getId();
        Label tl = new Label("⭐  Évaluer — " + titreSession);
        tl.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        tl.setWrapText(true);
        Label sub = new Label("Votre avis en tant que professeur");
        sub.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.85);");
        header.getChildren().addAll(tl, sub);

        VBox body = new VBox(14);
        body.setPadding(new Insets(18, 24, 22, 24));
        Label instruc = new Label("Note globale de la session (1 à 5) :");
        instruc.setStyle("-fx-font-size:13px;-fx-text-fill:#374151;-fx-font-weight:bold;");

        StarRatingComponent starComp = new StarRatingComponent();
        Label feedbackLabel = new Label("Sélectionne une note...");
        feedbackLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#94A3B8;-fx-font-style:italic;");

        for (int i = 0; i < 5; i++) {
            final int noteVal = i + 1;
            starComp.getChildren().get(i).setOnMouseClicked(e -> {
                starComp.setRating(noteVal);
                try {
                    int nbAcc = sessionservice.compterReservationsAcceptees(s.getId());
                    double scorePreview = sessionservice.calculerScorePondere(
                            noteVal, nbAcc, s.getCapaciteMax());
                    String[] classif = sessionservice.getClassificationQualite(scorePreview);
                    feedbackLabel.setText(classif[1] + " " + classif[0]
                            + String.format(" — Score pondéré : %.2f/5", scorePreview));
                    feedbackLabel.setStyle("-fx-font-size:12px;-fx-font-weight:bold;" +
                            "-fx-text-fill:" + classif[3] + ";-fx-background-color:" + classif[2] +
                            ";-fx-background-radius:8;-fx-padding:6 10 6 10;");
                } catch (SQLException ex) { /* ignored */ }
            });
        }
        body.getChildren().addAll(instruc, starComp, feedbackLabel);
        content.getChildren().addAll(header, body);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");

        ButtonType btnValider = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnValider, ButtonType.CANCEL);
        Button valBtn = (Button) dialog.getDialogPane().lookupButton(btnValider);
        valBtn.setStyle("-fx-background-color:linear-gradient(to right," + c1 + "," + c2 + ");" +
                "-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-padding:8 20 8 20;");

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == btnValider) {
                int rating = starComp.getRating();
                if (rating == 0) {
                    showStyledAlert("warning", "Note manquante",
                            "Sélectionne au moins 1 étoile !");
                    return;
                }
                try {
                    sessionservice.enregistrerRatingAvance(s.getId(), rating);
                    showStyledAlert("success", "Note enregistrée !",
                            "Session évaluée avec " + rating + " étoile(s).");
                    loadData();
                } catch (SQLException ex) {
                    showStyledAlert("error", "Erreur", ex.getMessage());
                }
            }
        });
    }
    // ── Ouvrir le calendrier prof dans une nouvelle fenêtre ───────
    @FXML
    private void handleOuvrirCalendrier() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/pijava_fluently/fxml/calendrier.fxml"));
            javafx.scene.Parent vue = loader.load();
            CalendrierController ctrl = loader.getController();
            ctrl.setUserId(currentProfId, true); // true = professeur

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("📅 Mon Calendrier — Professeur");
            stage.setScene(new javafx.scene.Scene(vue, 1100, 700));
            stage.initModality(javafx.stage.Modality.NONE);
            stage.show();
            System.out.println("✅ Calendrier prof ouvert");
        } catch (Exception e) {
            showStyledAlert("error", "Erreur",
                    "Impossible d'ouvrir le calendrier : " + e.getMessage());
        }
    }
}