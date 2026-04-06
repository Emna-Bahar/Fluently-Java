package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Question;
import com.example.pijava_fluently.entites.Reponse;
import com.example.pijava_fluently.services.QuestionService;
import com.example.pijava_fluently.services.ReponseService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class ReponseController {

    @FXML private VBox formCard;
    @FXML private Label formTitle;
    @FXML private TextField fieldContenu;
    @FXML private CheckBox checkCorrect;
    @FXML private TextField fieldScore;
    @FXML private ComboBox<Question> comboQuestion;
    @FXML private Label countLabel;
    @FXML private TextField searchField;
    @FXML private Label labelErreur;
    @FXML private TableView<Reponse> tableReponses;
    @FXML private TableColumn<Reponse, Integer>   colId;
    @FXML private TableColumn<Reponse, String>    colContenu;
    @FXML private TableColumn<Reponse, Boolean>   colCorrect;
    @FXML private TableColumn<Reponse, Integer>   colScore;
    @FXML private TableColumn<Reponse, LocalDate> colDate;
    @FXML private TableColumn<Reponse, Integer>   colQuestion;
    @FXML private TableColumn<Reponse, Void>      colActions;

    private final ReponseService   service          = new ReponseService();
    private final QuestionService  questionService  = new QuestionService();
    private ObservableList<Reponse> allData = FXCollections.observableArrayList();
    private Reponse selectedReponse = null;

    @FXML
    public void initialize() {
        try {
            List<Question> questions = questionService.recuperer();
            comboQuestion.setItems(FXCollections.observableArrayList(questions));
        } catch (SQLException e) { e.printStackTrace(); }
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colContenu.setCellValueFactory(new PropertyValueFactory<>("contenuRep"));
        colContenu.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                String txt = item.length() > 55 ? item.substring(0, 52) + "…" : item;
                setText(txt);
                setTooltip(new Tooltip(item));
            }
        });

        colCorrect.setCellValueFactory(new PropertyValueFactory<>("correct"));
        colCorrect.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean val, boolean empty) {
                super.updateItem(val, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || val == null) { setGraphic(null); return; }
                Label badge = new Label(val ? "✔ Correcte" : "✘ Incorrecte");
                badge.setStyle(val
                        ? "-fx-background-color:#ECFDF5;-fx-text-fill:#059669;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 10;"
                        : "-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 10;"
                );
                setGraphic(badge); setText(null);
            }
        });

        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));
        colScore.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) { setText(null); return; }
                setText(item + " pts");
                setStyle("-fx-font-weight:bold;-fx-text-fill:#6C63FF;");
            }
        });

        colDate.setCellValueFactory(new PropertyValueFactory<>("dateReponse"));
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                setText(empty || item == null ? "—" : item.toString());
                setStyle("-fx-text-fill:#6B7280;-fx-font-size:12px;");
            }
        });

        colQuestion.setCellValueFactory(new PropertyValueFactory<>("questionId"));
        colQuestion.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                setText(empty || item == null ? "—" : "Q#" + item);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✎ Modifier");
            private final Button btnDelete = new Button("🗑 Supprimer");
            private final HBox   box       = new HBox(8, btnEdit, btnDelete);
            {
                box.setAlignment(javafx.geometry.Pos.CENTER);
                btnEdit.setStyle("-fx-background-color:#F5F3FF;-fx-text-fill:#7C3AED;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:7;-fx-padding:5 12;-fx-cursor:hand;");
                btnDelete.setStyle("-fx-background-color:#FFF1F2;-fx-text-fill:#E11D48;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:7;-fx-padding:5 12;-fx-cursor:hand;");
                btnEdit.setOnAction(e   -> openEditForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

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
        selectedReponse = null;
        clearForm();
        formTitle.setText("Nouvelle Réponse");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    private void openEditForm(Reponse r) {
        selectedReponse = r;
        fieldContenu.setText(r.getContenuRep());
        checkCorrect.setSelected(r.isCorrect());
        fieldScore.setText(String.valueOf(r.getScore()));
        comboQuestion.getItems().stream()
                .filter(q -> q.getId() == r.getQuestionId())
                .findFirst()
                .ifPresent(comboQuestion::setValue);
        formTitle.setText("Modifier la Réponse");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    @FXML private void handleSave() {
        String erreur = validateForm();
        if (erreur != null) {
            afficherErreur(erreur);
            return;
        }
        cacherErreur();
        try {
            int score      = Integer.parseInt(fieldScore.getText().trim());
            int questionId = comboQuestion.getValue().getId();

            if (selectedReponse == null) {
                // dateReponse = aujourd'hui (gérée automatiquement)
                service.ajouter(new Reponse(
                        fieldContenu.getText().trim(),
                        checkCorrect.isSelected(),
                        score,
                        java.time.LocalDate.now(),
                        questionId));
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Réponse ajoutée !");
            } else {
                selectedReponse.setContenuRep(fieldContenu.getText().trim());
                selectedReponse.setCorrect(checkCorrect.isSelected());
                selectedReponse.setScore(score);
                selectedReponse.setQuestionId(questionId);
                // On ne change pas la dateReponse lors d'une modification
                service.modifier(selectedReponse);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "✅ Réponse modifiée !");
            }
            handleCancel();
            loadData();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Format invalide",
                    "⚠ Le score doit être un nombre entier valide.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    private void handleDelete(Reponse r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer cette réponse ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try { service.supprimer(r.getId()); loadData(); }
                catch (SQLException e) { showAlert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage()); }
            }
        });
    }

    @FXML private void handleCancel() {
        formCard.setVisible(false);
        formCard.setManaged(false);
        clearForm();
        selectedReponse = null;
    }

    private String validateForm() {
        // 1. Contenu obligatoire, min 1 caractère
        String contenu = fieldContenu.getText().trim();
        if (contenu.isBlank())
            return "⚠ Le contenu de la réponse est obligatoire.";
        if (contenu.length() < 1)
            return "⚠ Le contenu ne peut pas être vide.";
        if (contenu.length() > 500)
            return "⚠ Le contenu ne peut pas dépasser 500 caractères.";

        // 2. Score : obligatoire, entier, > 0
        if (fieldScore.getText().isBlank())
            return "⚠ Le score est obligatoire.";
        int score;
        try {
            score = Integer.parseInt(fieldScore.getText().trim());
        } catch (NumberFormatException e) {
            return "⚠ Le score doit être un nombre entier (ex: 2).";
        }
        if (score <= 0)
            return "⚠ Le score doit être supérieur à 0.";
        if (score > 100)
            return "⚠ Le score ne peut pas dépasser 100.";

        // 3. Question associée obligatoire
        if (comboQuestion.getValue() == null)
            return "⚠ Veuillez sélectionner une question associée.";

        // 4. Vérifier cohérence : le score de la réponse ne doit pas
        //    dépasser le scoreMax de la question
        int scoreMaxQuestion = comboQuestion.getValue().getScoreMax();
        if (score > scoreMaxQuestion)
            return "⚠ Le score (" + score + ") ne peut pas dépasser le score max "
                    + "de la question (" + scoreMaxQuestion + ").";

        return null; // tout est OK
    }

    private void clearForm() {
        fieldContenu.clear();
        checkCorrect.setSelected(false);
        fieldScore.clear();
        comboQuestion.setValue(null);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }
    private void afficherErreur(String message) {
        if (labelErreur != null) {
            labelErreur.setText("⚠ " + message);
            labelErreur.setVisible(true);
            labelErreur.setManaged(true);
        } else {
            showAlert(Alert.AlertType.WARNING, "Validation", message);
        }
    }

    private void cacherErreur() {
        if (labelErreur != null) {
            labelErreur.setVisible(false);
            labelErreur.setManaged(false);
        }
    }
}