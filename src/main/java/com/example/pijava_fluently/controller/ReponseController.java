package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Question;
import com.example.pijava_fluently.entites.Reponse;
import com.example.pijava_fluently.services.QuestionService;
import com.example.pijava_fluently.services.ReponseService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class ReponseController {

    @FXML private VBox            formCard;
    @FXML private Label           formTitle;
    @FXML private TextField       fieldContenu;
    @FXML private CheckBox        checkCorrect;
    @FXML private TextField       fieldScore;
    @FXML private ComboBox<Question> comboQuestion;
    @FXML private Label           countLabel;
    @FXML private TextField       searchField;
    @FXML private Label           labelErreur;

    @FXML private TableView<Reponse>            tableReponses;
    @FXML private TableColumn<Reponse, Integer> colId;
    @FXML private TableColumn<Reponse, String>  colContenu;
    @FXML private TableColumn<Reponse, Boolean> colCorrect;
    @FXML private TableColumn<Reponse, Integer> colScore;
    @FXML private TableColumn<Reponse, LocalDate> colDate;
    @FXML private TableColumn<Reponse, Integer> colQuestion;
    @FXML private TableColumn<Reponse, Void>    colActions;

    private final ReponseService  service         = new ReponseService();
    private final QuestionService questionService = new QuestionService();
    private ObservableList<Reponse> allData       = FXCollections.observableArrayList();
    private Reponse selectedReponse               = null;

    @FXML
    public void initialize() {
        try {
            List<Question> questions = questionService.recuperer();
            comboQuestion.setItems(FXCollections.observableArrayList(questions));
            comboQuestion.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Question q, boolean empty) {
                    super.updateItem(q, empty);
                    if (empty || q == null) { setText(null); return; }
                    String txt = q.getEnonce().length() > 50
                            ? q.getEnonce().substring(0, 47) + "…" : q.getEnonce();
                    setText("Q" + q.getId() + " — " + txt);
                }
            });
            comboQuestion.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Question q, boolean empty) {
                    super.updateItem(q, empty);
                    if (empty || q == null) {
                        setText("— Sélectionner —"); return;
                    }
                    String txt = q.getEnonce().length() > 40
                            ? q.getEnonce().substring(0, 37) + "…" : q.getEnonce();
                    setText(txt);
                }
            });
        } catch (SQLException e) { e.printStackTrace(); }
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        if (colId != null) colId.setVisible(false);

        colContenu.setCellValueFactory(new PropertyValueFactory<>("contenuRep"));
        colContenu.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                String txt = item.length() > 55 ? item.substring(0, 52) + "…" : item;
                Label lbl = new Label(txt);
                lbl.setStyle("-fx-font-size:12px;-fx-text-fill:#1A1D2E;");
                lbl.setTooltip(new Tooltip(item));
                setGraphic(lbl); setText(null);
            }
        });

        colCorrect.setCellValueFactory(new PropertyValueFactory<>("correct"));
        colCorrect.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean val, boolean empty) {
                super.updateItem(val, empty);
                setAlignment(Pos.CENTER);
                if (empty || val == null) { setGraphic(null); return; }
                Label badge = new Label(val ? "✔ Correcte" : "✘ Incorrecte");
                badge.setStyle(val
                        ? "-fx-background-color:#ECFDF5;-fx-text-fill:#059669;" +
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-background-radius:20;-fx-padding:4 10;"
                        : "-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;" +
                        "-fx-font-size:11px;-fx-font-weight:bold;" +
                        "-fx-background-radius:20;-fx-padding:4 10;");
                setGraphic(badge); setText(null);
            }
        });

        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
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

        colDate.setCellValueFactory(new PropertyValueFactory<>("dateReponse"));
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                setText(empty || item == null ? "—" : item.toString());
                setStyle("-fx-text-fill:#6B7280;-fx-font-size:12px;");
            }
        });

        colQuestion.setCellValueFactory(new PropertyValueFactory<>("questionId"));
        colQuestion.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                if (empty || item == null) { setText("—"); setGraphic(null); return; }
                String enonce = comboQuestion.getItems().stream()
                        .filter(q -> q.getId() == item)
                        .map(q -> q.getEnonce().length() > 30
                                ? q.getEnonce().substring(0, 27) + "…" : q.getEnonce())
                        .findFirst().orElse("Q#" + item);
                Label lbl = new Label(enonce);
                lbl.setStyle(
                        "-fx-font-size:11px;-fx-text-fill:#4A4D6A;" +
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

        tableReponses.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Reponse item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setStyle("");
                else if (getIndex() % 2 == 0) setStyle("-fx-background-color:#FAFBFF;");
                else setStyle("-fx-background-color:white;");
            }
        });
    }

    // ── Détails de la réponse ─────────────────────────────────────────
    private void afficherDetails(Reponse r) {
        String enonceQ = comboQuestion.getItems().stream()
                .filter(q -> q.getId() == r.getQuestionId())
                .map(Question::getEnonce)
                .findFirst().orElse("Question #" + r.getQuestionId());

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails — Réponse #" + r.getId());
        dialog.setHeaderText(null);

        VBox root = new VBox(0);
        root.setPrefWidth(520);

        // Header
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        String headerBg = r.isCorrect()
                ? "-fx-background-color:linear-gradient(to right,#059669,#10B981);"
                : "-fx-background-color:linear-gradient(to right,#E11D48,#F43F5E);";
        header.setStyle(headerBg + "-fx-padding:20 24;");
        Label ico = new Label(r.isCorrect() ? "✅" : "❌");
        ico.setStyle("-fx-font-size:28px;");
        VBox hInfo = new VBox(5);
        Label hTitre = new Label(r.isCorrect() ? "Réponse correcte" : "Réponse incorrecte");
        hTitre.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:white;");
        Label hScore = new Label("Score : " + r.getScore() + " pts");
        hScore.setStyle(
                "-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.85);" +
                        "-fx-background-color:rgba(255,255,255,0.15);" +
                        "-fx-background-radius:20;-fx-padding:3 10;");
        hInfo.getChildren().addAll(hTitre, hScore);
        header.getChildren().addAll(ico, hInfo);

        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 24, 20, 24));
        body.setStyle("-fx-background-color:#F8F9FD;");

        // Contenu de la réponse
        VBox contenuCard = new VBox(8);
        contenuCard.setStyle(
                "-fx-background-color:white;-fx-background-radius:12;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);-fx-padding:16;");
        Label contenuLbl = new Label("Contenu de la réponse");
        contenuLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#8A8FA8;");
        Label contenuVal = new Label(r.getContenuRep());
        contenuVal.setWrapText(true);
        contenuVal.setStyle(
                "-fx-font-size:14px;-fx-text-fill:#1A1D2E;-fx-line-spacing:3;");
        contenuCard.getChildren().addAll(contenuLbl, contenuVal);

        // Question associée
        VBox qCard = new VBox(8);
        qCard.setStyle(
                "-fx-background-color:white;-fx-background-radius:12;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);-fx-padding:16;");
        Label qLbl = new Label("❓ Question associée");
        qLbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#8A8FA8;");
        Label qVal = new Label(enonceQ);
        qVal.setWrapText(true);
        qVal.setStyle("-fx-font-size:13px;-fx-text-fill:#4A4D6A;-fx-line-spacing:2;");
        qCard.getChildren().addAll(qLbl, qVal);

        // Infos complémentaires
        HBox infoRow = new HBox(24);
        infoRow.setStyle(
                "-fx-background-color:white;-fx-background-radius:12;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);-fx-padding:14 16;");
        infoRow.setAlignment(Pos.CENTER_LEFT);

        VBox infoDate = new VBox(3);
        Label dateLbl = new Label("📅 Date");
        dateLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#8A8FA8;-fx-font-weight:bold;");
        Label dateVal = new Label(r.getDateReponse() != null
                ? r.getDateReponse().toString() : "—");
        dateVal.setStyle("-fx-font-size:13px;-fx-text-fill:#1A1D2E;-fx-font-weight:bold;");
        infoDate.getChildren().addAll(dateLbl, dateVal);

        VBox infoScore = new VBox(3);
        Label scoreLbl2 = new Label("🏆 Score");
        scoreLbl2.setStyle("-fx-font-size:10px;-fx-text-fill:#8A8FA8;-fx-font-weight:bold;");
        Label scoreVal2 = new Label(r.getScore() + " points");
        scoreVal2.setStyle(
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#6C63FF;");
        infoScore.getChildren().addAll(scoreLbl2, scoreVal2);

        VBox infoStatut = new VBox(3);
        Label statutLbl = new Label("✔ Statut");
        statutLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#8A8FA8;-fx-font-weight:bold;");
        Label statutVal = new Label(r.isCorrect() ? "Correcte" : "Incorrecte");
        statutVal.setStyle(
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" +
                        (r.isCorrect() ? "#059669" : "#E11D48") + ";");
        infoStatut.getChildren().addAll(statutLbl, statutVal);

        infoRow.getChildren().addAll(infoDate, new Separator() {{
            setOrientation(javafx.geometry.Orientation.VERTICAL);
        }}, infoScore, new Separator() {{
            setOrientation(javafx.geometry.Orientation.VERTICAL);
        }}, infoStatut);

        body.getChildren().addAll(contenuCard, qCard, infoRow);
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

    // ── CRUD ──────────────────────────────────────────────────────────
    private void loadData() {
        try {
            allData = FXCollections.observableArrayList(service.recuperer());
            tableReponses.setItems(allData);
            if (countLabel != null) countLabel.setText(allData.size() + " réponse(s)");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    @FXML private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) {
            tableReponses.setItems(allData);
            countLabel.setText(allData.size() + " réponse(s)");
        } else {
            ObservableList<Reponse> filtered = allData.stream()
                    .filter(r -> r.getContenuRep().toLowerCase().contains(q))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
            tableReponses.setItems(filtered);
            countLabel.setText(filtered.size() + " résultat(s)");
        }
    }

    @FXML private void handleAjouter() {
        selectedReponse = null; clearForm(); cacherErreur();
        formTitle.setText("Nouvelle Réponse");
        formCard.setVisible(true); formCard.setManaged(true);
    }

    private void openEditForm(Reponse r) {
        selectedReponse = r;
        fieldContenu.setText(r.getContenuRep());
        checkCorrect.setSelected(r.isCorrect());
        fieldScore.setText(String.valueOf(r.getScore()));
        comboQuestion.getItems().stream()
                .filter(q -> q.getId() == r.getQuestionId())
                .findFirst().ifPresent(comboQuestion::setValue);
        cacherErreur();
        formTitle.setText("Modifier la Réponse");
        formCard.setVisible(true); formCard.setManaged(true);
    }

    @FXML private void handleSave() {
        String err = validateForm();
        if (err != null) { afficherErreur(err); return; }
        cacherErreur();
        try {
            int score      = Integer.parseInt(fieldScore.getText().trim());
            int questionId = comboQuestion.getValue().getId();
            if (selectedReponse == null) {
                service.ajouter(new Reponse(
                        fieldContenu.getText().trim(), checkCorrect.isSelected(),
                        score, LocalDate.now(), questionId));
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Réponse ajoutée !");
            } else {
                selectedReponse.setContenuRep(fieldContenu.getText().trim());
                selectedReponse.setCorrect(checkCorrect.isSelected());
                selectedReponse.setScore(score);
                selectedReponse.setQuestionId(questionId);
                service.modifier(selectedReponse);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Réponse modifiée !");
            }
            handleCancel(); loadData();
        } catch (NumberFormatException e) {
            afficherErreur("Le score doit être un nombre entier.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    private void handleDelete(Reponse r) {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer cette réponse ?", ButtonType.YES, ButtonType.NO);
        c.setHeaderText(null);
        c.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { service.supprimer(r.getId()); loadData(); }
                catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
                }
            }
        });
    }

    @FXML private void handleCancel() {
        formCard.setVisible(false); formCard.setManaged(false);
        clearForm(); selectedReponse = null;
    }

    private String validateForm() {
        String c = fieldContenu.getText().trim();
        if (c.isBlank()) return "Le contenu est obligatoire.";
        if (c.length() > 500) return "Le contenu ne peut pas dépasser 500 caractères.";
        if (fieldScore.getText().isBlank()) return "Le score est obligatoire.";
        try {
            int s = Integer.parseInt(fieldScore.getText().trim());
            if (s <= 0)  return "Le score doit être > 0.";
            if (s > 100) return "Le score ne peut pas dépasser 100.";
            if (comboQuestion.getValue() != null
                    && s > comboQuestion.getValue().getScoreMax())
                return "Le score (" + s + ") dépasse le max de la question ("
                        + comboQuestion.getValue().getScoreMax() + ").";
        } catch (NumberFormatException e) { return "Le score doit être un entier."; }
        if (comboQuestion.getValue() == null) return "Sélectionnez une question.";
        return null;
    }

    private void clearForm() {
        fieldContenu.clear(); checkCorrect.setSelected(false);
        fieldScore.clear(); comboQuestion.setValue(null);
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
}