package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Objectif;
import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.ObjectifService;
import com.example.pijava_fluently.services.TacheService;
import com.example.pijava_fluently.utils.MyDatabase;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ObjectifController {

    @FXML private VBox      formCard;
    @FXML private Label     formTitle;
    @FXML private Label     formTitleIcon;
    @FXML private TextField fieldTitre;
    @FXML private TextArea  fieldDescription;
    @FXML private DatePicker fieldDateDeb;
    @FXML private DatePicker fieldDateFin;
    @FXML private ComboBox<String> comboStatut;
    @FXML private ComboBox<String> comboUser;

    @FXML private Label errTitre;
    @FXML private Label errDescription;
    @FXML private Label errDateDeb;
    @FXML private Label errDateFin;
    @FXML private Label errStatut;
    @FXML private Label errUser;

    @FXML private FlowPane  cardsContainer;
    @FXML private TextField searchField;
    @FXML private Label     countLabel;

    private final ObjectifService service      = new ObjectifService();
    private final TacheService    tacheService  = new TacheService();
    private ObservableList<Objectif> allData   = FXCollections.observableArrayList();
    private Objectif selectedObjectif          = null;
    private HomeController homeController;
    private User currentUser;

    private final Map<String, Integer> userMap = new LinkedHashMap<>();

    private static final String[] STATUTS = {"En cours", "Terminé", "En pause", "Annulé"};
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[][] CARD_COLORS = {
            {"#6C63FF","#8B5CF6"},{"#3B82F6","#2563EB"},{"#10B981","#059669"},
            {"#F59E0B","#D97706"},{"#EF4444","#DC2626"},{"#8B5CF6","#7C3AED"},
            {"#06B6D4","#0891B2"},{"#EC4899","#DB2777"},
    };

    public void setHomeController(HomeController hc) { this.homeController = hc; }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            loadUsersForCurrentUser();
            loadData();
        }
    }

    @FXML
    public void initialize() {
        comboStatut.setItems(FXCollections.observableArrayList(STATUTS));
        setupLiveValidation();
    }

    private void setupLiveValidation() {
        fieldTitre.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.trim().isEmpty()) validateTitre(val.trim());
        });

        fieldDescription.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.trim().isEmpty()) validateDescription(val.trim());
        });

        fieldDateDeb.valueProperty().addListener((obs, old, val) -> {
            if (val != null && fieldDateFin.getValue() != null) validateDates();
        });

        fieldDateFin.valueProperty().addListener((obs, old, val) -> {
            if (val != null && fieldDateDeb.getValue() != null) validateDates();
        });

        comboStatut.valueProperty().addListener((obs, old, val) -> {
            if (val != null && !val.isEmpty()) {
                clearError(errStatut);
                comboStatut.setStyle(comboStatut.getStyle().replace("-fx-border-color:#E11D48;", "") + "-fx-border-color:#E2E8F0;");
            }
        });

        comboUser.valueProperty().addListener((obs, old, val) -> {
            if (val != null && !val.isEmpty()) {
                clearError(errUser);
                comboUser.setStyle(comboUser.getStyle().replace("-fx-border-color:#E11D48;", "") + "-fx-border-color:#E2E8F0;");
            }
        });
    }

    private void loadUsersForCurrentUser() {
        userMap.clear();

        if (currentUser == null) {
            try {
                Connection cnx = MyDatabase.getInstance().getConnection();
                ResultSet rs = cnx.createStatement().executeQuery(
                        "SELECT id, nom, prenom FROM user ORDER BY nom, prenom"
                );
                while (rs.next()) {
                    String label = rs.getString("nom") + " " + rs.getString("prenom");
                    userMap.put(label, rs.getInt("id"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            comboUser.setItems(FXCollections.observableArrayList(userMap.keySet()));
            comboUser.setPromptText("Sélectionner un utilisateur…");
            comboUser.setDisable(false);
            return;
        }

        String label = currentUser.getNom() + " " + currentUser.getPrenom();
        userMap.put(label, currentUser.getId());
        comboUser.setItems(FXCollections.observableArrayList(label));
        comboUser.setValue(label);
        comboUser.setDisable(true);
        comboUser.setStyle("-fx-opacity:0.8;");
    }

    private void loadData() {
        try {
            allData = FXCollections.observableArrayList(service.recuperer());
            renderCards(allData);
            updateCountLabel(allData.size());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void renderCards(List<Objectif> list) {
        cardsContainer.getChildren().clear();
        int i = 0;
        for (Objectif o : list) {
            cardsContainer.getChildren().add(buildCard(o, i++ % CARD_COLORS.length));
        }
        if (list.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(60));
            Label icon = new Label("📭");
            icon.setStyle("-fx-font-size:48px;");
            Label msg = new Label("Aucun objectif trouvé");
            msg.setStyle("-fx-font-size:15px;-fx-text-fill:#9CA3AF;");
            emptyBox.getChildren().addAll(icon, msg);
            cardsContainer.getChildren().add(emptyBox);
        }
    }

    private VBox buildCard(Objectif o, int colorIdx) {
        String c1 = CARD_COLORS[colorIdx][0], c2 = CARD_COLORS[colorIdx][1];
        int nbTaches = 0;
        try { nbTaches = tacheService.recupererParObjectif(o.getId()).size(); } catch (SQLException ignored) {}

        VBox card = new VBox(0);
        card.setPrefWidth(295);
        card.setMaxWidth(295);
        card.setStyle("-fx-background-color:#FFFFFF;-fx-background-radius:18;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),20,0,0,5);" +
                "-fx-cursor:hand;");

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),25,0,0,8);-fx-scale-x:1.02;-fx-scale-y:1.02;"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),25,0,0,8);-fx-scale-x:1.02;-fx-scale-y:1.02;", "") + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),20,0,0,5);"));

        VBox header = new VBox(8);
        header.setPadding(new Insets(20,20,16,20));
        header.setStyle("-fx-background-color:linear-gradient(to bottom right,"+c1+","+c2+");-fx-background-radius:18 18 0 0;");

        HBox headerTop = new HBox(10);
        headerTop.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(getStatutIcon(o.getStatut()));
        iconLbl.setStyle("-fx-font-size:18px;-fx-background-color:rgba(255,255,255,0.22);-fx-background-radius:50;-fx-padding:6 8 6 8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label tachesCount = new Label("📋 "+nbTaches+" tâche"+(nbTaches>1?"s":""));
        tachesCount.setStyle("-fx-background-color:rgba(255,255,255,0.22);-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 9 3 9;");
        headerTop.getChildren().addAll(iconLbl, spacer, tachesCount);

        Label titreLabel = new Label(o.getTitre()!=null?o.getTitre():"Sans titre");
        titreLabel.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        titreLabel.setWrapText(true);
        header.getChildren().addAll(headerTop, titreLabel);

        VBox body = new VBox(10);
        body.setPadding(new Insets(14,18,10,18));

        String desc = o.getDescription()!=null && !o.getDescription().isBlank()
                ? (o.getDescription().length()>75?o.getDescription().substring(0,72)+"…":o.getDescription())
                : "Aucune description.";
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size:12px;-fx-text-fill:#6B7280;-fx-wrap-text:true;");
        descLabel.setWrapText(true);

        HBox datesBox = new HBox(10);
        datesBox.setAlignment(Pos.CENTER_LEFT);
        datesBox.getChildren().addAll(dateBadge("📅 Début",o.getDateDeb(),"#EFF6FF","#3B82F6"),
                dateBadge("🏁 Fin",o.getDateFin(),"#FFF7ED","#EA580C"));

        Label statutBadge = buildStatutBadge(o.getStatut());
        String nomUser = getUserLabel(o.getIdUserId());
        Label userBadge = new Label("👤  " + nomUser);
        userBadge.setStyle("-fx-background-color:#F0FDF4;-fx-text-fill:#16A34A;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;");

        body.getChildren().addAll(descLabel, datesBox, statutBadge, userBadge);

        Separator sep = new Separator();
        VBox.setMargin(sep, new Insets(4,0,0,0));

        VBox actionsBox = new VBox(8);
        actionsBox.setPadding(new Insets(12,16,14,16));

        HBox row1 = new HBox(6);
        row1.setAlignment(Pos.CENTER);

        Button btnVoir = makeBtn("👁 Détails","#EFF6FF","#3B82F6");
        Button btnEdit = makeBtn("✏ Modifier",c1+"22",c1);
        Button btnDel  = makeBtn("🗑 Supprimer","#FFF1F2","#E11D48");

        boolean isOwner = (currentUser != null && o.getIdUserId() == currentUser.getId());

        if (!isOwner) {
            btnEdit.setDisable(true);
            btnEdit.setVisible(false);
            btnDel.setDisable(true);
            btnDel.setVisible(false);
            HBox.setHgrow(btnVoir, Priority.ALWAYS);
            btnVoir.setMaxWidth(Double.MAX_VALUE);
        } else {
            HBox.setHgrow(btnVoir, Priority.ALWAYS);
            btnVoir.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnEdit, Priority.ALWAYS);
            btnEdit.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btnDel, Priority.ALWAYS);
            btnDel.setMaxWidth(Double.MAX_VALUE);
        }

        row1.getChildren().addAll(btnVoir, btnEdit, btnDel);

        final int nb = nbTaches;
        Button btnTaches = new Button("📋  Voir les tâches ("+nb+")");
        btnTaches.setMaxWidth(Double.MAX_VALUE);
        btnTaches.setStyle("-fx-background-color:linear-gradient(to right,"+c1+","+c2+");-fx-text-fill:white;-fx-font-size:12px;-fx-font-weight:bold;-fx-background-radius:10;-fx-padding:9 0 9 0;-fx-cursor:hand;");

        btnVoir.setOnAction(e  -> showDetails(o));
        if (isOwner) {
            btnEdit.setOnAction(e  -> openEditForm(o));
            btnDel.setOnAction(e   -> handleDelete(o));
        }
        btnTaches.setOnAction(e-> ouvrirTaches(o));

        actionsBox.getChildren().addAll(row1, btnTaches);
        card.getChildren().addAll(header, body, sep, actionsBox);
        return card;
    }

    private String getUserLabel(int userId) {
        return userMap.entrySet().stream()
                .filter(e -> e.getValue() == userId)
                .map(Map.Entry::getKey)
                .findFirst().orElse("User #" + userId);
    }

    private void ouvrirTaches(Objectif o) {
        if (homeController == null) {
            showAlert(Alert.AlertType.ERROR,"Erreur","HomeController non initialisé.");
            return;
        }
        try {
            var resource = getClass().getResource("/com/example/pijava_fluently/fxml/Tache-view.fxml");
            if (resource == null) {
                showAlert(Alert.AlertType.ERROR,"Erreur","Fichier Tache-view.fxml introuvable.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Node view = loader.load();
            TacheController ctrl = loader.getController();
            ctrl.setObjectif(o);
            ctrl.setObjectifController(this);
            ctrl.setCurrentUser(currentUser);
            homeController.setContent(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR,"Erreur","Impossible de charger la vue des tâches : "+e.getMessage());
        }
    }

    public void retourObjectifs() {
        if (homeController != null) homeController.showObjectifs();
    }

    @FXML private void handleAjouter() {
        selectedObjectif = null;
        clearForm();
        clearErrors();
        formTitle.setText("Nouvel Objectif");
        formTitleIcon.setText("✚");
        formCard.setVisible(true);
        formCard.setManaged(true);
        formCard.setStyle(formCard.getStyle() + "-fx-scale-x:0.95;-fx-scale-y:0.95;");
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(100), e -> formCard.setStyle(formCard.getStyle().replace("-fx-scale-x:0.95;-fx-scale-y:0.95;", "")))
        );
        timeline.play();
    }

    private void openEditForm(Objectif o) {
        selectedObjectif = o;
        clearErrors();
        fieldTitre.setText(o.getTitre()!=null?o.getTitre():"");
        fieldDescription.setText(o.getDescription()!=null?o.getDescription():"");
        fieldDateDeb.setValue(o.getDateDeb());
        fieldDateFin.setValue(o.getDateFin());
        comboStatut.setValue(o.getStatut());
        userMap.entrySet().stream()
                .filter(e -> e.getValue() == o.getIdUserId())
                .map(Map.Entry::getKey)
                .findFirst()
                .ifPresent(comboUser::setValue);
        formTitle.setText("Modifier l'Objectif");
        formTitleIcon.setText("✎");
        formCard.setVisible(true);
        formCard.setManaged(true);
    }

    @FXML private void handleSave() {
        clearErrors();
        if (!validateForm()) return;
        try {
            String titre      = fieldTitre.getText().trim();
            String desc       = fieldDescription.getText().trim();
            LocalDate dateDeb = fieldDateDeb.getValue();
            LocalDate dateFin = fieldDateFin.getValue();
            String statut     = comboStatut.getValue();
            int idUser        = userMap.get(comboUser.getValue());

            if (selectedObjectif == null) {
                service.ajouter(new Objectif(titre, desc, dateDeb, dateFin, statut, idUser));
                showSuccessToast("✅ Objectif ajouté avec succès !");
            } else {
                selectedObjectif.setTitre(titre);
                selectedObjectif.setDescription(desc);
                selectedObjectif.setDateDeb(dateDeb);
                selectedObjectif.setDateFin(dateFin);
                selectedObjectif.setStatut(statut);
                selectedObjectif.setIdUserId(idUser);
                service.modifier(selectedObjectif);
                showSuccessToast("✅ Objectif modifié avec succès !");
            }
            handleCancel();
            loadData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR,"Erreur BD",e.getMessage());
        }
    }

    @FXML private void handleCancel() {
        formCard.setVisible(false);
        formCard.setManaged(false);
        clearForm();
        clearErrors();
        selectedObjectif = null;
    }

    private boolean validateTitre(String titre) {
        if (titre.isEmpty()) {
            setError(errTitre, "Le titre est obligatoire.", fieldTitre);
            return false;
        } else if (titre.length() < 3) {
            setError(errTitre, "Minimum 3 caractères.", fieldTitre);
            return false;
        } else if (titre.length() > 50) {
            setError(errTitre, "Maximum 50 caractères.", fieldTitre);
            return false;
        } else {
            clearError(errTitre);
            setValidStyle(fieldTitre);
            return true;
        }
    }

    private boolean validateDescription(String desc) {
        if (desc.isEmpty()) {
            setError(errDescription, "La description est obligatoire.", fieldDescription);
            return false;
        } else if (desc.length() > 255) {
            setError(errDescription, "Maximum 255 caractères.", fieldDescription);
            return false;
        } else {
            clearError(errDescription);
            setValidStyle(fieldDescription);
            return true;
        }
    }

    private boolean validateDates() {
        LocalDate dateDeb = fieldDateDeb.getValue();
        LocalDate dateFin = fieldDateFin.getValue();

        if (dateDeb == null) {
            setError(errDateDeb, "Date de début obligatoire.", fieldDateDeb);
            return false;
        } else {
            clearError(errDateDeb);
            setValidStyle(fieldDateDeb);
        }

        if (dateFin == null) {
            setError(errDateFin, "Date de fin obligatoire.", fieldDateFin);
            return false;
        } else {
            clearError(errDateFin);
            setValidStyle(fieldDateFin);
        }

        if (dateDeb != null && dateFin != null && !dateFin.isAfter(dateDeb)) {
            setError(errDateFin, "La date de fin doit être après la date de début.", fieldDateFin);
            return false;
        } else if (dateDeb != null && dateFin != null) {
            clearError(errDateFin);
            setValidStyle(fieldDateFin);
        }

        return true;
    }

    private boolean validateForm() {
        boolean ok = true;

        if (!validateTitre(fieldTitre.getText().trim())) ok = false;
        if (!validateDescription(fieldDescription.getText().trim())) ok = false;
        if (!validateDates()) ok = false;

        if (comboStatut.getValue() == null) {
            setError(errStatut, "Sélectionnez un statut.", comboStatut);
            ok = false;
        } else {
            clearError(errStatut);
            setValidStyle(comboStatut);
        }

        if (comboUser.getValue() == null) {
            setError(errUser, "Sélectionnez un utilisateur.", comboUser);
            ok = false;
        } else {
            clearError(errUser);
            setValidStyle(comboUser);
        }

        return ok;
    }

    private static final String ERROR_STYLE = "-fx-border-color:#E11D48;-fx-border-width:2;-fx-border-radius:10;";
    private static final String VALID_STYLE = "-fx-border-color:#10B981;-fx-border-width:2;-fx-border-radius:10;";
    private static final String NORMAL_STYLE = "-fx-border-color:#E2E8F0;-fx-border-width:1.5;-fx-border-radius:10;";

    private void setError(Label lbl, String msg, Control control) {
        if (lbl == null) return;
        lbl.setText("⚠  " + msg);
        lbl.setStyle("-fx-font-size:11px;-fx-text-fill:#E11D48;-fx-font-weight:bold;-fx-padding:3 0 0 4;");
        lbl.setVisible(true);
        lbl.setManaged(true);

        if (control != null) {
            String currentStyle = control.getStyle();
            currentStyle = currentStyle.replace(VALID_STYLE, "").replace(NORMAL_STYLE, "");
            control.setStyle(currentStyle + ERROR_STYLE);
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
        currentStyle = currentStyle.replace(ERROR_STYLE, "").replace(NORMAL_STYLE, "");
        control.setStyle(currentStyle + VALID_STYLE);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(2), e -> {
                    String style = control.getStyle().replace(VALID_STYLE, NORMAL_STYLE);
                    control.setStyle(style);
                })
        );
        timeline.setCycleCount(1);
        timeline.play();
    }

    private void clearFieldError(Control control) {
        if (control == null) return;
        String currentStyle = control.getStyle();
        currentStyle = currentStyle.replace(ERROR_STYLE, "").replace(VALID_STYLE, "");
        control.setStyle(currentStyle + NORMAL_STYLE);
    }

    private void clearErrors() {
        Label[] labels = {errTitre, errDescription, errDateDeb, errDateFin, errStatut, errUser};
        Control[] controls = {fieldTitre, fieldDescription, fieldDateDeb, fieldDateFin, comboStatut, comboUser};

        for (Label l : labels) {
            if (l != null) {
                l.setText("");
                l.setVisible(false);
                l.setManaged(false);
            }
        }

        for (Control c : controls) {
            clearFieldError(c);
        }
    }

    private void showSuccessToast(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color:#FFFFFF;-fx-background-radius:12;");
        alert.showAndWait();
    }

    @FXML private void handleSearch() {
        String q = searchField.getText().toLowerCase().trim();
        if (q.isEmpty()) {
            renderCards(allData);
            updateCountLabel(allData.size());
            return;
        }
        List<Objectif> filtered = allData.stream()
                .filter(o -> (o.getTitre()!=null && o.getTitre().toLowerCase().contains(q)) ||
                        (o.getDescription()!=null && o.getDescription().toLowerCase().contains(q)) ||
                        (o.getStatut()!=null && o.getStatut().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        renderCards(filtered);
        updateCountLabel(filtered.size());
    }

    private void showDetails(Objectif o) {
        int colorIdx = (int)(o.getId() % CARD_COLORS.length);
        String c1 = CARD_COLORS[colorIdx][0], c2 = CARD_COLORS[colorIdx][1];
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails — "+o.getTitre());
        dialog.setHeaderText(null);

        VBox content = new VBox(0);
        content.setPrefWidth(500);

        VBox header = new VBox(6);
        header.setPadding(new Insets(24,28,20,28));
        header.setStyle("-fx-background-color:linear-gradient(to right,"+c1+","+c2+");");

        Label titleLbl = new Label(o.getTitre()!=null?o.getTitre():"—");
        titleLbl.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");
        titleLbl.setWrapText(true);

        Label statLbl = new Label(getStatutIcon(o.getStatut())+"  "+(o.getStatut()!=null?o.getStatut():"—"));
        statLbl.setStyle("-fx-font-size:12px;-fx-text-fill:rgba(255,255,255,0.85);-fx-font-weight:bold;");
        header.getChildren().addAll(titleLbl, statLbl);

        VBox body = new VBox(14);
        body.setPadding(new Insets(20,28,24,28));

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setStyle("-fx-background-color:#F8F9FD;-fx-background-radius:12;-fx-padding:16;");

        ColumnConstraints cc1 = new ColumnConstraints(110);
        ColumnConstraints cc2 = new ColumnConstraints();
        cc2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc1, cc2);

        String ls="-fx-font-size:11px;-fx-text-fill:#9CA3AF;-fx-font-weight:bold;";
        String vs="-fx-font-size:13px;-fx-text-fill:#1F2937;-fx-font-weight:bold;";

        addRow(grid,0,"ID",String.valueOf(o.getId()),ls,vs);
        addRow(grid,1,"Date début",o.getDateDeb()!=null?o.getDateDeb().format(FMT):"—",ls,vs);
        addRow(grid,2,"Date fin",o.getDateFin()!=null?o.getDateFin().format(FMT):"—",ls,vs);
        addRow(grid,3,"Utilisateur",getUserLabel(o.getIdUserId()),ls,vs);

        Label descTitle = new Label("📝  Description");
        descTitle.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#374151;");

        TextArea descArea = new TextArea(o.getDescription()!=null?o.getDescription():"Aucune description.");
        descArea.setEditable(false);
        descArea.setWrapText(true);
        descArea.setPrefHeight(80);
        descArea.setStyle("-fx-background-color:#F9FAFB;-fx-border-color:#E5E7EB;-fx-border-radius:10;-fx-font-size:13px;");

        body.getChildren().addAll(grid, descTitle, descArea);
        content.getChildren().addAll(header, body);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF;-fx-padding:0;");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Button close = (Button)dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        close.setText("Fermer");
        close.setStyle("-fx-background-color:"+c1+";-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 24 8 24;-fx-cursor:hand;");

        dialog.showAndWait();
    }

    private void addRow(GridPane g, int row, String l, String v, String ls, String vs) {
        Label ll=new Label(l); ll.setStyle(ls);
        Label vv=new Label(v); vv.setStyle(vs);
        g.add(ll,0,row); g.add(vv,1,row);
    }

    private void handleDelete(Objectif o) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer \""+o.getTitre()+"\" ?\nSes tâches seront aussi supprimées.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn==ButtonType.YES) {
                try {
                    service.supprimer(o.getId());
                    loadData();
                    showSuccessToast("🗑 Objectif supprimé avec succès !");
                } catch (SQLException e) {
                    showAlert(Alert.AlertType.ERROR,"Erreur BD",e.getMessage());
                }
            }
        });
    }

    private VBox dateBadge(String label, LocalDate date, String bg, String fg) {
        VBox box = new VBox(2);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:10px;-fx-text-fill:#9CA3AF;-fx-font-weight:bold;");
        Label val = new Label(date!=null?date.format(FMT):"—");
        val.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:2 8 2 8;");
        box.getChildren().addAll(lbl, val);
        return box;
    }

    private Label buildStatutBadge(String statut) {
        String bg,fg,icon;
        switch (statut!=null?statut:"") {
            case "Terminé"  -> { bg="#ECFDF5"; fg="#059669"; icon="✅"; }
            case "En pause" -> { bg="#FFFBEB"; fg="#D97706"; icon="⏸"; }
            case "Annulé"   -> { bg="#FFF1F2"; fg="#E11D48"; icon="❌"; }
            default          -> { bg="#EEF2FF"; fg="#6C63FF"; icon="🔄"; }
        }
        Label badge = new Label(icon+"  "+(statut!=null?statut:"—"));
        badge.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:4 12 4 12;");
        return badge;
    }

    private String getStatutIcon(String statut) {
        return switch (statut!=null?statut:"") {
            case "Terminé"  -> "✅";
            case "En pause" -> "⏸";
            case "Annulé"   -> "❌";
            default          -> "🎯";
        };
    }

    private Button makeBtn(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:"+bg+";-fx-text-fill:"+fg+";-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:6 10 6 10;-fx-cursor:hand;");
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle() + "-fx-opacity:0.85;"));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace("-fx-opacity:0.85;", "")));
        return btn;
    }

    private void updateCountLabel(int count) {
        countLabel.setText(count+" objectif(s)");
    }

    private void clearForm() {
        fieldTitre.clear();
        fieldDescription.clear();
        fieldDateDeb.setValue(LocalDate.now());
        fieldDateFin.setValue(null);
        comboStatut.setValue(null);
        if (currentUser != null) {
            String label = currentUser.getNom() + " " + currentUser.getPrenom();
            comboUser.setValue(label);
        } else {
            comboUser.setValue(null);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}