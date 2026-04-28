package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.entites.Niveau;
import com.example.pijava_fluently.entites.Groupe;
import com.example.pijava_fluently.services.GroupService;
import com.example.pijava_fluently.services.LangueService;
import com.example.pijava_fluently.services.MessageService;
import com.example.pijava_fluently.services.NiveauService;
import java.util.HashMap;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class GroupesController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterLangue;
    @FXML private ComboBox<String> filterNiveau;
    @FXML private ComboBox<String> filterStatut;
    @FXML private CheckBox checkCanJoin;
    @FXML private Label lblResultCount;
    @FXML private FlowPane groupsContainer;
    @FXML private VBox groupsBrowseSection;
    @FXML private VBox conversationSection;
    @FXML private Label conversationTitle;
    @FXML private StackPane conversationContainer;

    private GroupService groupService;
    private LangueService langueService;
    private NiveauService niveauService;
    private MessageService messageService;
    private List<Groupe> allGroupes;
    private int currentUserId = 1;
    private final Map<String, Integer> langueNameToId = new HashMap<>();
    private final Map<String, Integer> niveauNameToId = new HashMap<>();
    private java.util.Set<Integer> eligibleGroupIds = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        groupService = new GroupService();
        langueService = new LangueService();
        niveauService = new NiveauService();
        messageService = new MessageService();

        setupFilters();
        loadGroupes();
        showBrowseSection();
    }

    public void setCurrentUserId(int currentUserId) {
        this.currentUserId = currentUserId;
    }

    private void setupFilters() {
        filterStatut.getItems().addAll("Tous", "actif", "inactif", "complet");
        filterStatut.setValue("Tous");

        try {
            List<Langue> langues = langueService.recupererToutesLanguesActives();
            filterLangue.getItems().add("Toutes");
            for (Langue l : langues) {
                filterLangue.getItems().add(l.getNom());
                langueNameToId.put(l.getNom(), l.getId());
            }
            filterLangue.setValue("Toutes");

            filterNiveau.getItems().add("Tous");
            filterNiveau.setValue("Tous");

            filterLangue.valueProperty().addListener((obs, old, val) -> updateNiveauFilter(val));
        } catch (SQLException e) {
            showError("Erreur lors du chargement des filtres : " + e.getMessage());
        }
    }

    private void loadGroupes() {
        try {
            allGroupes = groupService.recuperer();
            eligibleGroupIds = null;
            applyFilters();
        } catch (SQLException e) {
            showError("Erreur lors du chargement des groupes : " + e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    @FXML
    private void handleFilter() {
        applyFilters();
    }

    private void applyFilters() {
        if (allGroupes == null) return;

        List<Groupe> filtered = allGroupes.stream()
                .filter(this::matchesSearch)
                .filter(this::matchesLangueFilter)
                .filter(this::matchesNiveauFilter)
                .filter(this::matchesStatutFilter)
                .filter(this::matchesCanJoinFilter)
                .collect(Collectors.toList());

        displayGroupes(filtered);
        lblResultCount.setText(filtered.size() + " groupe" + (filtered.size() > 1 ? "s" : ""));
    }

    private void updateNiveauFilter(String langueName) {
        niveauNameToId.clear();
        filterNiveau.getItems().clear();
        filterNiveau.getItems().add("Tous");
        filterNiveau.setValue("Tous");
        if (langueName == null || langueName.equals("Toutes")) return;
        Integer langueId = langueNameToId.get(langueName);
        if (langueId == null) return;
        try {
            List<Niveau> niveaux = niveauService.recupererNiveauxParLangue(langueId);
            for (Niveau n : niveaux) {
                filterNiveau.getItems().add(n.getTitre());
                niveauNameToId.put(n.getTitre(), n.getId());
            }
        } catch (SQLException e) {
            showError("Erreur lors du chargement des niveaux : " + e.getMessage());
        }
    }

    private boolean matchesLangueFilter(Groupe groupe) {
        String selected = filterLangue.getValue();
        if (selected == null || selected.equals("Toutes")) return true;
        Integer langueId = langueNameToId.get(selected);
        return langueId != null && groupe.getIdLangueId() == langueId;
    }

    private boolean matchesNiveauFilter(Groupe groupe) {
        String selected = filterNiveau.getValue();
        if (selected == null || selected.equals("Tous")) return true;
        Integer niveauId = niveauNameToId.get(selected);
        return niveauId != null && groupe.getIdNiveauId() == niveauId;
    }

    private boolean matchesCanJoinFilter(Groupe groupe) {
        if (checkCanJoin == null || !checkCanJoin.isSelected()) return true;
        if (eligibleGroupIds == null) computeEligibleGroups();
        return eligibleGroupIds.contains(groupe.getId());
    }

    private void computeEligibleGroups() {
        eligibleGroupIds = new java.util.HashSet<>();
        if (allGroupes == null) return;
        for (Groupe g : allGroupes) {
            try {
                boolean alreadyMember = isCurrentUserParticipant(g.getId());
                boolean notFull = getCurrentMemberCount(g.getId()) < g.getCapacite();
                boolean levelMatches = messageService.belongsToLangueAndNiveau(currentUserId, g.getId());
                if (levelMatches && (alreadyMember || notFull))
                    eligibleGroupIds.add(g.getId());
            } catch (SQLException ignored) {}
        }
    }

    private boolean matchesSearch(Groupe groupe) {
        if (searchField.getText() == null || searchField.getText().trim().isEmpty()) {
            return true;
        }
        String search = searchField.getText().toLowerCase();
        String description = groupe.getDescription() == null ? "" : groupe.getDescription().toLowerCase();
        return groupe.getNom().toLowerCase().contains(search) || description.contains(search);
    }

    private boolean matchesStatutFilter(Groupe groupe) {
        String statut = filterStatut.getValue();
        return statut.equals("Tous") || groupe.getStatut().equalsIgnoreCase(statut);
    }

    private void displayGroupes(List<Groupe> groupes) {
        groupsContainer.getChildren().clear();

        if (groupes.isEmpty()) {
            VBox emptyState = new VBox(8);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setStyle("-fx-padding: 60 0;");
            Label icon = new Label("🔍");
            icon.setStyle("-fx-font-size: 36px;");
            Label title = new Label("Aucun groupe trouvé");
            title.getStyleClass().add("empty-state-title");
            Label sub = new Label("Essayez de modifier vos filtres ou votre recherche");
            sub.getStyleClass().add("empty-state-subtitle");
            emptyState.getChildren().addAll(icon, title, sub);
            groupsContainer.getChildren().add(emptyState);
            return;
        }

        for (Groupe groupe : groupes) {
            groupsContainer.getChildren().add(createGroupCard(groupe));
        }
    }

    private VBox createGroupCard(Groupe groupe) {
        VBox card = new VBox(14);
        card.setPrefWidth(300);
        card.getStyleClass().add("group-card");
        card.setStyle("-fx-padding: 20;");

        // ── Header: name + status badge ──
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(groupe.getNom());
        nameLabel.getStyleClass().add("group-card-title");
        nameLabel.setWrapText(true);
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Label statusBadge = new Label(groupe.getStatut());
        statusBadge.setStyle(getStatusStyle(groupe.getStatut()));
        header.getChildren().addAll(nameLabel, statusBadge);

        // ── Description ──
        Label descLabel = new Label(groupe.getDescription() == null ? "" : groupe.getDescription());
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(50);
        descLabel.getStyleClass().add("group-card-desc");

        // ── Resolve langue / niveau names ──
        String langueNom = "N/A";
        String niveauTitre = "N/A";
        try {
            Langue langue = langueService.recupererParId(groupe.getIdLangueId());
            if (langue != null) langueNom = langue.getNom();
            Niveau niveau = niveauService.recupererParId(groupe.getIdNiveauId());
            if (niveau != null) niveauTitre = niveau.getTitre();
        } catch (SQLException ignored) {}

        int currentMembers = getCurrentMemberCount(groupe.getId());
        boolean alreadyParticipant = isCurrentUserParticipant(groupe.getId());

        // ── Detail rows ──
        VBox details = new VBox(6);
        details.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8; -fx-padding: 10 12;");
        details.getChildren().addAll(
                createDetailRow("🌍", "Langue", langueNom),
                createDetailRow("📊", "Niveau", niveauTitre),
                createDetailRow("👥", "Membres", currentMembers + " / " + groupe.getCapacite())
        );

        // ── Progress bar ──
        ProgressBar progressBar = new ProgressBar(calculateProgress(currentMembers, groupe.getCapacite()));
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(8);
        progressBar.getStyleClass().add("group-progress");

        // ── Join / enter button ──
        Button joinBtn = new Button(alreadyParticipant ? "Entrer dans le groupe →" : "Rejoindre →");
        joinBtn.setMaxWidth(Double.MAX_VALUE);
        joinBtn.setOnAction(e -> handleJoinGroup(groupe));

        if (currentMembers >= groupe.getCapacite() && !alreadyParticipant) {
            joinBtn.setText("Groupe complet");
            joinBtn.getStyleClass().add("group-card-join-btn-disabled");
            joinBtn.setDisable(true);
        } else {
            joinBtn.getStyleClass().add("group-card-join-btn");
        }

        card.getChildren().addAll(header, descLabel, details, progressBar, joinBtn);

        // ── Hover lift effect ──
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-padding: 20; -fx-translate-y: -3; " +
                "-fx-effect: dropshadow(gaussian, rgba(76,110,245,0.25), 18, 0, 0, 6); " +
                "-fx-border-color: #C7D2FE;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-padding: 20;"));

        return card;
    }

    private double calculateProgress(int currentMembers, int capacity) {
        if (capacity <= 0) return 0.0;
        return Math.min(1.0, (double) currentMembers / capacity);
    }

    private HBox createDetailRow(String icon, String label, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 13px;");

        Label textLabel = new Label(label + " :");
        textLabel.getStyleClass().add("group-card-detail-key");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("group-card-detail-val");

        row.getChildren().addAll(iconLabel, textLabel, spacer, valueLabel);
        return row;
    }

    private String getStatusStyle(String statut) {
        String baseStyle = """
            -fx-padding: 4px 12px;
            -fx-background-radius: 12px;
            -fx-font-size: 11px;
            -fx-font-weight: bold;
            """;

        return switch (statut.toLowerCase()) {
            case "actif" -> baseStyle + "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;";
            case "inactif" -> baseStyle + "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;";
            case "complet" -> baseStyle + "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;";
            default -> baseStyle + "-fx-background-color: #e5e7eb; -fx-text-fill: #374151;";
        };
    }

    private int getCurrentMemberCount(int groupeId) {
        try {
            return messageService.compterParticipantsParGroupe(groupeId);
        } catch (SQLException e) {
            return 0;
        }
    }

    private boolean isCurrentUserParticipant(int groupeId) {
        try {
            return messageService.estParticipant(groupeId, currentUserId);
        } catch (SQLException e) {
            return false;
        }
    }

    private void handleJoinGroup(Groupe groupe) {
        try {
            MessageService.JoinGroupResult joinResult = messageService.rejoindreGroupe(
                    groupe.getId(),
                    currentUserId,
                    groupe.getCapacite()
            );

            if (joinResult == MessageService.JoinGroupResult.GROUP_FULL) {
                showError("Ce groupe est complet.");
                loadGroupes();
                return;
            }

            if (joinResult == MessageService.JoinGroupResult.LANGUAGE_LEVEL_MISMATCH) {
                showError("Vous devez avoir la meme langue et le meme niveau dans votre progression pour rejoindre ce groupe.");
                loadGroupes();
                return;
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pijava_fluently/fxml/group-chat.fxml")
            );
            Parent root = loader.load();

            GroupChatController controller = loader.getController();
            controller.setCurrentUserId(currentUserId);
            controller.setGroupe(groupe);
            if (conversationTitle != null) {
                conversationTitle.setText("Conversation");
            }
            if (conversationContainer != null && conversationSection != null) {
                conversationContainer.getChildren().setAll(root);
                showConversationSection();
            }

            loadGroupes();
        } catch (IOException | SQLException e) {
            showError("Impossible d'ouvrir la conversation : " + e.getMessage());
        }
    }

    @FXML
    private void handleCloseConversation() {
        if (conversationContainer != null) {
            conversationContainer.getChildren().clear();
        }
        showBrowseSection();
    }

    private void showConversationSection() {
        if (groupsBrowseSection != null) {
            groupsBrowseSection.setVisible(false);
            groupsBrowseSection.setManaged(false);
        }
        if (conversationSection != null) {
            conversationSection.setVisible(true);
            conversationSection.setManaged(true);
        }
    }

    private void showBrowseSection() {
        if (conversationSection != null) {
            conversationSection.setVisible(false);
            conversationSection.setManaged(false);
        }
        if (groupsBrowseSection != null) {
            groupsBrowseSection.setVisible(true);
            groupsBrowseSection.setManaged(true);
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
