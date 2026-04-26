package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Question;
import com.example.pijava_fluently.entites.Reponse;
import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.services.QuestionService;
import com.example.pijava_fluently.services.ReponseService;
import com.example.pijava_fluently.services.TestService;
import com.example.pijava_fluently.utils.LoggerUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import com.example.pijava_fluently.services.AIQuizGeneratorService;
import javafx.application.Platform;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class QuestionController {

    @FXML private VBox           formCard;
    @FXML private Label          formTitle;
    @FXML private TextArea       fieldEnonce;
    @FXML private ComboBox<String> comboType;
    @FXML private TextField      fieldScoreMax;
    @FXML private ComboBox<Test> comboTest;
    @FXML private Label          countLabel;
    @FXML private TextField      searchField;
    @FXML private Label          labelErreur;

    @FXML private TableView<Question>            tableQuestions;
    @FXML private TableColumn<Question, Integer> colId;
    @FXML private TableColumn<Question, String>  colEnonce;
    @FXML private TableColumn<Question, String>  colType;
    @FXML private TableColumn<Question, Integer> colScore;
    @FXML private TableColumn<Question, Integer> colTest;
    @FXML private TableColumn<Question, Void>    colActions;

    private final QuestionService service        = new QuestionService();
    private final TestService     testService    = new TestService();
    private final ReponseService  reponseService = new ReponseService();
    private ObservableList<Question> allData     = FXCollections.observableArrayList();
    private Question selectedQuestion            = null;
    private final AIQuizGeneratorService aiQuizService = new AIQuizGeneratorService();


    @FXML
    public void initialize() {
        comboType.setItems(FXCollections.observableArrayList("qcm", "oral", "texte_libre"));
        try {
            List<Test> tests = testService.recuperer();
            comboTest.setItems(FXCollections.observableArrayList(tests));
            comboTest.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Test t, boolean empty) {
                    super.updateItem(t, empty);
                    setText(empty || t == null ? "— Sélectionner —"
                            : t.getTitre() + " [" + t.getType() + "]");
                }
            });
            comboTest.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Test t, boolean empty) {
                    super.updateItem(t, empty);
                    setText(empty || t == null ? "— Sélectionner —" : t.getTitre());
                }
            });
        } catch (SQLException e) { e.printStackTrace(); }
        setupColumns();
        loadData();
        ajouterBoutonIA();
    }

    private void setupColumns() {
        if (colId != null) colId.setVisible(false);

        colEnonce.setCellValueFactory(new PropertyValueFactory<>("enonce"));
        colEnonce.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                String txt = item.length() > 65
                        ? item.substring(0, 62) + "…" : item;
                Label lbl = new Label(txt);
                lbl.setStyle("-fx-font-size:12px;-fx-text-fill:#1A1D2E;");
                lbl.setTooltip(new Tooltip(item));
                setGraphic(lbl); setText(null);
            }
        });

        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) { setGraphic(null); return; }
                String c = switch (item) {
                    case "qcm"         -> "-fx-background-color:#EFF6FF;-fx-text-fill:#3B82F6;";
                    case "oral"        -> "-fx-background-color:#F0FDF4;-fx-text-fill:#16A34A;";
                    case "texte_libre" -> "-fx-background-color:#FEF9C3;-fx-text-fill:#CA8A04;";
                    default            -> "-fx-background-color:#F4F5FA;-fx-text-fill:#6B7280;";
                };
                Label b = new Label(item.toUpperCase());
                b.setStyle(c + "-fx-font-size:10px;-fx-font-weight:bold;" +
                        "-fx-background-radius:20;-fx-padding:3 8;");
                setGraphic(b); setText(null);
            }
        });

        colScore.setCellValueFactory(new PropertyValueFactory<>("scoreMax"));
        colScore.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) { setText(null); return; }
                Label lbl = new Label(item + " pts");
                lbl.setStyle(
                        "-fx-font-weight:bold;-fx-text-fill:#6C63FF;-fx-font-size:12px;");
                setGraphic(lbl); setText(null);
            }
        });

        colTest.setCellValueFactory(new PropertyValueFactory<>("testId"));
        colTest.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) { setText("—"); setGraphic(null); return; }
                String titre = comboTest.getItems().stream()
                        .filter(t -> t.getId() == item)
                        .map(Test::getTitre)
                        .findFirst().orElse("Test #" + item);
                Label lbl = new Label(titre);
                lbl.setStyle(
                        "-fx-font-size:11px;-fx-text-fill:#6B7280;" +
                                "-fx-background-color:#F4F5FA;-fx-background-radius:8;-fx-padding:3 8;");
                setGraphic(lbl); setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDetails = bouton(
                    "🔍 Détails",
                    "#EFF6FF", "#3B82F6", "#DBEAFE", "#1D4ED8", "#BFDBFE");
            private final Button btnEdit = bouton(
                    "✎ Modifier",
                    "#F5F3FF", "#7C3AED", "#EDE9FE", "#5B21B6", "#DDD6FE");
            private final Button btnDelete = bouton(
                    "🗑 Supprimer",
                    "#FFF1F2", "#E11D48", "#FFE4E6", "#BE123C", "#FECDD3");
            private final HBox box = new HBox(6, btnDetails, btnEdit, btnDelete);
            {
                box.setAlignment(javafx.geometry.Pos.CENTER);
                btnDetails.setOnAction(e ->
                        afficherDetails(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(e ->
                        openEditForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e ->
                        handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tableQuestions.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Question item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setStyle("");
                else if (getIndex() % 2 == 0) setStyle("-fx-background-color:#FAFBFF;");
                else setStyle("-fx-background-color:white;");
            }
        });
    }

    // ── Détails de la question ─────────────────────────────────────────
    private void afficherDetails(Question q) {
        String titreTest = comboTest.getItems().stream()
                .filter(t -> t.getId() == q.getTestId())
                .map(Test::getTitre).findFirst().orElse("Test #" + q.getTestId());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails — Question #" + q.getId());
        dialog.setHeaderText(null);

        VBox root = new VBox(0);
        root.setPrefWidth(580);

        // Header
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color:linear-gradient(to right,#6C63FF,#8B7CF6);" +
                        "-fx-padding:20 24;");
        Label ico = new Label("❓");
        ico.setStyle("-fx-font-size:28px;");
        VBox hInfo = new VBox(5);
        Label hTitre = new Label("Question #" + q.getId());
        hTitre.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label hType = new Label(q.getType().toUpperCase() + "  ·  " + q.getScoreMax() + " pts");
        hType.setStyle(
                "-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.85);" +
                        "-fx-background-color:rgba(255,255,255,0.15);" +
                        "-fx-background-radius:20;-fx-padding:3 10;");
        hInfo.getChildren().addAll(hTitre, hType);
        header.getChildren().addAll(ico, hInfo);

        VBox body = new VBox(16);
        body.setPadding(new Insets(20, 24, 20, 24));
        body.setStyle("-fx-background-color:#F8F9FD;");

        // Énoncé
        VBox enCard = new VBox(8);
        enCard.setStyle(
                "-fx-background-color:white;-fx-background-radius:12;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);-fx-padding:16;");
        Label enLbl = new Label("Énoncé");
        enLbl.setStyle(
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#8A8FA8;");
        Label enonce = new Label(q.getEnonce());
        enonce.setWrapText(true);
        enonce.setStyle("-fx-font-size:14px;-fx-text-fill:#1A1D2E;-fx-line-spacing:3;");
        enCard.getChildren().addAll(enLbl, enonce);

        // Infos test associé
        HBox infoRow = new HBox(12);
        infoRow.setStyle(
                "-fx-background-color:white;-fx-background-radius:12;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);-fx-padding:14 16;");
        Label testLbl = new Label("📝 Test associé");
        testLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#8A8FA8;-fx-font-weight:bold;");
        Label testVal = new Label(titreTest);
        testVal.setStyle(
                "-fx-font-size:13px;-fx-text-fill:#6C63FF;-fx-font-weight:bold;");
        infoRow.getChildren().addAll(testLbl, new Label("→") {{
            setStyle("-fx-text-fill:#C0C4D8;");
        }}, testVal);

        // Réponses
        VBox repCard = new VBox(10);
        repCard.setStyle(
                "-fx-background-color:white;-fx-background-radius:12;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);-fx-padding:16;");
        Label repTitre = new Label("✅ Réponses associées");
        repTitre.setStyle(
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1A1D2E;");
        repCard.getChildren().add(repTitre);

        try {
            List<Reponse> reponses = reponseService.recupererParQuestion(q.getId());
            if (reponses.isEmpty()) {
                Label aucune = new Label("Aucune réponse définie pour cette question.");
                aucune.setStyle("-fx-font-size:12px;-fx-text-fill:#8A8FA8;-fx-padding:6 0;");
                repCard.getChildren().add(aucune);
            } else {
                for (Reponse r : reponses) {
                    repCard.getChildren().add(creerLigneReponse(r));
                }
            }
        } catch (SQLException e) {
            repCard.getChildren().add(new Label("Erreur chargement réponses."));
        }

        body.getChildren().addAll(enCard, infoRow, repCard);
        root.getChildren().addAll(header, body);

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

    private HBox creerLigneReponse(Reponse r) {
        HBox ligne = new HBox(12);
        ligne.setAlignment(Pos.CENTER_LEFT);
        String bg = r.isCorrect()
                ? "-fx-background-color:#F0FDF4;-fx-border-color:#BBF7D0;"
                : "-fx-background-color:#FFF1F2;-fx-border-color:#FECDD3;";
        ligne.setStyle(bg +
                "-fx-background-radius:10;-fx-border-radius:10;" +
                "-fx-border-width:1;-fx-padding:10 14;");

        Label icone = new Label(r.isCorrect() ? "✅" : "❌");
        icone.setStyle("-fx-font-size:16px;");

        Label contenu = new Label(r.getContenuRep());
        contenu.setStyle(
                "-fx-font-size:12px;-fx-text-fill:" +
                        (r.isCorrect() ? "#059669" : "#E11D48") + ";");
        contenu.setWrapText(true);
        HBox.setHgrow(contenu, Priority.ALWAYS);

        Label score = new Label(r.getScore() + " pts");
        score.setStyle(
                "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-text-fill:" + (r.isCorrect() ? "#059669" : "#9CA3AF") + ";");

        ligne.getChildren().addAll(icone, contenu, score);
        return ligne;
    }

    // ── CRUD ──────────────────────────────────────────────────────────
    private void loadData() {
        try {
            allData = FXCollections.observableArrayList(service.recuperer());
            tableQuestions.setItems(allData);
            if (countLabel != null) countLabel.setText(allData.size() + " question(s)");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    @FXML private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) {
            tableQuestions.setItems(allData);
            countLabel.setText(allData.size() + " question(s)");
        } else {
            ObservableList<Question> filtered = allData.stream()
                    .filter(qu -> qu.getEnonce().toLowerCase().contains(q)
                            || qu.getType().toLowerCase().contains(q))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            tableQuestions.setItems(filtered);
            countLabel.setText(filtered.size() + " résultat(s)");
        }
    }

    @FXML private void handleAjouter() {
        selectedQuestion = null; clearForm(); cacherErreur();
        formTitle.setText("Nouvelle Question");
        formCard.setVisible(true); formCard.setManaged(true);
    }

    private void openEditForm(Question q) {
        selectedQuestion = q;
        fieldEnonce.setText(q.getEnonce());
        comboType.setValue(q.getType());
        fieldScoreMax.setText(String.valueOf(q.getScoreMax()));
        comboTest.getItems().stream()
                .filter(t -> t.getId() == q.getTestId())
                .findFirst().ifPresent(comboTest::setValue);
        cacherErreur();
        formTitle.setText("Modifier la Question");
        formCard.setVisible(true); formCard.setManaged(true);
    }

    @FXML private void handleSave() {
        String err = validateForm();
        if (err != null) { afficherErreur(err); return; }
        cacherErreur();
        try {
            int score  = Integer.parseInt(fieldScoreMax.getText().trim());
            int testId = comboTest.getValue().getId();
            if (selectedQuestion == null) {
                service.ajouter(new Question(
                        fieldEnonce.getText().trim(), comboType.getValue(), score, testId));
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Question ajoutée !");
            } else {
                selectedQuestion.setEnonce(fieldEnonce.getText().trim());
                selectedQuestion.setType(comboType.getValue());
                selectedQuestion.setScoreMax(score);
                selectedQuestion.setTestId(testId);
                service.modifier(selectedQuestion);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Question modifiée !");
            }
            handleCancel(); loadData();
        } catch (NumberFormatException e) {
            afficherErreur("Le score doit être un nombre entier.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    private void handleDelete(Question q) {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer cette question ?", ButtonType.YES, ButtonType.NO);
        c.setHeaderText(null);
        c.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { service.supprimer(q.getId()); loadData(); }
                catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
                }
            }
        });
    }

    @FXML private void handleCancel() {
        formCard.setVisible(false); formCard.setManaged(false);
        clearForm(); selectedQuestion = null;
    }

    private String validateForm() {
        String enonce = fieldEnonce.getText().trim();
        if (enonce.isBlank()) return "L'énoncé est obligatoire.";
        if (enonce.length() < 5) return "L'énoncé doit contenir au moins 5 caractères.";
        if (comboType.getValue() == null) return "Le type est obligatoire.";
        if (fieldScoreMax.getText().isBlank()) return "Le score maximum est obligatoire.";
        try {
            int s = Integer.parseInt(fieldScoreMax.getText().trim());
            if (s <= 0)   return "Le score doit être > 0.";
            if (s > 100)  return "Le score ne peut pas dépasser 100.";
        } catch (NumberFormatException e) { return "Le score doit être un entier."; }
        if (comboTest.getValue() == null) return "Veuillez sélectionner un test.";
        return null;
    }

    private void clearForm() {
        fieldEnonce.clear(); comboType.setValue(null);
        fieldScoreMax.clear(); comboTest.setValue(null);
    }

    private Button bouton(String texte, String bgNormal, String textNormal,
                          String bgHover, String textHover, String borderColor) {
        Button b = new Button(texte);
        String base =
                "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-background-radius:7;-fx-padding:5 12 5 12;-fx-cursor:hand;" +
                        "-fx-border-radius:7;-fx-border-width:1;";
        String normal = "-fx-background-color:" + bgNormal + ";" +
                "-fx-text-fill:" + textNormal + ";" +
                "-fx-border-color:" + borderColor + ";";
        String hover  = "-fx-background-color:" + bgHover + ";" +
                "-fx-text-fill:" + textHover + ";" +
                "-fx-border-color:" + borderColor + ";";
        b.setStyle(normal + base);
        b.setOnMouseEntered(e -> b.setStyle(hover + base));
        b.setOnMouseExited(e  -> b.setStyle(normal + base));
        return b;
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
    private void ajouterBoutonIA() {
        // Le bouton IA est géré via handleGenererIA() appelé depuis le FXML
    }

    @FXML
    private void handleGenererIA() {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("🤖 Générer des questions avec l'IA");
        dialog.setHeaderText(null);

        VBox root = new VBox(0);
        root.setPrefWidth(500);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color:linear-gradient(to right,#7C3AED,#6C63FF);" +
                        "-fx-padding:20 24;");
        Label ico = new Label("🤖");
        ico.setStyle("-fx-font-size:26px;");
        VBox hInfo = new VBox(3);
        Label hT = new Label("Générateur de questions par IA");
        hT.setStyle(
                "-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label hS = new Label("Groq LLaMA — QCM, Oral, Texte libre");
        hS.setStyle("-fx-font-size:11px;-fx-text-fill:rgba(255,255,255,0.75);");
        hInfo.getChildren().addAll(hT, hS);
        header.getChildren().addAll(ico, hInfo);

        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 24, 20, 24));
        body.setStyle("-fx-background-color:#F8F9FD;");

        // Thème
        VBox themeBox = new VBox(5);
        Label themeLbl = new Label("THÈME / SUJET *");
        themeLbl.setStyle(
                "-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:#9CA3AF;");
        TextField fieldTheme = new TextField();
        fieldTheme.setPromptText("Ex: conjugaison du présent, vocabulaire...");
        fieldTheme.setStyle(
                "-fx-background-color:white;-fx-border-color:#E5E7EB;" +
                        "-fx-border-radius:10;-fx-background-radius:10;" +
                        "-fx-padding:10 14;-fx-font-size:13px;");
        themeBox.getChildren().addAll(themeLbl, fieldTheme);

        // Type de question
        VBox typeBox = new VBox(5);
        Label typeLbl = new Label("TYPE DE QUESTION *");
        typeLbl.setStyle(
                "-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:#9CA3AF;");
        ComboBox<String> comboTypeQ = new ComboBox<>();
        comboTypeQ.getItems().addAll(
                "qcm — Choix multiple",
                "oral — Répétition orale",
                "texte_libre — Rédaction libre",
                "mixte — Mélange des 3 types"
        );
        comboTypeQ.setValue("qcm — Choix multiple");
        comboTypeQ.setMaxWidth(Double.MAX_VALUE);
        comboTypeQ.setStyle(
                "-fx-background-color:white;-fx-border-color:#E5E7EB;" +
                        "-fx-border-radius:10;-fx-background-radius:10;-fx-font-size:13px;");
        typeBox.getChildren().addAll(typeLbl, comboTypeQ);

        // Ligne Langue + Niveau + Nombre
        HBox row = new HBox(12);

        VBox langueBox = new VBox(5);
        Label langueLbl = new Label("LANGUE *");
        langueLbl.setStyle(
                "-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:#9CA3AF;");
        ComboBox<String> comboLangueIA = new ComboBox<>();
        comboLangueIA.getItems().addAll("Français", "English", "Español", "Deutsch");
        comboLangueIA.setValue("Français");
        comboLangueIA.setMaxWidth(Double.MAX_VALUE);
        comboLangueIA.setStyle(
                "-fx-background-color:white;-fx-border-color:#E5E7EB;" +
                        "-fx-border-radius:10;-fx-background-radius:10;-fx-font-size:13px;");
        langueBox.getChildren().addAll(langueLbl, comboLangueIA);
        HBox.setHgrow(langueBox, Priority.ALWAYS);

        VBox niveauBox = new VBox(5);
        Label niveauLbl = new Label("NIVEAU *");
        niveauLbl.setStyle(
                "-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:#9CA3AF;");
        ComboBox<String> comboNiveauIA = new ComboBox<>();
        comboNiveauIA.getItems().addAll("A1","A2","B1","B2","C1","C2");
        comboNiveauIA.setValue("A1");
        comboNiveauIA.setMaxWidth(Double.MAX_VALUE);
        comboNiveauIA.setStyle(
                "-fx-background-color:white;-fx-border-color:#E5E7EB;" +
                        "-fx-border-radius:10;-fx-background-radius:10;-fx-font-size:13px;");
        niveauBox.getChildren().addAll(niveauLbl, comboNiveauIA);
        HBox.setHgrow(niveauBox, Priority.ALWAYS);

        VBox nombreBox = new VBox(5);
        Label nombreLbl = new Label("NOMBRE");
        nombreLbl.setStyle(
                "-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:#9CA3AF;");
        Spinner<Integer> spinnerNombre = new Spinner<>(1, 10, 3);
        spinnerNombre.setStyle("-fx-font-size:13px;");
        spinnerNombre.setPrefWidth(80);
        nombreBox.getChildren().addAll(nombreLbl, spinnerNombre);

        row.getChildren().addAll(langueBox, niveauBox, nombreBox);

        // Test cible
        VBox testBox = new VBox(5);
        Label testLbl = new Label("AJOUTER AU TEST *");
        testLbl.setStyle(
                "-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:#9CA3AF;");
        ComboBox<Test> comboTestIA = new ComboBox<>();
        comboTestIA.getItems().addAll(comboTest.getItems());
        comboTestIA.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Test t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? "" : t.getTitre());
            }
        });
        comboTestIA.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Test t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null
                        ? "Sélectionner un test..." : t.getTitre());
            }
        });
        comboTestIA.setMaxWidth(Double.MAX_VALUE);
        comboTestIA.setStyle(
                "-fx-background-color:white;-fx-border-color:#E5E7EB;" +
                        "-fx-border-radius:10;-fx-background-radius:10;-fx-font-size:13px;");
        testBox.getChildren().addAll(testLbl, comboTestIA);

        body.getChildren().addAll(themeBox, typeBox, row, testBox);
        root.getChildren().addAll(header, body);

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setStyle(
                "-fx-background-color:#F8F9FD;-fx-padding:0;");

        ButtonType btnGenerer = new ButtonType(
                "🚀 Générer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(btnGenerer, ButtonType.CANCEL);

        Button btnG = (Button) dialog.getDialogPane().lookupButton(btnGenerer);
        btnG.setStyle(
                "-fx-background-color:linear-gradient(to right,#7C3AED,#6C63FF);" +
                        "-fx-text-fill:white;-fx-font-weight:bold;" +
                        "-fx-background-radius:10;-fx-padding:10 22;-fx-cursor:hand;");

        dialog.setResultConverter(bt -> {
            if (bt == btnGenerer) {
                Map<String, String> p = new HashMap<>();
                p.put("theme",  fieldTheme.getText().trim());
                // Extraire juste la clé avant " — "
                String typeVal = comboTypeQ.getValue();
                p.put("type",   typeVal.split(" — ")[0]);
                p.put("langue", comboLangueIA.getValue());
                p.put("niveau", comboNiveauIA.getValue());
                p.put("nombre", String.valueOf(spinnerNombre.getValue()));
                p.put("testId", comboTestIA.getValue() != null
                        ? String.valueOf(comboTestIA.getValue().getId()) : "");
                return p;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(params -> {
            if (params.get("theme").isEmpty()) {
                showAlert(Alert.AlertType.WARNING,
                        "Validation", "Le thème est obligatoire.");
                return;
            }
            if (params.get("testId").isEmpty()) {
                showAlert(Alert.AlertType.WARNING,
                        "Validation", "Sélectionnez un test cible.");
                return;
            }
            lancerGenerationIA(
                    params.get("theme"),
                    params.get("langue"),
                    params.get("niveau"),
                    Integer.parseInt(params.get("nombre")),
                    params.get("type"),
                    Integer.parseInt(params.get("testId"))
            );
        });
    }

    private void lancerGenerationIA(String theme, String langue,
                                    String niveau, int nombre, String type, int testId) {

        // Créer un Stage personnalisé pour la progression
        Stage progressStage = new Stage();
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.setTitle("⏳ Génération en cours...");
        progressStage.setResizable(false);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30, 50, 30, 50));
        root.setStyle("-fx-background-color: white; -fx-background-radius: 15;");

        // Icône animée
        Label iconLabel = new Label("🤖");
        iconLabel.setStyle("-fx-font-size: 48px;");

        // ProgressIndicator
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(50, 50);

        // Texte de statut
        Label statusLabel = new Label("L'IA génère " + nombre + " question(s)...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4A4D6A;");

        // Sous-texte
        Label subLabel = new Label("Thème : " + theme + " (" + langue + " — " + niveau + ")");
        subLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8A8FA8;");

        // Bouton Annuler
        Button cancelButton = new Button("Annuler");
        cancelButton.setStyle(
                "-fx-background-color: #EF4444; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 20; " +
                        "-fx-cursor: hand;");

        root.getChildren().addAll(iconLabel, progressIndicator, statusLabel, subLabel, cancelButton);

        Scene scene = new Scene(root);
        progressStage.setScene(scene);

        // Gérer l'annulation
        final Thread[] generationThread = new Thread[1];

        cancelButton.setOnAction(e -> {
            if (generationThread[0] != null && generationThread[0].isAlive()) {
                generationThread[0].interrupt();
            }
            progressStage.close();
        });

        progressStage.show();

        // Lancer la génération
        generationThread[0] = new Thread(() -> {
            try {
                List<AIQuizGeneratorService.QuestionGeneree> questions =
                        aiQuizService.generer(theme, langue, niveau, nombre, type);

                Platform.runLater(() -> {
                    progressStage.close();
                    afficherPreviewQuestions(questions, testId, theme);
                });

            } catch (Exception e) {
                LoggerUtil.error("Erreur pendant la génération IA", e);
                Platform.runLater(() -> {
                    progressStage.close();
                    showAlert(Alert.AlertType.ERROR,
                            "Erreur",
                            "Erreur lors de la génération : " + e.getMessage());
                });
            }
        });

        generationThread[0].setDaemon(true);
        generationThread[0].start();
    }

    private void afficherPreviewQuestions(
            List<AIQuizGeneratorService.QuestionGeneree> questions,
            int testId, String theme) {

        if (questions.isEmpty()) {
            showAlert(Alert.AlertType.ERROR,
                    "Erreur", "Aucune question générée. Réessayez.");
            return;
        }

        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("✅ Prévisualisation — " + questions.size() + " question(s)");
        dialog.setHeaderText(null);

        VBox root = new VBox(0);
        root.setPrefWidth(660);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
                "-fx-background-color:linear-gradient(to right,#059669,#10B981);" +
                        "-fx-padding:16 24;");
        Label hT = new Label("✅ " + questions.size()
                + " questions générées sur « " + theme + " »");
        hT.setStyle(
                "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;");
        header.getChildren().add(hT);

        VBox listeQ = new VBox(10);
        listeQ.setStyle("-fx-padding:16 20;-fx-background-color:#F8F9FD;");

        for (int i = 0; i < questions.size(); i++) {
            AIQuizGeneratorService.QuestionGeneree q = questions.get(i);
            VBox qCard = new VBox(8);

            // Couleur selon type
            String borderColor = switch (q.type()) {
                case "oral"         -> "#BBF7D0";
                case "texte_libre"  -> "#FDE68A";
                default             -> "#DDD6FE";
            };
            String bgColor = switch (q.type()) {
                case "oral"         -> "#F0FDF4";
                case "texte_libre"  -> "#FFFBEB";
                default             -> "#F5F3FF";
            };
            qCard.setStyle(
                    "-fx-background-color:white;-fx-background-radius:10;" +
                            "-fx-border-color:" + borderColor + ";-fx-border-radius:10;" +
                            "-fx-border-width:1.5;-fx-padding:12 14;");

            HBox qHead = new HBox(8);
            qHead.setAlignment(Pos.CENTER_LEFT);

            Label numLbl = new Label("Q" + (i + 1));
            numLbl.setStyle(
                    "-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:#7C3AED;" +
                            "-fx-background-color:#F5F3FF;-fx-background-radius:20;" +
                            "-fx-padding:2 7;");

            String typeIcon = switch (q.type()) {
                case "oral"        -> "🎤";
                case "texte_libre" -> "✍️";
                default            -> "🔘";
            };
            Label typeLabel = new Label(typeIcon + " " + q.type().toUpperCase()
                    + " — " + q.scoreMax() + " pts");
            typeLabel.setStyle(
                    "-fx-font-size:10px;-fx-font-weight:bold;" +
                            "-fx-background-color:" + bgColor + ";" +
                            "-fx-background-radius:20;-fx-padding:2 8;" +
                            "-fx-text-fill:" + (q.type().equals("oral") ? "#059669"
                            : q.type().equals("texte_libre") ? "#D97706" : "#7C3AED") + ";");

            Label enonceLbl = new Label(q.enonce());
            enonceLbl.setWrapText(true);
            enonceLbl.setStyle(
                    "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1A1D2E;");
            HBox.setHgrow(enonceLbl, Priority.ALWAYS);
            qHead.getChildren().addAll(numLbl, typeLabel, enonceLbl);

            qCard.getChildren().add(qHead);

            // Afficher les réponses pour QCM seulement
            if ("qcm".equals(q.type()) && !q.reponses().isEmpty()) {
                VBox repsBox = new VBox(3);
                repsBox.setStyle("-fx-padding:4 0 0 16;");
                for (AIQuizGeneratorService.ReponseGeneree r : q.reponses()) {
                    Label rLbl = new Label(
                            (r.isCorrecte() ? "✅ " : "○  ") + r.contenu());
                    rLbl.setStyle(
                            "-fx-font-size:12px;-fx-text-fill:" +
                                    (r.isCorrecte() ? "#059669" : "#6B7280") + ";" +
                                    (r.isCorrecte() ? "-fx-font-weight:bold;" : ""));
                    repsBox.getChildren().add(rLbl);
                }
                qCard.getChildren().add(repsBox);
            } else if (!"qcm".equals(q.type())) {
                Label note = new Label(
                        q.type().equals("oral")
                                ? "💡 L'étudiant devra répéter cette phrase à voix haute"
                                : "💡 L'étudiant rédigera librement (corrigé par l'IA)");
                note.setStyle("-fx-font-size:11px;-fx-text-fill:#9CA3AF;-fx-padding:4 0 0 0;");
                qCard.getChildren().add(note);
            }

            listeQ.getChildren().add(qCard);
        }

        ScrollPane scroll = new ScrollPane(listeQ);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(420);
        scroll.setStyle(
                "-fx-background:transparent;-fx-border-color:transparent;");

        root.getChildren().addAll(header, scroll);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setStyle(
                "-fx-background-color:#F8F9FD;-fx-padding:0;");

        ButtonType btnInserer = new ButtonType(
                "💾 Insérer en BD", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(btnInserer, ButtonType.CANCEL);
        Button btnI = (Button) dialog.getDialogPane().lookupButton(btnInserer);
        btnI.setStyle(
                "-fx-background-color:linear-gradient(to right,#059669,#10B981);" +
                        "-fx-text-fill:white;-fx-font-weight:bold;" +
                        "-fx-background-radius:10;-fx-padding:10 22;-fx-cursor:hand;");

        dialog.setResultConverter(bt -> bt == btnInserer);
        dialog.showAndWait().ifPresent(ok -> {
            if (ok) insererQuestionsEnBD(questions, testId);
        });
    }

    private void insererQuestionsEnBD(
            List<AIQuizGeneratorService.QuestionGeneree> questions, int testId) {
        int success = 0, errors = 0;
        for (AIQuizGeneratorService.QuestionGeneree qg : questions) {
            try {
                service.ajouter(
                        new Question(qg.enonce(), qg.type(), qg.scoreMax(), testId));

                // Récupérer la dernière question insérée
                List<Question> toutes = service.recupererParTest(testId);
                Question derniere = toutes.get(toutes.size() - 1);

                // Insérer les réponses seulement pour QCM
                if ("qcm".equals(qg.type())) {
                    for (AIQuizGeneratorService.ReponseGeneree rg : qg.reponses()) {
                        reponseService.ajouter(
                                new com.example.pijava_fluently.entites.Reponse(
                                        rg.contenu(), rg.isCorrecte(),
                                        rg.isCorrecte() ? qg.scoreMax() : 0,
                                        java.time.LocalDate.now(),
                                        derniere.getId()
                                )
                        );
                    }
                }
                success++;
            } catch (Exception e) {
                LoggerUtil.error("Erreur insertion question IA", e);
                errors++;
            }
        }
        final int s = success, err = errors;
        Platform.runLater(() -> {
            showAlert(err == 0 ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING,
                    err == 0 ? "✅ Succès" : "⚠️ Partiel",
                    s + " question(s) insérée(s)" +
                            (err > 0 ? ", " + err + " erreur(s)." : " !"));
            loadData();
        });
    }
}