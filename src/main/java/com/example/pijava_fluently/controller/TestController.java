package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.entites.Niveau;
import com.example.pijava_fluently.entites.Question;
import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.services.LangueService;
import com.example.pijava_fluently.services.NiveauService;
import com.example.pijava_fluently.services.QuestionService;
import com.example.pijava_fluently.services.TestService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class TestController {

    @FXML private VBox            formCard;
    @FXML private Label           formTitle;
    @FXML private TextField       fieldTitre;
    @FXML private ComboBox<String>  comboType;
    @FXML private TextField       fieldDuree;
    @FXML private ComboBox<Langue>  comboLangue;
    @FXML private ComboBox<Niveau>  comboNiveau;
    @FXML private Label           countLabel;
    @FXML private TextField       searchField;
    @FXML private Label           labelErreur;

    @FXML private TableView<Test>            tableTests;
    @FXML private TableColumn<Test, Integer> colId;
    @FXML private TableColumn<Test, String>  colTitre;
    @FXML private TableColumn<Test, String>  colType;
    @FXML private TableColumn<Test, Integer> colDuree;
    @FXML private TableColumn<Test, Integer> colLangue;
    @FXML private TableColumn<Test, Integer> colNiveau;
    @FXML private TableColumn<Test, Void>    colActions;

    private final TestService     service         = new TestService();
    private final LangueService   langueService   = new LangueService();
    private final NiveauService   niveauService   = new NiveauService();
    private final QuestionService questionService = new QuestionService();

    private ObservableList<Test> allData = FXCollections.observableArrayList();
    private List<Langue> langues;
    private List<Niveau> niveaux;
    private Test selectedTest = null;

    // ── Init ──────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        comboType.setItems(FXCollections.observableArrayList(
                "Test de niveau", "Test de fin de niveau", "quiz_debutant"));
        chargerLanguesEtNiveaux();
        setupColumns();
        loadData();
    }

    private void chargerLanguesEtNiveaux() {
        try {
            langues = langueService.recuperer();
            comboLangue.setItems(FXCollections.observableArrayList(langues));
            comboLangue.setCellFactory(lv -> cellLangue());
            comboLangue.setButtonCell(cellLangue());
            comboLangue.valueProperty().addListener((obs, old, nv) -> filtrerNiveaux(nv));
        } catch (SQLException e) { e.printStackTrace(); }
        try {
            niveaux = niveauService.recuperer();
            configurerComboNiveau(niveaux);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private ListCell<Langue> cellLangue() {
        return new ListCell<>() {
            @Override protected void updateItem(Langue l, boolean empty) {
                super.updateItem(l, empty);
                setText(empty || l == null ? "— Sélectionner —" : l.getNom());
            }
        };
    }

    private void filtrerNiveaux(Langue l) {
        comboNiveau.setValue(null);
        List<Niveau> liste = l == null ? niveaux :
                niveaux.stream().filter(n -> n.getIdLangueId() == l.getId())
                        .collect(Collectors.toList());
        configurerComboNiveau(liste);
    }

    private void configurerComboNiveau(List<Niveau> liste) {
        comboNiveau.setItems(FXCollections.observableArrayList(liste));
        comboNiveau.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Niveau n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? "— Sélectionner —"
                        : n.getTitre() + " (" + n.getDifficulte() + ")");
            }
        });
        comboNiveau.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Niveau n, boolean empty) {
                super.updateItem(n, empty);
                setText(empty || n == null ? "— Sélectionner —"
                        : n.getTitre() + " (" + n.getDifficulte() + ")");
            }
        });
    }

    // ── Colonnes ──────────────────────────────────────────────────────
    private void setupColumns() {
        if (colId != null) colId.setVisible(false);

        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colTitre.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label lbl = new Label(item);
                lbl.setStyle(
                        "-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#1A1D2E;");
                lbl.setWrapText(true);
                setGraphic(lbl); setText(null);
            }
        });

        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) { setGraphic(null); return; }
                String color = switch (item) {
                    case "Test de niveau" ->
                            "-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;-fx-border-color:#BFDBFE;";
                    case "Test de fin de niveau" ->
                            "-fx-background-color:#F0FDF4;-fx-text-fill:#16A34A;-fx-border-color:#BBF7D0;";
                    default ->
                            "-fx-background-color:#FEF9C3;-fx-text-fill:#CA8A04;-fx-border-color:#FDE68A;";
                };
                Label b = new Label(item);
                b.setStyle(color + "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-background-radius:20;-fx-border-radius:20;" +
                        "-fx-border-width:1;-fx-padding:4 10;");
                setGraphic(b); setText(null);
            }
        });

        colDuree.setCellValueFactory(new PropertyValueFactory<>("dureeEstimee"));
        colDuree.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) { setText(null); return; }
                Label lbl = new Label("⏱ " + item + " min");
                lbl.setStyle(
                        "-fx-font-size:12px;-fx-text-fill:#6B7280;" +
                                "-fx-background-color:#F4F5FA;-fx-background-radius:8;-fx-padding:3 8;");
                setGraphic(lbl); setText(null);
            }
        });

        colLangue.setCellValueFactory(new PropertyValueFactory<>("langueId"));
        colLangue.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null || item == 0) {
                    setText("—"); setGraphic(null); return;
                }
                String nom = langues == null ? "#" + item :
                        langues.stream().filter(l -> l.getId() == item)
                                .map(Langue::getNom).findFirst().orElse("#" + item);
                Label lbl = new Label(nom);
                lbl.setStyle(
                        "-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#3B82F6;" +
                                "-fx-background-color:#EFF6FF;-fx-background-radius:8;-fx-padding:4 10;");
                setGraphic(lbl); setText(null);
            }
        });

        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveauId"));
        colNiveau.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null || item == 0) {
                    setText("—"); setGraphic(null); return;
                }
                String titre = niveaux == null ? "#" + item :
                        niveaux.stream().filter(n -> n.getId() == item)
                                .map(n -> n.getTitre() + " · " + n.getDifficulte())
                                .findFirst().orElse("#" + item);
                Label lbl = new Label(titre);
                lbl.setStyle(
                        "-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#7C3AED;" +
                                "-fx-background-color:#F5F3FF;-fx-background-radius:8;-fx-padding:4 10;");
                setGraphic(lbl); setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDetails = bouton("🔍 Détails",
                    "-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;",
                    "-fx-background-color:#DBEAFE;-fx-text-fill:#1D4ED8;");
            private final Button btnEdit    = bouton("✎ Modifier",
                    "-fx-background-color:#F5F3FF;-fx-text-fill:#7C3AED;",
                    "-fx-background-color:#EDE9FE;-fx-text-fill:#5B21B6;");
            private final Button btnDelete  = bouton("🗑 Supprimer",
                    "-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;",
                    "-fx-background-color:#FFE4E6;-fx-text-fill:#BE123C;");
            private final HBox box = new HBox(6, btnDetails, btnEdit, btnDelete);
            {
                box.setAlignment(Pos.CENTER);
                btnDetails.setOnAction(e -> afficherDetails(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(e    -> openEditForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e  -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableTests.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Test item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setStyle("");
                else if (getIndex() % 2 == 0) setStyle("-fx-background-color:#FAFBFF;");
                else setStyle("-fx-background-color:white;");
            }
        });
    }

    // ── Détails du test ───────────────────────────────────────────────
    private void afficherDetails(Test t) {
        String nomLangue = langues == null ? "#" + t.getLangueId() :
                langues.stream().filter(l -> l.getId() == t.getLangueId())
                        .map(Langue::getNom).findFirst().orElse("—");
        String nomNiveau = niveaux == null ? "#" + t.getNiveauId() :
                niveaux.stream().filter(n -> n.getId() == t.getNiveauId())
                        .map(n -> n.getTitre() + " (" + n.getDifficulte() + ")")
                        .findFirst().orElse("—");

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails — " + t.getTitre());
        dialog.setHeaderText(null);

        VBox root = new VBox(20);
        root.setPadding(new Insets(0));
        root.setPrefWidth(620);
        root.setStyle("-fx-background-color:#F8F9FD;");

        // ── Header ──
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color:linear-gradient(to right,#6C63FF,#8B7CF6);" +
                        "-fx-padding:22 28;");
        VBox hInfo = new VBox(5);
        Label hTitre = new Label(t.getTitre());
        hTitre.setStyle(
                "-fx-font-size:19px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label hType = new Label(t.getType());
        hType.setStyle(
                "-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.8);" +
                        "-fx-background-color:rgba(255,255,255,0.15);" +
                        "-fx-background-radius:20;-fx-padding:3 10;");
        hInfo.getChildren().addAll(hTitre, hType);
        Label hIco = new Label("📝");
        hIco.setStyle("-fx-font-size:32px;");
        header.getChildren().addAll(hIco, hInfo);

        // ── Infos générales ──
        GridPane grid = new GridPane();
        grid.setHgap(20); grid.setVgap(10);
        grid.setPadding(new Insets(20, 28, 0, 28));
        grid.setStyle(
                "-fx-background-color:white;-fx-background-radius:14;");
        String ls = "-fx-font-size:12px;-fx-text-fill:#8A8FA8;-fx-font-weight:bold;";
        String vs = "-fx-font-size:13px;-fx-text-fill:#1A1D2E;";
        ajouterLigne(grid, 0, "ID",       String.valueOf(t.getId()), ls, vs);
        ajouterLigne(grid, 1, "Durée",    t.getDureeEstimee() + " minutes", ls, vs);
        ajouterLigne(grid, 2, "Langue",   nomLangue, ls, vs);
        ajouterLigne(grid, 3, "Niveau",   nomNiveau, ls, vs);
        VBox gridWrapper = new VBox(grid);
        gridWrapper.setPadding(new Insets(0, 28, 0, 28));
        // (on construit le grid directement dans le contenu)

        // ── Section questions ──
        VBox secQuestions = new VBox(12);
        secQuestions.setPadding(new Insets(0, 28, 24, 28));

        Label titreQ = new Label("❓ Questions associées");
        titreQ.setStyle(
                "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#1A1D2E;");

        VBox listeQ = new VBox(8);
        try {
            List<Question> questions = questionService.recupererParTest(t.getId());
            if (questions.isEmpty()) {
                Label aucune = new Label("Aucune question associée à ce test.");
                aucune.setStyle("-fx-font-size:13px;-fx-text-fill:#8A8FA8;-fx-padding:10;");
                listeQ.getChildren().add(aucune);
            } else {
                for (int i = 0; i < questions.size(); i++) {
                    Question q = questions.get(i);
                    listeQ.getChildren().add(creerLigneQuestion(i + 1, q));
                }
            }
        } catch (SQLException e) {
            listeQ.getChildren().add(new Label("Erreur chargement questions."));
        }

        ScrollPane scrollQ = new ScrollPane(listeQ);
        scrollQ.setFitToWidth(true);
        scrollQ.setPrefHeight(Math.min(280, 60 + questionService.hashCode() % 100));
        scrollQ.setStyle(
                "-fx-background:transparent;-fx-background-color:transparent;" +
                        "-fx-border-color:transparent;");
        // Recalculer la hauteur proprement
        scrollQ.setPrefHeight(240);

        secQuestions.getChildren().addAll(titreQ, scrollQ);

        // ── Assemblage ──
        VBox content = new VBox(0);
        content.getChildren().add(header);

        VBox body = new VBox(16);
        body.setPadding(new Insets(20, 28, 20, 28));
        body.setStyle("-fx-background-color:#F8F9FD;");

        // Grille infos dans une carte blanche
        VBox infoCard = new VBox(grid);
        infoCard.setStyle(
                "-fx-background-color:white;-fx-background-radius:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),10,0,0,2);");
        grid.setPadding(new Insets(16));
        infoCard.getChildren().setAll(grid);

        body.getChildren().addAll(infoCard, secQuestions);
        content.getChildren().add(body);
        root.getChildren().add(content);

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setStyle("-fx-background-color:#F8F9FD;-fx-padding:0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Button close = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        close.setText("Fermer");
        close.setStyle(
                "-fx-background-color:#6C63FF;-fx-text-fill:white;" +
                        "-fx-font-size:13px;-fx-font-weight:bold;" +
                        "-fx-background-radius:10;-fx-padding:9 22;-fx-cursor:hand;");
        dialog.showAndWait();
    }

    private HBox creerLigneQuestion(int num, Question q) {
        HBox ligne = new HBox(12);
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setStyle(
                "-fx-background-color:white;-fx-background-radius:10;" +
                        "-fx-border-color:#F0F1F7;-fx-border-radius:10;-fx-border-width:1;" +
                        "-fx-padding:10 14;");

        Label numLbl = new Label(String.valueOf(num));
        numLbl.setStyle(
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#6C63FF;" +
                        "-fx-background-color:#F0EEFF;-fx-background-radius:20;-fx-padding:3 8;");
        numLbl.setMinWidth(28);

        String typeBg = switch (q.getType()) {
            case "qcm"         -> "-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;";
            case "oral"        -> "-fx-background-color:#F0FDF4;-fx-text-fill:#16A34A;";
            case "texte_libre" -> "-fx-background-color:#FEF9C3;-fx-text-fill:#CA8A04;";
            default            -> "-fx-background-color:#F4F5FA;-fx-text-fill:#6B7280;";
        };
        Label typeLbl = new Label(q.getType().toUpperCase());
        typeLbl.setStyle(typeBg +
                "-fx-font-size:9px;-fx-font-weight:bold;" +
                "-fx-background-radius:20;-fx-padding:3 8;");

        String enonce = q.getEnonce().length() > 70
                ? q.getEnonce().substring(0, 67) + "…" : q.getEnonce();
        Label enonceLbl = new Label(enonce);
        enonceLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#1A1D2E;");
        enonceLbl.setTooltip(new Tooltip(q.getEnonce()));
        HBox.setHgrow(enonceLbl, Priority.ALWAYS);

        Label scoreLbl = new Label(q.getScoreMax() + " pts");
        scoreLbl.setStyle(
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#6C63FF;");

        ligne.getChildren().addAll(numLbl, typeLbl, enonceLbl, scoreLbl);
        return ligne;
    }

    private void ajouterLigne(GridPane g, int row,
                              String label, String val,
                              String ls, String vs) {
        Label l = new Label(label); l.setStyle(ls);
        Label v = new Label(val);   v.setStyle(vs);
        g.add(l, 0, row); g.add(v, 1, row);
    }

    // ── Bouton helper ─────────────────────────────────────────────────
    private Button bouton(String text, String normal, String hover) {
        Button b = new Button(text);
        String base =
                "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-background-radius:8;-fx-padding:6 12;-fx-cursor:hand;";
        b.setStyle(normal + base);
        b.setOnMouseEntered(e -> b.setStyle(hover + base));
        b.setOnMouseExited(e  -> b.setStyle(normal + base));
        return b;
    }

    // ── CRUD standard ─────────────────────────────────────────────────
    private void loadData() {
        try {
            allData = FXCollections.observableArrayList(service.recuperer());
            tableTests.setItems(allData);
            if (countLabel != null) countLabel.setText(allData.size() + " test(s)");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    @FXML private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) {
            tableTests.setItems(allData);
            countLabel.setText(allData.size() + " test(s)");
        } else {
            ObservableList<Test> filtered = allData.stream()
                    .filter(t -> t.getTitre().toLowerCase().contains(q)
                            || t.getType().toLowerCase().contains(q))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            tableTests.setItems(filtered);
            countLabel.setText(filtered.size() + " résultat(s)");
        }
    }

    @FXML private void handleAjouter() {
        selectedTest = null; clearForm(); cacherErreur();
        formTitle.setText("Nouveau Test");
        formCard.setVisible(true); formCard.setManaged(true);
    }

    private void openEditForm(Test t) {
        selectedTest = t;
        fieldTitre.setText(t.getTitre());
        comboType.setValue(t.getType());
        fieldDuree.setText(String.valueOf(t.getDureeEstimee()));
        if (langues != null)
            langues.stream().filter(l -> l.getId() == t.getLangueId())
                    .findFirst().ifPresent(comboLangue::setValue);
        if (niveaux != null)
            niveaux.stream().filter(n -> n.getId() == t.getNiveauId())
                    .findFirst().ifPresent(comboNiveau::setValue);
        cacherErreur();
        formTitle.setText("Modifier le Test");
        formCard.setVisible(true); formCard.setManaged(true);
    }

    @FXML private void handleSave() {
        String err = validateForm();
        if (err != null) { afficherErreur(err); return; }
        cacherErreur();
        try {
            int duree    = Integer.parseInt(fieldDuree.getText().trim());
            int langueId = comboLangue.getValue().getId();
            int niveauId = comboNiveau.getValue() != null
                    ? comboNiveau.getValue().getId() : 0;
            if (selectedTest == null) {
                service.ajouter(new Test(comboType.getValue(),
                        fieldTitre.getText().trim(), duree, langueId, niveauId));
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Test ajouté !");
            } else {
                selectedTest.setTitre(fieldTitre.getText().trim());
                selectedTest.setType(comboType.getValue());
                selectedTest.setDureeEstimee(duree);
                selectedTest.setLangueId(langueId);
                selectedTest.setNiveauId(niveauId);
                service.modifier(selectedTest);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Test modifié !");
            }
            handleCancel(); loadData();
        } catch (NumberFormatException e) {
            afficherErreur("La durée doit être un nombre entier.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    private void handleDelete(Test t) {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer \"" + t.getTitre() + "\" ?", ButtonType.YES, ButtonType.NO);
        c.setHeaderText(null);
        c.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { service.supprimer(t.getId()); loadData(); }
                catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
                }
            }
        });
    }

    @FXML private void handleCancel() {
        formCard.setVisible(false); formCard.setManaged(false);
        clearForm(); selectedTest = null;
    }

    private String validateForm() {
        if (fieldTitre.getText().isBlank()) return "Le titre est obligatoire.";
        if (fieldTitre.getText().trim().length() < 3)
            return "Le titre doit contenir au moins 3 caractères.";
        if (comboType.getValue() == null) return "Le type est obligatoire.";
        if (fieldDuree.getText().isBlank()) return "La durée est obligatoire.";
        try {
            int d = Integer.parseInt(fieldDuree.getText().trim());
            if (d <= 0)  return "La durée doit être > 0.";
            if (d > 300) return "La durée ne peut pas dépasser 300 minutes.";
        } catch (NumberFormatException e) { return "La durée doit être un entier."; }
        if (comboLangue.getValue() == null) return "Veuillez sélectionner une langue.";
        return null;
    }

    private void clearForm() {
        fieldTitre.clear(); comboType.setValue(null);
        fieldDuree.clear(); comboLangue.setValue(null); comboNiveau.setValue(null);
    }

    private void afficherErreur(String msg) {
        if (labelErreur != null) {
            labelErreur.setText("⚠ " + msg);
            labelErreur.setVisible(true); labelErreur.setManaged(true);
        } else showAlert(Alert.AlertType.WARNING, "Validation", msg);
    }

    private void cacherErreur() {
        if (labelErreur != null) {
            labelErreur.setVisible(false); labelErreur.setManaged(false);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
}