package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Groupe;
import com.example.pijava_fluently.entites.Message;
import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.ModerationService;
import com.example.pijava_fluently.services.MessageLogService;
import com.example.pijava_fluently.services.MessageService;
import com.example.pijava_fluently.services.UserService;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroupChatController implements Initializable {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9_]+)");
    private static final Pattern LOCAL_MODERATION_PATTERN = Pattern.compile(
            "(?i)\\b(hate\\s*speech|racist|sexist|genocide|kill\\s+them|terrorist|nazi)\\b"
    );

    @FXML private Label lblGroupName;
    @FXML private Label lblGroupSubtitle;
    @FXML private Label lblError;
    @FXML private ListView<Message> listMessages;
    @FXML private TextArea txtMessage;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private MessageService messageService;
    private MessageLogService messageLogService;
    private UserService userService;
    private Groupe currentGroupe;
    private int currentUserId = 1;
    private final Map<Integer, String> userDisplayCache = new HashMap<>();
    private final Map<Integer, Message> messageCacheById = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        messageService = new MessageService();
        messageLogService = new MessageLogService();
        userService = new UserService();
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

        sendMessage(contenu, null);
    }

    private void sendMessage(String contenu, Integer parentMessageId) {
        if (parentMessageId != null) {
            try {
                Message parent = messageService.recupererParId(parentMessageId);
                if (parent == null || parent.getIdGroupeId() != currentGroupe.getId()) {
                    showError("Impossible de repondre: message parent introuvable.");
                    return;
                }
            } catch (SQLException e) {
                showError("Impossible de verifier le message parent.");
                return;
            }
        }

        Message message = new Message();
        message.setContenu(contenu);
        message.setTypeMessage("texte");
        message.setStatutMessage("actif");
        message.setIdGroupeId(currentGroupe.getId());
        message.setIdUserId(currentUserId);
        message.setParentMessageId(parentMessageId);
        message.setMentions(extractMentions(contenu));

        try {
            ModerationService.ModerationResult moderationResult = messageService.analyserMessage(contenu);

            boolean blockedByApi = moderationResult.isApiAvailable()
                    && (moderationResult.isFlagged() || moderationResult.getTopScore() >= 0.70);
            boolean blockedByLocalFallback = isLocallyFlagged(contenu);

            if (blockedByApi || blockedByLocalFallback) {
                String reason = blockedByApi
                        ? moderationResult.getTopCategory()
                        : "suspicious_local_pattern";
                showError("Message bloque par moderation (" + reason + "). Merci de reformuler.");
                return;
            }

            if (!moderationResult.isApiAvailable() && isStrictModerationEnabled()) {
                String detail = moderationResult.getErrorMessage();
                if (detail == null || detail.isBlank()) {
                    detail = "API indisponible";
                }
                showError("Verification de moderation indisponible (" + detail + "). Reessayez plus tard.");
                return;
            }

            int messageId = messageService.ajouterEtRetournerId(message, parentMessageId, message.getMentions());
            messageService.enregistrerModeration(messageId, moderationResult);
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
            messageCacheById.clear();
            for (Message msg : messages) {
                messageCacheById.put(msg.getId(), msg);
            }
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
            private final Label threadLabel = new Label();
            private final Label mentionsLabel = new Label();
            private final Label metaLabel = new Label();
            private final Button btnEdit = new Button("Modifier");
            private final Button btnDelete = new Button("Supprimer");
            private final Button btnReply = new Button("Repondre");
            private final HBox actionBox = new HBox(8, btnReply, btnEdit, btnDelete);
            private final Region spacer = new Region();
            private final HBox metaRow = new HBox(8, metaLabel, spacer, actionBox);
            private final VBox box = new VBox(8, threadLabel, contentLabel, mentionsLabel, metaRow);

            {
                contentLabel.setWrapText(true);
                contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #111827;");
                contentLabel.setMaxWidth(640);

                threadLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4f46e5; -fx-font-weight: bold;");
                threadLabel.setVisible(false);
                threadLabel.setManaged(false);

                mentionsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #0f766e; -fx-font-weight: bold;");
                mentionsLabel.setVisible(false);
                mentionsLabel.setManaged(false);

                metaLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
                HBox.setHgrow(spacer, Priority.ALWAYS);
                metaRow.setAlignment(Pos.CENTER_LEFT);

                btnEdit.setStyle("-fx-background-color: #e5e7eb; -fx-text-fill: #111827; -fx-padding: 4 10; -fx-background-radius: 6; -fx-cursor: hand;");
                btnDelete.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-padding: 4 10; -fx-background-radius: 6; -fx-cursor: hand;");
                btnReply.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-padding: 4 10; -fx-background-radius: 6; -fx-cursor: hand;");

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
                String displayName = resolveUserDisplayName(message.getIdUserId());
                metaLabel.setText(displayName + " • " + dateText);

                if (message.getParentMessageId() != null) {
                    threadLabel.setText("↪ Reponse a " + resolveParentUserName(message.getParentMessageId()));
                    threadLabel.setVisible(true);
                    threadLabel.setManaged(true);
                } else {
                    threadLabel.setVisible(false);
                    threadLabel.setManaged(false);
                }

                if (message.getMentions() != null && !message.getMentions().isBlank()) {
                    mentionsLabel.setText("Mentions: " + message.getMentions());
                    mentionsLabel.setVisible(true);
                    mentionsLabel.setManaged(true);
                } else {
                    mentionsLabel.setVisible(false);
                    mentionsLabel.setManaged(false);
                }

                btnEdit.setOnAction(e -> handleEditMessage(message));
                btnDelete.setOnAction(e -> handleDeleteMessage(message));
                btnReply.setOnAction(e -> handleReplyMessage(message));

                updateActionsVisibility();
                setText(null);
                setGraphic(box);
            }
        });
    }

    private void handleReplyMessage(Message parentMessage) {
        String defaultMention = resolveUserDisplayName(parentMessage.getIdUserId());
        String defaultReply = defaultMention == null || defaultMention.isBlank()
                ? ""
                : "@" + defaultMention + " ";

        TextInputDialog dialog = new TextInputDialog(defaultReply);
        dialog.setTitle("Repondre au message");
        dialog.setHeaderText("Reponse a " + resolveUserDisplayName(parentMessage.getIdUserId())
            + " : " + truncate(parentMessage.getContenu(), 80));
        dialog.setContentText("Votre reponse :");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        String contenu = result.get().trim();
        if (contenu.isEmpty()) {
            showError("Le message ne peut pas etre vide.");
            return;
        }

        sendMessage(contenu, parentMessage.getId());
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

    private boolean isStrictModerationEnabled() {
        String value = System.getenv("FLUENTLY_MODERATION_STRICT");
        return value != null && value.equalsIgnoreCase("true");
    }

    private String extractMentions(String content) {
        LinkedHashSet<String> mentions = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content == null ? "" : content);
        while (matcher.find()) {
            mentions.add("@" + matcher.group(1));
        }

        if (mentions.isEmpty()) {
            return null;
        }
        return String.join(",", mentions);
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max - 1) + "...";
    }

    private String resolveUserDisplayName(int userId) {
        String cached = userDisplayCache.get(userId);
        if (cached != null) {
            return cached;
        }

        try {
            User user = userService.findById(userId);
            if (user != null) {
                String username = user.getNom() == null ? "" : user.getNom().trim();
                if (!username.isBlank()) {
                    userDisplayCache.put(userId, username);
                    return username;
                }

                String fullName = ((user.getPrenom() == null ? "" : user.getPrenom().trim()) + " " +
                        (user.getNom() == null ? "" : user.getNom().trim())).trim();
                if (!fullName.isBlank()) {
                    userDisplayCache.put(userId, fullName);
                    return fullName;
                }

                String email = user.getEmail() == null ? "" : user.getEmail().trim();
                if (!email.isBlank()) {
                    int at = email.indexOf('@');
                    String emailUser = at > 0 ? email.substring(0, at) : email;
                    userDisplayCache.put(userId, emailUser);
                    return emailUser;
                }
            }
        } catch (SQLException ignored) {
            // Fallback to id label if DB lookup fails.
        }

        String fallback = "Utilisateur #" + userId;
        userDisplayCache.put(userId, fallback);
        return fallback;
    }

    private String resolveParentUserName(Integer parentMessageId) {
        if (parentMessageId == null) {
            return "message introuvable";
        }

        Message parent = messageCacheById.get(parentMessageId);
        if (parent != null) {
            return resolveUserDisplayName(parent.getIdUserId());
        }

        try {
            Message fromDb = messageService.recupererParId(parentMessageId);
            if (fromDb != null) {
                messageCacheById.put(fromDb.getId(), fromDb);
                return resolveUserDisplayName(fromDb.getIdUserId());
            }
        } catch (SQLException ignored) {
            // Keep fallback label if parent lookup fails.
        }

        return "message supprime";
    }

    private boolean isLocallyFlagged(String content) {
        String value = content == null ? "" : content;
        return LOCAL_MODERATION_PATTERN.matcher(value).find();
    }
}
