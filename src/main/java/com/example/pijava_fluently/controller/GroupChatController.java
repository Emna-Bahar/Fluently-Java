package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.services.Groupe;
import com.example.pijava_fluently.services.Message;
import com.example.pijava_fluently.services.MessageLogService;
import com.example.pijava_fluently.services.MessageService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class GroupChatController implements Initializable {

    @FXML private Label lblGroupName;
    @FXML private Label lblGroupSubtitle;
    @FXML private Label lblError;
    @FXML private ListView<Message> listMessages;
    @FXML private TextArea txtMessage;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private MessageService messageService;
    private MessageLogService messageLogService;
    private Groupe currentGroupe;
    private int currentUserId = 1;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        messageService = new MessageService();
        messageLogService = new MessageLogService();
        Label emptyLabel = new Label("Aucun message pour l'instant. Soyez le premier à écrire.");
        emptyLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 13px;");
        listMessages.setPlaceholder(emptyLabel);
        setupMessagesList();
        hideError();
    }

    public void setGroupe(Groupe groupe) {
        this.currentGroupe = groupe;
        lblGroupName.setText(groupe.getNom());
        lblGroupSubtitle.setText("Messages récents en haut");
        loadMessages();
    }

    public void setCurrentUserId(int currentUserId) {
        this.currentUserId = currentUserId;
    }

    @FXML
    private void handleSend() {
        hideError();

        if (currentGroupe == null) {
            showError("Aucun groupe sélectionné.");
            return;
        }

        String contenu = txtMessage.getText() == null ? "" : txtMessage.getText().trim();
        if (contenu.isEmpty()) {
            showError("Le message ne peut pas être vide.");
            return;
        }

        Message message = new Message();
        message.setContenu(contenu);
        message.setTypeMessage("texte");
        message.setStatutMessage("actif");
        message.setIdGroupeId(currentGroupe.getId());
        message.setIdUserId(currentUserId);

        try {
            messageService.ajouter(message);
            txtMessage.clear();
            loadMessages();
        } catch (SQLException e) {
            showError("Erreur lors de l'envoi : " + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        hideError();
        loadMessages();
    }

    private void loadMessages() {
        if (currentGroupe == null) {
            return;
        }

        try {
            List<Message> messages = messageService.recupererParGroupe(currentGroupe.getId());
            listMessages.getItems().setAll(messages);
            if (!messages.isEmpty()) {
                listMessages.scrollTo(0);
            }
        } catch (SQLException e) {
            showError("Erreur lors du chargement des messages : " + e.getMessage());
        }
    }

    private void setupMessagesList() {
        listMessages.setCellFactory(param -> new ListCell<>() {
            private final Label contentLabel = new Label();
            private final Label metaLabel = new Label();
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox actionBox = new HBox(8, btnEdit, btnDelete);
            private final Region spacer = new Region();
            private final HBox metaRow = new HBox(8, metaLabel, spacer, actionBox);
            private final VBox box = new VBox(8, contentLabel, metaRow);

            {
                contentLabel.setWrapText(true);
                contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #111827;");
                contentLabel.setMaxWidth(640);

                metaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
                HBox.setHgrow(spacer, Priority.ALWAYS);
                metaRow.setAlignment(Pos.CENTER_LEFT);

                btnEdit.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #111827; -fx-padding: 4 10; -fx-background-radius: 6; -fx-cursor: hand;");
                btnDelete.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-padding: 4 10; -fx-background-radius: 6; -fx-cursor: hand;");

                actionBox.setVisible(false);
                actionBox.setManaged(false);

                box.setPadding(new Insets(10));
                box.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 10;");

                hoverProperty().addListener((obs, oldVal, newVal) -> updateActionsVisibility());
                selectedProperty().addListener((obs, oldVal, newVal) -> updateActionsVisibility());
            }

            private void updateActionsVisibility() {
                boolean show = getItem() != null && !isEmpty() && (isHover() || isSelected());
                actionBox.setVisible(show);
                actionBox.setManaged(show);
            }

            @Override
            protected void updateItem(Message message, boolean empty) {
                super.updateItem(message, empty);
                if (empty || message == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                contentLabel.setText(message.getContenu());

                String dateText = message.getDateCreation() == null
                        ? "Date inconnue"
                        : dateFormat.format(message.getDateCreation());
                metaLabel.setText("Utilisateur #" + message.getIdUserId() + " • " + dateText);

                btnEdit.setOnAction(e -> handleEditMessage(message));
                btnDelete.setOnAction(e -> handleDeleteMessage(message));

                updateActionsVisibility();
                setText(null);
                setGraphic(box);
            }
        });
    }

    private void handleEditMessage(Message message) {
        TextInputDialog dialog = new TextInputDialog(message.getContenu());
        dialog.setTitle("Modifier le message");
        dialog.setHeaderText(null);
        dialog.setContentText("Nouveau contenu :");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String nouveauContenu = result.get().trim();
        if (nouveauContenu.isEmpty()) {
            showError("Le message ne peut pas être vide.");
            return;
        }

        try {
            messageService.modifierContenuAvecLog(message, nouveauContenu,
                    currentUserId, "User #" + currentUserId, messageLogService);
            loadMessages();
        } catch (SQLException e) {
            showError("Erreur lors de la modification : " + e.getMessage());
        }
    }

    private void handleDeleteMessage(Message message) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le message");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer ce message ?");

        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            return;
        }

        try {
            messageService.supprimerAvecLog(message, currentUserId,
                    "User #" + currentUserId, messageLogService);
            loadMessages();
        } catch (SQLException e) {
            showError("Erreur lors de la suppression : " + e.getMessage());
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}
