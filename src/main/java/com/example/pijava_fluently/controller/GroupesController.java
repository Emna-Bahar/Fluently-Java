package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.services.GroupService;
import com.example.pijava_fluently.services.Groupe;
import com.example.pijava_fluently.services.Langue;
import com.example.pijava_fluently.services.LangueService;
import com.example.pijava_fluently.services.MessageService;
import com.example.pijava_fluently.services.Niveau;
import com.example.pijava_fluently.services.NiveauService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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
            filterLangue.getItems().addAll(langues.stream()
                    .map(Langue::getNom)
                    .collect(Collectors.toList()));
            filterLangue.setValue("Toutes");

            filterNiveau.getItems().add("Tous");
            filterNiveau.setValue("Tous");
        } catch (SQLException e) {
            showError("Erreur lors du chargement des filtres : " + e.getMessage());
        }
    }

    private void loadGroupes() {
        try {
            allGroupes = groupService.recuperer();
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
                .filter(this::matchesStatutFilter)
                .collect(Collectors.toList());

        displayGroupes(filtered);
        lblResultCount.setText(filtered.size() + " groupe" + (filtered.size() > 1 ? "s" : ""));
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
            Label emptyLabel = new Label("Aucun groupe trouvé");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280; -fx-padding: 40px;");
            groupsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Groupe groupe : groupes) {
            groupsContainer.getChildren().add(createGroupCard(groupe));
        }
    }

    private VBox createGroupCard(Groupe groupe) {
        VBox card = new VBox(12);
        card.setPrefWidth(320);
        card.setStyle("""
            -fx-background-color: white;
            -fx-background-radius: 12px;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);
            -fx-padding: 20px;
            -fx-cursor: hand;
            """);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(groupe.getNom());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Label statusBadge = new Label(groupe.getStatut());
        statusBadge.setStyle(getStatusStyle(groupe.getStatut()));
        header.getChildren().addAll(nameLabel, statusBadge);

        Label descLabel = new Label(groupe.getDescription() == null ? "" : groupe.getDescription());
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(60);
        descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6b7280;");

        int currentMembers = getCurrentMemberCount(groupe.getId());
        boolean alreadyParticipant = isCurrentUserParticipant(groupe.getId());

        VBox details = new VBox(8);
        details.setStyle("-fx-padding: 12px 0;");

        String langueNom = "N/A";
        String niveauTitre = "N/A";
        try {
            Langue langue = langueService.recupererParId(groupe.getIdLangueId());
            if (langue != null) {
                langueNom = langue.getNom();
            }

            Niveau niveau = niveauService.recupererParId(groupe.getIdNiveauId());
            if (niveau != null) {
                niveauTitre = niveau.getTitre();
            }
        } catch (SQLException ignored) {
        }

        HBox langueBox = createDetailRow("🌍", "Langue", langueNom);
        HBox niveauBox = createDetailRow("📊", "Niveau", niveauTitre);
        String capacityText = currentMembers + " / " + groupe.getCapacite();
        HBox capacityBox = createDetailRow("👥", "Membres", capacityText);
        details.getChildren().addAll(langueBox, niveauBox, capacityBox);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(Double.MAX_VALUE);
        progressBar.setProgress(calculateProgress(currentMembers, groupe.getCapacite()));
        progressBar.setStyle("-fx-accent: #3b82f6;");

        Button joinBtn = new Button(alreadyParticipant ? "Entrer dans le groupe →" : "Rejoindre le groupe →");
        joinBtn.setMaxWidth(Double.MAX_VALUE);
        joinBtn.setStyle("""
            -fx-background-color: #3b82f6;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-padding: 12px 20px;
            -fx-background-radius: 8px;
            -fx-cursor: hand;
            """);
        joinBtn.setOnAction(e -> handleJoinGroup(groupe));

        if (currentMembers >= groupe.getCapacite() && !alreadyParticipant) {
            joinBtn.setDisable(true);
            joinBtn.setText("Groupe complet");
            joinBtn.setStyle(joinBtn.getStyle() + "-fx-background-color: #9ca3af; -fx-opacity: 0.6;");
        }

        card.getChildren().addAll(header, descLabel, details, progressBar, joinBtn);

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + """
            -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.3), 15, 0, 0, 4);
            -fx-translate-y: -2px;
            """));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace(
                "-fx-effect: dropshadow(gaussian, rgba(59,130,246,0.3), 15, 0, 0, 4);",
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
        ).replace("-fx-translate-y: -2px;", "")));

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
        iconLabel.setStyle("-fx-font-size: 14px;");

        Label textLabel = new Label(label + ":");
        textLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280; -fx-font-weight: 500;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #111827; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

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
            int currentMembers = messageService.compterParticipantsParGroupe(groupe.getId());
            boolean alreadyParticipant = messageService.estParticipant(groupe.getId(), currentUserId);

            if (currentMembers >= groupe.getCapacite() && !alreadyParticipant) {
                showError("Ce groupe est complet.");
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
