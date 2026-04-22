package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Objectif;
import com.example.pijava_fluently.entites.Tache;
import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.ObjectifService;
import com.example.pijava_fluently.services.SpellCheckerService;
import com.example.pijava_fluently.services.TacheService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TacheController {

    // ── FXML Header ──────────────────────────────────────────────
    @FXML private Label  pageTitle;
    @FXML private Label  pageSubtitle;
    @FXML private Label  countLabel;

    // ── FXML Formulaire ──────────────────────────────────────────
    @FXML private VBox      formCard;
    @FXML private Label     formTitle;
    @FXML private Label     formTitleIcon;
    @FXML private TextField fieldTitre;
    @FXML private TextArea  fieldDescription;
    @FXML private DatePicker fieldDateLimite;
    @FXML private ComboBox<String> comboStatut;
    @FXML private ComboBox<String> comboPriorite;
    @FXML private ComboBox<String> comboLangue;
    @FXML private Label errTitre;
    @FXML private Label errDescription;
    @FXML private Label errDateLimite;
    @FXML private Label errStatut;
    @FXML private Label errPriorite;

    // ── FXML Recherche ───────────────────────────────────────────
    @FXML private TextField searchField;

    // ── FXML Colonnes Kanban ─────────────────────────────────────
    @FXML private VBox colAFaire;
    @FXML private VBox colEnCours;
    @FXML private VBox colTerminee;
    @FXML private VBox colAnnulee;

    // Compteurs de colonnes
    @FXML private Label cntAFaire;
    @FXML private Label cntEnCours;
    @FXML private Label cntTerminee;
    @FXML private Label cntAnnulee;

    // Bouton Ajouter
    @FXML private Button btnAjouter;

    // ── Menus contextuels pour suggestions ───────────────────────
    private ContextMenu titreSuggestionsMenu;
    private ContextMenu descriptionSuggestionsMenu;

    // Timer pour debounce
    private Timeline titreDebounceTimer;
    private Timeline descDebounceTimer;

    // ── Services & État ──────────────────────────────────────────
    private final TacheService    service         = new TacheService();
    private final ObjectifService objectifService = new ObjectifService();
    private SpellCheckerService   spellChecker;
    private ObservableList<Tache> allData         = FXCollections.observableArrayList();
    private Tache                 selectedTache   = null;
    private Objectif              currentObjectif = null;
    private ObjectifController    objectifController;
    private User                  currentUser;

    // Map pour stocker les titres des objectifs
    private Map<Integer, String> objectifTitreMap = new HashMap<>();

    // La tâche en cours de glissement
    private Tache tacheEnGlissement = null;

    // ── Constantes ───────────────────────────────────────────────
    private static final String[] STATUTS   = {"À faire", "En cours", "Terminée", "Annulée"};
    private static final String[] PRIORITES = {"Basse", "Normale", "Haute", "Urgente"};
    private static final String[] LANGUES   = {"Français", "Anglais", "Espagnol", "Allemand", "Italien", "Portugais"};
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String ERROR_STYLE      = "-fx-border-color:#E11D48;-fx-border-width:2;-fx-border-radius:10;";
    private static final String VALID_STYLE       = "-fx-border-color:#10B981;-fx-border-width:2;-fx-border-radius:10;";
    private static final String NORMAL_STYLE      = "-fx-border-color:#E2E8F0;-fx-border-width:1.5;-fx-border-radius:10;";
    private static final String AREA_ERROR_STYLE  = "-fx-border-color:#E11D48;-fx-border-width:2;-fx-border-radius:10;";
    private static final String AREA_VALID_STYLE  = "-fx-border-color:#10B981;-fx-border-width:2;-fx-border-radius:10;";
    private static final String AREA_NORMAL_STYLE = "-fx-border-color:#E2E8F0;-fx-border-width:1.5;-fx-border-radius:10;";

    // ════════════════════════════════════════════════════════════
    //  SETTERS
    // ════════════════════════════════════════════════════════════

    public void setObjectif(Objectif o) {
        this.currentObjectif = o;
        if (pageTitle != null) pageTitle.setText("📋  Tâches — " + o.getTitre());
        if (pageSubtitle != null) pageSubtitle.setText("Kanban — Glissez vos tâches pour changer leur statut");
        loadObjectifsTitres();
        if (currentUser != null) {
            updateAddButtonVisibility();
            loadData();
        }
    }

    public void setObjectifController(ObjectifController oc) {
        this.objectifController = oc;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        updateAddButtonVisibility();
        if (currentObjectif != null) {
            loadData();
        }
    }

    private void updateAddButtonVisibility() {
        Platform.runLater(() -> {
            if (btnAjouter != null) {
                boolean visible = isOwner();
                btnAjouter.setVisible(visible);
                btnAjouter.setManaged(visible);
            }
        });
    }

    private void loadObjectifsTitres() {
        objectifTitreMap.clear();
        try {
            List<Objectif> objectifs = objectifService.recuperer();
            for (Objectif o : objectifs) {
                objectifTitreMap.put(o.getId(), o.getTitre());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getObjectifTitre(int id) {
        return objectifTitreMap.getOrDefault(id, "Objectif #" + id);
    }

    // ════════════════════════════════════════════════════════════
    //  INITIALISATION JavaFX
    // ════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        comboStatut.setItems(FXCollections.observableArrayList(STATUTS));
        comboPriorite.setItems(FXCollections.observableArrayList(PRIORITES));
        comboLangue.setItems(FXCollections.observableArrayList(LANGUES));
        comboLangue.setValue("Français");

        spellChecker = new SpellCheckerService();

        setupLiveValidation();
        setupAutoCorrection();
    }

    // ════════════════════════════════════════════════════════════
    //  VALIDATION EN TEMPS RÉEL (CONTRÔLE DE SAISIE)
    // ════════════════════════════════════════════════════════════

    private void setupLiveValidation() {
        // Validation du titre en temps réel
        fieldTitre.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.trim().isEmpty()) {
                validateTitre(val.trim());
            } else if (val != null && val.trim().isEmpty()) {
                setError(errTitre, "Le titre est obligatoire", fieldTitre);
            }
        });

        // Validation de la description en temps réel
        fieldDescription.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.trim().isEmpty()) {
                validateDescription(val.trim());
            } else if (val != null && val.trim().isEmpty()) {
                setError(errDescription, "La description est obligatoire", fieldDescription);
            }
        });

        // Validation de la date limite en temps réel
        fieldDateLimite.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                validateDate(val);
            } else {
                setError(errDateLimite, "La date limite est obligatoire", fieldDateLimite);
            }
        });

        // Validation du statut
        comboStatut.valueProperty().addListener((obs, old, val) -> {
            if (val != null && !val.isEmpty()) {
                clearError(errStatut);
                comboStatut.setStyle(NORMAL_STYLE);
            }
        });

        // Validation de la priorité
        comboPriorite.valueProperty().addListener((obs, old, val) -> {
            if (val != null && !val.isEmpty()) {
                clearError(errPriorite);
                comboPriorite.setStyle(NORMAL_STYLE);
            }
        });
    }

    private boolean validateTitre(String titre) {
        if (titre.isEmpty()) {
            setError(errTitre, "Le titre est obligatoire", fieldTitre);
            return false;
        } else if (titre.length() < 3) {
            setError(errTitre, "Minimum 3 caractères", fieldTitre);
            return false;
        } else if (titre.length() > 50) {
            setError(errTitre, "Maximum 50 caractères", fieldTitre);
            return false;
        } else {
            clearError(errTitre);
            setValidStyle(fieldTitre);
            return true;
        }
    }

    private boolean validateDescription(String desc) {
        if (desc.isEmpty()) {
            setError(errDescription, "La description est obligatoire", fieldDescription);
            return false;
        } else if (desc.length() > 255) {
            setError(errDescription, "Maximum 255 caractères", fieldDescription);
            return false;
        } else {
            clearError(errDescription);
            setValidStyle(fieldDescription);
            return true;
        }
    }

    private boolean validateDate(LocalDate date) {
        if (date == null) {
            setError(errDateLimite, "La date limite est obligatoire", fieldDateLimite);
            return false;
        } else if (date.isBefore(LocalDate.now())) {
            setError(errDateLimite, "La date limite ne peut pas être dans le passé", fieldDateLimite);
            return false;
        } else {
            clearError(errDateLimite);
            setValidStyle(fieldDateLimite);
            return true;
        }
    }

    private void setError(Label lbl, String msg, Control control) {
        if (lbl == null) return;
        lbl.setText("⚠ " + msg);
        lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#E11D48;-fx-font-weight:bold;");
        lbl.setVisible(true);
        lbl.setManaged(true);

        if (control != null) {
            String currentStyle = control.getStyle();
            if (control instanceof TextArea) {
                currentStyle = currentStyle.replace(AREA_VALID_STYLE, "").replace(AREA_NORMAL_STYLE, "");
                control.setStyle(currentStyle + AREA_ERROR_STYLE);
            } else {
                currentStyle = currentStyle.replace(VALID_STYLE, "").replace(NORMAL_STYLE, "");
                control.setStyle(currentStyle + ERROR_STYLE);
            }
        }
    }

    private void clearError(Label lbl) {
        if (lbl == null) return;
        lbl.setText("");
        lbl.setVisible(false);
        lbl.setManaged(false);
    }

    private void setValidStyle(Control control) {
        if (control == null) return;
        String currentStyle = control.getStyle();
        if (control instanceof TextArea) {
            currentStyle = currentStyle.replace(AREA_ERROR_STYLE, "").replace(AREA_NORMAL_STYLE, "");
            control.setStyle(currentStyle + AREA_VALID_STYLE);
        } else {
            currentStyle = currentStyle.replace(ERROR_STYLE, "").replace(NORMAL_STYLE, "");
            control.setStyle(currentStyle + VALID_STYLE);
        }

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(2), e -> {
                    String style = control.getStyle();
                    if (control instanceof TextArea) {
                        style = style.replace(AREA_VALID_STYLE, AREA_NORMAL_STYLE);
                    } else {
                        style = style.replace(VALID_STYLE, NORMAL_STYLE);
                    }
                    control.setStyle(style);
                })
        );
        timeline.setCycleCount(1);
        timeline.play();
    }

    private void clearErrors() {
        Label[] labels = {errTitre, errDescription, errDateLimite, errStatut, errPriorite};
        Control[] controls = {fieldTitre, fieldDescription, fieldDateLimite, comboStatut, comboPriorite};

        for (Label l : labels) {
            if (l != null) {
                l.setText("");
                l.setVisible(false);
                l.setManaged(false);
            }
        }

        for (Control c : controls) {
            if (c != null) {
                String style = c.getStyle();
                if (c instanceof TextArea) {
                    style = style.replace(AREA_ERROR_STYLE, "").replace(AREA_VALID_STYLE, AREA_NORMAL_STYLE);
                } else {
                    style = style.replace(ERROR_STYLE, "").replace(VALID_STYLE, NORMAL_STYLE);
                }
                c.setStyle(style);
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  VALIDATION FINALE DU FORMULAIRE (appelée dans handleSave)
    // ════════════════════════════════════════════════════════════

    private boolean validateForm() {
        boolean ok = true;

        if (!validateTitre(fieldTitre.getText().trim())) ok = false;
        if (!validateDescription(fieldDescription.getText().trim())) ok = false;
        if (!validateDate(fieldDateLimite.getValue())) ok = false;

        if (comboStatut.getValue() == null) {
            setError(errStatut, "Sélectionnez un statut", comboStatut);
            ok = false;
        } else {
            clearError(errStatut);
        }

        if (comboPriorite.getValue() == null) {
            setError(errPriorite, "Sélectionnez une priorité", comboPriorite);
            ok = false;
        } else {
            clearError(errPriorite);
        }

        return ok;
    }

    // ════════════════════════════════════════════════════════════
    //  CORRECTION AUTOMATIQUE
    // ════════════════════════════════════════════════════════════

    private void setupAutoCorrection() {
        // Debounce pour le titre (500ms)
        fieldTitre.textProperty().addListener((obs, old, newVal) -> {
            if (titreDebounceTimer != null) titreDebounceTimer.stop();
            titreDebounceTimer = new Timeline(new KeyFrame(Duration.millis(500), e -> checkTitreSuggestions()));
            titreDebounceTimer.play();
        });

        // Debounce pour la description (500ms)
        fieldDescription.textProperty().addListener((obs, old, newVal) -> {
            if (descDebounceTimer != null) descDebounceTimer.stop();
            descDebounceTimer = new Timeline(new KeyFrame(Duration.millis(500), e -> checkDescriptionSuggestions()));
            descDebounceTimer.play();
        });

        // Changement de langue
        comboLangue.valueProperty().addListener((obs, old, newVal) -> {
            checkTitreSuggestions();
            checkDescriptionSuggestions();
        });
    }

    private void checkTitreSuggestions() {
        String texte = fieldTitre.getText();
        if (texte == null || texte.trim().isEmpty()) return;

        String langue = comboLangue.getValue();

        new Thread(() -> {
            List<SpellCheckerService.SpellingError> errors = spellChecker.checkText(texte, langue);
            Platform.runLater(() -> {
                if (!errors.isEmpty()) {
                    showSuggestionsForTitre(errors);
                } else if (titreSuggestionsMenu != null) {
                    titreSuggestionsMenu.hide();
                }
            });
        }).start();
    }

    private void checkDescriptionSuggestions() {
        String texte = fieldDescription.getText();
        if (texte == null || texte.trim().isEmpty()) return;

        String langue = comboLangue.getValue();

        new Thread(() -> {
            List<SpellCheckerService.SpellingError> errors = spellChecker.checkText(texte, langue);
            Platform.runLater(() -> {
                if (!errors.isEmpty()) {
                    showSuggestionsForDescription(errors);
                } else if (descriptionSuggestionsMenu != null) {
                    descriptionSuggestionsMenu.hide();
                }
            });
        }).start();
    }

    private void showSuggestionsForTitre(List<SpellCheckerService.SpellingError> errors) {
        if (titreSuggestionsMenu != null) titreSuggestionsMenu.hide();

        titreSuggestionsMenu = new ContextMenu();

        // En-tête
        Label headerLabel = new Label("✏️ Suggestions de correction");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-padding: 8 12 4 12; -fx-font-size: 12px;");
        CustomMenuItem headerItem = new CustomMenuItem(headerLabel);
        headerItem.setDisable(true);
        titreSuggestionsMenu.getItems().add(headerItem);
        titreSuggestionsMenu.getItems().add(new SeparatorMenuItem());

        for (SpellCheckerService.SpellingError error : errors) {
            String wrongWord = "";
            try {
                wrongWord = fieldTitre.getText().substring(error.getOffset(),
                        Math.min(error.getOffset() + error.getLength(), fieldTitre.getText().length()));
            } catch (Exception e) { continue; }

            // Mot erroné
            Label errorLabel = new Label("❌ " + wrongWord);
            errorLabel.setStyle("-fx-text-fill: #E11D48; -fx-font-weight: bold; -fx-padding: 6 12 2 12; -fx-font-size: 11px;");
            errorLabel.setWrapText(true);
            errorLabel.setMaxWidth(300);
            CustomMenuItem errorItem = new CustomMenuItem(errorLabel);
            errorItem.setDisable(true);
            titreSuggestionsMenu.getItems().add(errorItem);

            // Suggestions cliquables
            if (error.getSuggestions() != null && !error.getSuggestions().isEmpty()) {
                FlowPane suggestionsFlow = new FlowPane(8, 8);
                suggestionsFlow.setPadding(new Insets(4, 12, 8, 24));
                suggestionsFlow.setAlignment(Pos.CENTER_LEFT);

                for (String suggestion : error.getSuggestions()) {
                    Button suggestionBtn = new Button(suggestion);
                    suggestionBtn.setStyle("-fx-background-color: #E0E7FF; -fx-text-fill: #4338CA; " +
                            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 15; " +
                            "-fx-padding: 4 12 4 12; -fx-cursor: hand;");
                    suggestionBtn.setOnAction(e -> {
                        String currentText = fieldTitre.getText();
                        String corrected = currentText.substring(0, error.getOffset())
                                + suggestion
                                + currentText.substring(error.getOffset() + error.getLength());
                        fieldTitre.setText(corrected);
                        fieldTitre.positionCaret(corrected.length());
                        setValidStyle(fieldTitre);
                        titreSuggestionsMenu.hide();
                    });
                    suggestionsFlow.getChildren().add(suggestionBtn);
                }

                CustomMenuItem suggestionsItem = new CustomMenuItem(suggestionsFlow);
                suggestionsItem.setDisable(true);
                titreSuggestionsMenu.getItems().add(suggestionsItem);
            } else {
                Label noSuggestion = new Label("  Aucune suggestion");
                noSuggestion.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px; -fx-padding: 4 12 8 24;");
                CustomMenuItem noSuggestionItem = new CustomMenuItem(noSuggestion);
                noSuggestionItem.setDisable(true);
                titreSuggestionsMenu.getItems().add(noSuggestionItem);
            }

            titreSuggestionsMenu.getItems().add(new SeparatorMenuItem());
        }

        // Ignorer tout
        MenuItem ignoreAll = new MenuItem("Ignorer toutes les suggestions");
        ignoreAll.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-padding: 8 12 8 12;");
        ignoreAll.setOnAction(e -> titreSuggestionsMenu.hide());
        titreSuggestionsMenu.getItems().add(ignoreAll);

        // Positionner sous le champ
        titreSuggestionsMenu.show(fieldTitre,
                fieldTitre.localToScreen(fieldTitre.getBoundsInLocal()).getMinX(),
                fieldTitre.localToScreen(fieldTitre.getBoundsInLocal()).getMaxY());
    }

    private void showSuggestionsForDescription(List<SpellCheckerService.SpellingError> errors) {
        if (descriptionSuggestionsMenu != null) descriptionSuggestionsMenu.hide();

        descriptionSuggestionsMenu = new ContextMenu();

        // En-tête
        Label headerLabel = new Label("✏️ Suggestions de correction");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B; -fx-padding: 8 12 4 12; -fx-font-size: 12px;");
        CustomMenuItem headerItem = new CustomMenuItem(headerLabel);
        headerItem.setDisable(true);
        descriptionSuggestionsMenu.getItems().add(headerItem);
        descriptionSuggestionsMenu.getItems().add(new SeparatorMenuItem());

        for (SpellCheckerService.SpellingError error : errors) {
            String wrongWord = "";
            try {
                wrongWord = fieldDescription.getText().substring(error.getOffset(),
                        Math.min(error.getOffset() + error.getLength(), fieldDescription.getText().length()));
            } catch (Exception e) { continue; }

            // Mot erroné
            Label errorLabel = new Label("❌ " + wrongWord);
            errorLabel.setStyle("-fx-text-fill: #E11D48; -fx-font-weight: bold; -fx-padding: 6 12 2 12; -fx-font-size: 11px;");
            errorLabel.setWrapText(true);
            errorLabel.setMaxWidth(300);
            CustomMenuItem errorItem = new CustomMenuItem(errorLabel);
            errorItem.setDisable(true);
            descriptionSuggestionsMenu.getItems().add(errorItem);

            // Suggestions cliquables
            if (error.getSuggestions() != null && !error.getSuggestions().isEmpty()) {
                FlowPane suggestionsFlow = new FlowPane(8, 8);
                suggestionsFlow.setPadding(new Insets(4, 12, 8, 24));
                suggestionsFlow.setAlignment(Pos.CENTER_LEFT);

                for (String suggestion : error.getSuggestions()) {
                    Button suggestionBtn = new Button(suggestion);
                    suggestionBtn.setStyle("-fx-background-color: #E0E7FF; -fx-text-fill: #4338CA; " +
                            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 15; " +
                            "-fx-padding: 4 12 4 12; -fx-cursor: hand;");
                    suggestionBtn.setOnAction(e -> {
                        String currentText = fieldDescription.getText();
                        String corrected = currentText.substring(0, error.getOffset())
                                + suggestion
                                + currentText.substring(error.getOffset() + error.getLength());
                        fieldDescription.setText(corrected);
                        fieldDescription.positionCaret(corrected.length());
                        setValidStyle(fieldDescription);
                        descriptionSuggestionsMenu.hide();
                    });
                    suggestionsFlow.getChildren().add(suggestionBtn);
                }

                CustomMenuItem suggestionsItem = new CustomMenuItem(suggestionsFlow);
                suggestionsItem.setDisable(true);
                descriptionSuggestionsMenu.getItems().add(suggestionsItem);
            } else {
                Label noSuggestion = new Label("  Aucune suggestion");
                noSuggestion.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px; -fx-padding: 4 12 8 24;");
                CustomMenuItem noSuggestionItem = new CustomMenuItem(noSuggestion);
                noSuggestionItem.setDisable(true);
                descriptionSuggestionsMenu.getItems().add(noSuggestionItem);
            }

            descriptionSuggestionsMenu.getItems().add(new SeparatorMenuItem());
        }

        // Ignorer tout
        MenuItem ignoreAll = new MenuItem("Ignorer toutes les suggestions");
        ignoreAll.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-padding: 8 12 8 12;");
        ignoreAll.setOnAction(e -> descriptionSuggestionsMenu.hide());
        descriptionSuggestionsMenu.getItems().add(ignoreAll);

        // Positionner sous le champ
        descriptionSuggestionsMenu.show(fieldDescription,
                fieldDescription.localToScreen(fieldDescription.getBoundsInLocal()).getMinX(),
                fieldDescription.localToScreen(fieldDescription.getBoundsInLocal()).getMaxY());
    }

    // ════════════════════════════════════════════════════════════
    //  CHARGEMENT DONNÉES
    // ════════════════════════════════════════════════════════════

    private void loadData() {
        if (currentObjectif == null) return;
        try {
            allData.clear();
            List<Tache> taches = service.recupererParObjectif(currentObjectif.getId());
            allData.addAll(taches);
            setupDropZones();
            renderKanban(allData);
            updateCountLabel(allData.size());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  RENDU KANBAN
    // ════════════════════════════════════════════════════════════

    private void renderKanban(List<Tache> list) {
        clearColonne(colAFaire);
        clearColonne(colEnCours);
        clearColonne(colTerminee);
        clearColonne(colAnnulee);

        Map<String, List<Tache>> parStatut = new LinkedHashMap<>();
        parStatut.put("À faire", new ArrayList<>());
        parStatut.put("En cours", new ArrayList<>());
        parStatut.put("Terminée", new ArrayList<>());
        parStatut.put("Annulée", new ArrayList<>());

        for (Tache t : list) {
            String statut = t.getStatut() != null ? t.getStatut() : "À faire";
            parStatut.computeIfAbsent(statut, k -> new ArrayList<>()).add(t);
        }

        remplirColonne(colAFaire, parStatut.get("À faire"));
        remplirColonne(colEnCours, parStatut.get("En cours"));
        remplirColonne(colTerminee, parStatut.get("Terminée"));
        remplirColonne(colAnnulee, parStatut.get("Annulée"));

        setCompteur(cntAFaire, parStatut.get("À faire").size());
        setCompteur(cntEnCours, parStatut.get("En cours").size());
        setCompteur(cntTerminee, parStatut.get("Terminée").size());
        setCompteur(cntAnnulee, parStatut.get("Annulée").size());
    }

    private void clearColonne(VBox col) {
        if (col == null) return;
        col.getChildren().removeIf(n -> "card".equals(n.getUserData()));
    }

    private void remplirColonne(VBox col, List<Tache> taches) {
        if (col == null) return;
        for (Tache t : taches) {
            VBox card = buildKanbanCard(t);
            card.setUserData("card");
            col.getChildren().add(card);
        }
        if (taches.isEmpty()) {
            VBox ph = buildPlaceholder();
            ph.setUserData("card");
            col.getChildren().add(ph);
        }
    }

    private void setCompteur(Label lbl, int count) {
        if (lbl != null) lbl.setText(String.valueOf(count));
    }

    // ════════════════════════════════════════════════════════════
    //  CONSTRUCTION CARTE KANBAN
    // ════════════════════════════════════════════════════════════

    private VBox buildKanbanCard(Tache t) {
        boolean owner = isOwner();

        String[] pColors = getPrioriteColors(t.getPriorite());
        boolean enRetard = t.getDateLimite() != null
                && t.getDateLimite().isBefore(LocalDate.now())
                && !"Terminée".equals(t.getStatut());

        VBox card = new VBox(9);
        card.setPadding(new Insets(12, 13, 11, 13));
        card.setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:13;" +
                        "-fx-border-color:" + (enRetard ? "#FCA5A5" : "#E2E8F0") + ";" +
                        "-fx-border-width:" + (enRetard ? "1.5" : "1") + ";" +
                        "-fx-border-radius:13;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,3);"
        );

        HBox prioriteBande = new HBox(5);
        prioriteBande.setAlignment(Pos.CENTER_LEFT);
        prioriteBande.setPadding(new Insets(3, 8, 3, 8));
        prioriteBande.setStyle("-fx-background-color:" + pColors[0] + ";-fx-background-radius:7;");

        Label prioIcon = new Label(getPrioriteIcon(t.getPriorite()));
        prioIcon.setStyle("-fx-font-size:10px;");
        Label prioLabel = new Label(t.getPriorite() != null ? t.getPriorite() : "Basse");
        prioLabel.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:" + pColors[2] + ";");

        if (enRetard) {
            Label retardBadge = new Label("⚠ Retard");
            retardBadge.setStyle("-fx-background-color:#FEE2E2;-fx-text-fill:#B91C1C;-fx-font-size:9px;-fx-font-weight:bold;-fx-background-radius:5;-fx-padding:2 5 2 5;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            prioriteBande.getChildren().addAll(prioIcon, prioLabel, spacer, retardBadge);
        } else {
            prioriteBande.getChildren().addAll(prioIcon, prioLabel);
        }

        Label titreLabel = new Label(t.getTitre() != null ? t.getTitre() : "Sans titre");
        titreLabel.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1E293B;-fx-wrap-text:true;");
        titreLabel.setWrapText(true);
        card.getChildren().addAll(prioriteBande, titreLabel);

        if (t.getDescription() != null && !t.getDescription().isBlank()) {
            String desc = t.getDescription().length() > 55 ? t.getDescription().substring(0, 52) + "…" : t.getDescription();
            Label descLabel = new Label(desc);
            descLabel.setStyle("-fx-font-size:11px;-fx-text-fill:#94A3B8;-fx-wrap-text:true;");
            descLabel.setWrapText(true);
            card.getChildren().add(descLabel);
        }

        if (t.getDateLimite() != null) {
            HBox dateBox = new HBox(4);
            dateBox.setAlignment(Pos.CENTER_LEFT);
            Label dateIcon = new Label("⏰");
            dateIcon.setStyle("-fx-font-size:10px;");
            Label dateVal = new Label(t.getDateLimite().format(FMT));
            dateVal.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:" + (enRetard ? "#EF4444" : "#64748B") + ";");
            dateBox.getChildren().addAll(dateIcon, dateVal);
            card.getChildren().add(dateBox);
        }

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#F1F5F9;");
        card.getChildren().add(sep);

        HBox actionsBox = new HBox(5);
        actionsBox.setAlignment(Pos.CENTER_LEFT);

        Button btnVoir = makeSmallBtn("👁", "#F1F5F9", "#475569");
        btnVoir.setTooltip(new Tooltip("Voir les détails"));
        btnVoir.setOnAction(e -> showDetailsTache(t));
        actionsBox.getChildren().add(btnVoir);

        if (owner) {
            Button btnEdit = makeSmallBtn("✏️", "#EFF6FF", "#3B82F6");
            btnEdit.setTooltip(new Tooltip("Modifier"));
            btnEdit.setOnAction(e -> openEditForm(t));

            Button btnDel = makeSmallBtn("🗑️", "#FFF1F2", "#E11D48");
            btnDel.setTooltip(new Tooltip("Supprimer"));
            btnDel.setOnAction(e -> handleDelete(t));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label dragHint = new Label("⠿ glisser");
            dragHint.setStyle("-fx-font-size:9px;-fx-text-fill:#CBD5E1;-fx-cursor:open-hand;");
            actionsBox.getChildren().addAll(btnEdit, btnDel, spacer, dragHint);

            setupDragAndDrop(card, t);
        }

        card.getChildren().add(actionsBox);
        return card;
    }

    private String[] getPrioriteColors(String priorite) {
        switch (priorite != null ? priorite : "Basse") {
            case "Urgente": return new String[]{"#FEE2E2", "#EF4444", "#991B1B"};
            case "Haute":   return new String[]{"#FEF3C7", "#F59E0B", "#92400E"};
            case "Normale": return new String[]{"#DBEAFE", "#3B82F6", "#1E40AF"};
            default:        return new String[]{"#DCFCE7", "#22C55E", "#166534"};
        }
    }

    private void setupDragAndDrop(VBox card, Tache t) {
        card.setCursor(javafx.scene.Cursor.OPEN_HAND);
        card.setOnDragDetected(event -> {
            tacheEnGlissement = t;
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent cc = new ClipboardContent();
            cc.putString(String.valueOf(t.getId()));
            db.setContent(cc);
            card.setOpacity(0.55);
            event.consume();
        });
        card.setOnDragDone(event -> {
            card.setOpacity(1.0);
            tacheEnGlissement = null;
            event.consume();
        });
    }

    private VBox buildPlaceholder() {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color:#F8FAFC;-fx-background-radius:10;-fx-border-color:#E2E8F0;-fx-border-style:dashed;-fx-border-width:1.5;-fx-border-radius:10;");
        Label icon = new Label("📭");
        icon.setStyle("-fx-font-size:20px;");
        Label msg = new Label("Aucune tâche");
        msg.setStyle("-fx-font-size:10px;-fx-text-fill:#CBD5E1;-fx-font-weight:bold;");
        box.getChildren().addAll(icon, msg);
        return box;
    }

    // ════════════════════════════════════════════════════════════
    //  SETUP DROP ZONES
    // ════════════════════════════════════════════════════════════

    private void setupDropZones() {
        if (colAFaire != null) setupDropZone(colAFaire, "À faire");
        if (colEnCours != null) setupDropZone(colEnCours, "En cours");
        if (colTerminee != null) setupDropZone(colTerminee, "Terminée");
        if (colAnnulee != null) setupDropZone(colAnnulee, "Annulée");
    }

    private void setupDropZone(VBox col, String nouveauStatut) {
        String baseStyle = col.getStyle() != null ? col.getStyle() : "";

        col.setOnDragOver(event -> {
            if (event.getDragboard().hasString() && isOwner()) {
                event.acceptTransferModes(TransferMode.MOVE);
                col.setStyle(baseStyle + "-fx-border-color:#3B82F6;-fx-border-width:2;-fx-border-radius:16;-fx-background-color:#3B82F612;");
            }
            event.consume();
        });

        col.setOnDragExited(event -> {
            col.setStyle(baseStyle);
            event.consume();
        });

        col.setOnDragDropped(event -> {
            col.setStyle(baseStyle);
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasString() && tacheEnGlissement != null && isOwner()) {
                String ancienStatut = tacheEnGlissement.getStatut();
                if (!nouveauStatut.equals(ancienStatut)) {
                    try {
                        tacheEnGlissement.setStatut(nouveauStatut);
                        service.modifier(tacheEnGlissement);
                        loadData();
                        success = true;
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Tâche déplacée vers '" + nouveauStatut + "'");
                    } catch (SQLException e) {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de déplacer la tâche");
                    }
                } else {
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private boolean isOwner() {
        if (currentUser == null || currentObjectif == null) return false;
        return currentObjectif.getIdUserId() == currentUser.getId();
    }

    // ════════════════════════════════════════════════════════════
    //  RECHERCHE
    // ════════════════════════════════════════════════════════════

    @FXML
    private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) {
            renderKanban(allData);
            updateCountLabel(allData.size());
            return;
        }
        List<Tache> filtered = allData.stream()
                .filter(t -> (t.getTitre() != null && t.getTitre().toLowerCase().contains(q))
                        || (t.getDescription() != null && t.getDescription().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        renderKanban(filtered);
        updateCountLabel(filtered.size());
    }

    // ════════════════════════════════════════════════════════════
    //  FORMULAIRE
    // ════════════════════════════════════════════════════════════

    @FXML
    private void handleAjouter() {
        if (!isOwner()) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé", "Vous ne pouvez pas ajouter de tâches à l'objectif d'un autre utilisateur.");
            return;
        }
        selectedTache = null;
        clearForm();
        clearErrors();
        formTitle.setText("Nouvelle Tâche");
        formTitleIcon.setText("✚");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    private void openEditForm(Tache t) {
        if (!isOwner()) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé", "Vous ne pouvez modifier que vos propres tâches.");
            return;
        }
        selectedTache = t;
        clearErrors();
        fieldTitre.setText(t.getTitre() != null ? t.getTitre() : "");
        fieldDescription.setText(t.getDescription() != null ? t.getDescription() : "");
        fieldDateLimite.setValue(t.getDateLimite());
        comboStatut.setValue(t.getStatut());
        comboPriorite.setValue(t.getPriorite());
        formTitle.setText("Modifier la Tâche");
        formTitleIcon.setText("✎");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    @FXML
    private void handleSave() {
        clearErrors();
        if (!validateForm()) return;
        try {
            String titre = fieldTitre.getText().trim();
            String desc = fieldDescription.getText().trim();
            LocalDate dl = fieldDateLimite.getValue();
            String statut = comboStatut.getValue();
            String priorite = comboPriorite.getValue();
            int idObj = currentObjectif.getId();

            if (selectedTache == null) {
                service.ajouter(new Tache(titre, desc, dl, statut, priorite, idObj));
                showSuccessToast("✅ Tâche ajoutée avec succès !");
            } else {
                selectedTache.setTitre(titre);
                selectedTache.setDescription(desc);
                selectedTache.setDateLimite(dl);
                selectedTache.setStatut(statut);
                selectedTache.setPriorite(priorite);
                service.modifier(selectedTache);
                showSuccessToast("✅ Tâche modifiée avec succès !");
            }
            handleCancel();
            loadData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        formCard.setVisible(false);
        formCard.setManaged(false);
        clearForm();
        clearErrors();
        selectedTache = null;
    }

    private void handleDelete(Tache t) {
        if (!isOwner()) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé", "Vous ne pouvez supprimer que vos propres tâches.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer la tâche \"" + t.getTitre() + "\" ?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    service.supprimer(t.getId());
                    loadData();
                    showSuccessToast("🗑 Tâche supprimée !");
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
                }
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  DÉTAILS
    // ════════════════════════════════════════════════════════════

    private void showDetailsTache(Tache t) {
        String[] pColors = getPrioriteColors(t.getPriorite());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails — " + t.getTitre());
        dialog.setHeaderText(null);

        VBox content = new VBox(0);
        content.setPrefWidth(440);

        VBox header = new VBox(7);
        header.setPadding(new Insets(20, 24, 16, 24));
        header.setStyle("-fx-background-color:" + pColors[0] + ";");

        Label titleLbl = new Label(t.getTitre() != null ? t.getTitre() : "—");
        titleLbl.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:" + pColors[2] + ";-fx-wrap-text:true;");
        titleLbl.setWrapText(true);

        Label statutLbl = new Label(getStatutIcon(t.getStatut()) + "  " + (t.getStatut() != null ? t.getStatut() : "—"));
        statutLbl.setStyle("-fx-font-size:12px;-fx-text-fill:" + pColors[1] + ";-fx-font-weight:bold;");
        header.getChildren().addAll(titleLbl, statutLbl);

        VBox body = new VBox(12);
        body.setPadding(new Insets(16, 24, 20, 24));

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(9);
        grid.setStyle("-fx-background-color:#F8FAFC;-fx-background-radius:9;-fx-padding:13;");
        ColumnConstraints cc1 = new ColumnConstraints(110);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc1, cc2);

        String ls = "-fx-font-size:11px;-fx-text-fill:#94A3B8;-fx-font-weight:bold;";
        String vs = "-fx-font-size:12px;-fx-text-fill:#1E293B;-fx-font-weight:bold;";

        addRow(grid, 0, "Priorité", t.getPriorite() != null ? t.getPriorite() : "—", ls, vs);
        addRow(grid, 1, "Date limite", t.getDateLimite() != null ? t.getDateLimite().format(FMT) : "—", ls, vs);
        addRow(grid, 2, "Objectif", getObjectifTitre(t.getIdObjectifId()), ls, vs);

        Label descTitle = new Label("📝 Description");
        descTitle.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#374151;");

        TextArea descArea = new TextArea(t.getDescription() != null ? t.getDescription() : "Aucune description.");
        descArea.setEditable(false);
        descArea.setWrapText(true);
        descArea.setPrefHeight(65);
        descArea.setStyle("-fx-background-color:#F9FAFB;-fx-border-color:#E5E7EB;-fx-border-radius:8;-fx-font-size:12px;");

        body.getChildren().addAll(grid, descTitle, descArea);
        content.getChildren().addAll(header, body);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button closeBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeBtn.setText("Fermer");
        closeBtn.setStyle("-fx-background-color:" + pColors[1] + ";-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 20 8 20;-fx-cursor:hand;");
        dialog.showAndWait();
    }

    private void addRow(GridPane g, int row, String l, String v, String ls, String vs) {
        Label ll = new Label(l);
        ll.setStyle(ls);
        Label vv = new Label(v);
        vv.setStyle(vs);
        g.add(ll, 0, row);
        g.add(vv, 1, row);
    }

    @FXML
    private void handleRetour() {
        if (objectifController != null) objectifController.retourObjectifs();
    }

    private void clearForm() {
        fieldTitre.clear();
        fieldDescription.clear();
        fieldDateLimite.setValue(null);
        comboStatut.setValue(null);
        comboPriorite.setValue(null);
        comboLangue.setValue("Français");
    }

    private void updateCountLabel(int count) {
        if (countLabel != null) countLabel.setText(count + " tâche(s)");
    }

    private Button makeSmallBtn(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";-fx-background-radius:7;-fx-padding:4 8 4 8;-fx-cursor:hand;-fx-font-size:11px;");
        btn.setOnMouseEntered(e -> btn.setOpacity(0.8));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    private void showSuccessToast(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private String getStatutIcon(String s) {
        return switch (s != null ? s : "") {
            case "Terminée" -> "✅";
            case "En cours" -> "🔄";
            case "Annulée" -> "❌";
            default -> "📋";
        };
    }

    private String getPrioriteIcon(String p) {
        return switch (p != null ? p : "") {
            case "Urgente" -> "🔴";
            case "Haute" -> "🟠";
            case "Normale" -> "🟡";
            default -> "🟢";
        };

    }
    /**
     * Affiche les détails d'une tâche et la met en évidence
     */
    public void showDetailsAndHighlight(Tache tache) {
        Platform.runLater(() -> {
            // Afficher les détails
            showDetailsTache(tache);

            // Mettre en évidence la carte dans le Kanban
            String statut = tache.getStatut();
            VBox colonne = getColonneByStatut(statut);

            if (colonne != null) {
                for (javafx.scene.Node node : colonne.getChildren()) {
                    if (node instanceof VBox && node.getUserData() != null) {
                        // La carte contient la tâche, on cherche par titre
                        VBox card = (VBox) node;
                        for (javafx.scene.Node child : card.getChildren()) {
                            if (child instanceof Label && ((Label) child).getText().equals(tache.getTitre())) {
                                card.setStyle(card.getStyle() +
                                        "-fx-border-color: #6C63FF; -fx-border-width: 2; -fx-border-radius: 13;");

                                // Animation
                                Timeline blink = new Timeline(
                                        new KeyFrame(Duration.seconds(0), e -> card.setOpacity(1.0)),
                                        new KeyFrame(Duration.seconds(0.3), e -> card.setOpacity(0.5)),
                                        new KeyFrame(Duration.seconds(0.6), e -> card.setOpacity(1.0))
                                );
                                blink.setCycleCount(3);
                                blink.play();
                                break;
                            }
                        }
                    }
                }
            }
        });
    }

    private VBox getColonneByStatut(String statut) {
        switch (statut) {
            case "À faire": return colAFaire;
            case "En cours": return colEnCours;
            case "Terminée": return colTerminee;
            case "Annulée": return colAnnulee;
            default: return colAFaire;
        }
    }
}
