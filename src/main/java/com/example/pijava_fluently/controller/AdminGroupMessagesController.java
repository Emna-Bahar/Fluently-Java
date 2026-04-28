package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Groupe;
import com.example.pijava_fluently.entites.Message;
import com.example.pijava_fluently.entites.MessageLog;
import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.MessageLogService;
import com.example.pijava_fluently.services.MessageService;
import com.example.pijava_fluently.services.UserService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminGroupMessagesController implements Initializable {

    @FXML private Label lblGroupName;
    @FXML private Label lblGroupSubtitle;
    @FXML private Label lblMessageCount;
    @FXML private Label lblLogCount;

    // Messages table
    @FXML private TableView<Message> messagesTable;
    @FXML private TableColumn<Message, String> colMsgId;
    @FXML private TableColumn<Message, String> colMsgUser;
    @FXML private TableColumn<Message, String> colMsgContent;
    @FXML private TableColumn<Message, String> colMsgDate;
    @FXML private TableColumn<Message, Void>   colMsgActions;

    // Log table
    @FXML private TableView<MessageLog> logTable;
    @FXML private TableColumn<MessageLog, String> colLogAction;
    @FXML private TableColumn<MessageLog, String> colLogUser;
    @FXML private TableColumn<MessageLog, String> colLogOriginal;
    @FXML private TableColumn<MessageLog, String> colLogNew;
    @FXML private TableColumn<MessageLog, String> colLogDate;

    private MessageService messageService;
    private MessageLogService messageLogService;
    private UserService userService;
    private final java.util.Map<Integer, String> userDisplayCache = new java.util.HashMap<>();
    private Groupe currentGroupe;
    private Runnable onBack;

    private int adminId = 0;
    private String adminName = "Admin";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        messageService = new MessageService();
        messageLogService = new MessageLogService();
        userService = new UserService();
        setupMessagesTable();
        setupLogTable();
    }

    /** Called by AdminDashboardController after loading the FXML. */
    public void setGroupe(Groupe groupe) {
        this.currentGroupe = groupe;
        lblGroupName.setText("Messages — " + groupe.getNom());
        lblGroupSubtitle.setText(groupe.getDescription() == null ? "" : groupe.getDescription());
        loadMessages();
        loadLog();
    }

    /** Callback so the "← Retour" button can pop this view. */
    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    public void setAdminContext(int adminId, String adminName) {
        this.adminId = adminId;
        if (adminName != null && !adminName.isBlank()) {
            this.adminName = adminName;
        }
    }

    @FXML
    private void handleBack() {
        if (onBack != null) onBack.run();
    }

    @FXML
    private void handleRefresh() {
        loadMessages();
    }

    @FXML
    private void handleRefreshLog() {
        loadLog();
    }

    // ── Data loading ─────────────────────────────────────────────────

    private void loadMessages() {
        if (currentGroupe == null) return;
        try {
            List<Message> messages = messageService.recupererParGroupe(currentGroupe.getId());
            messagesTable.setItems(FXCollections.observableArrayList(messages));
            lblMessageCount.setText(messages.size() + " message" + (messages.size() > 1 ? "s" : ""));
        } catch (SQLException e) {
            showError("Erreur chargement messages : " + e.getMessage());
        }
    }

    private void loadLog() {
        if (currentGroupe == null) return;
        try {
            List<MessageLog> logs = messageLogService.recupererParGroupe(currentGroupe.getId());
            logTable.setItems(FXCollections.observableArrayList(logs));
            lblLogCount.setText(logs.size() + " entrée" + (logs.size() > 1 ? "s" : ""));
        } catch (SQLException e) {
            showError("Erreur chargement journal : " + e.getMessage());
        }
    }

    // ── Table setup ──────────────────────────────────────────────────

    private void setupMessagesTable() {
        colMsgId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colMsgUser.setCellValueFactory(c -> new SimpleStringProperty(resolveUsername(c.getValue().getIdUserId())));
        colMsgContent.setCellValueFactory(c -> {
            String txt = c.getValue().getContenu();
            return new SimpleStringProperty(txt != null && txt.length() > 80 ? txt.substring(0, 77) + "…" : txt);
        });
        colMsgDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateCreation() == null ? "—" : dateFormat.format(c.getValue().getDateCreation())
        ));

        colMsgActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("Supprimer");
            {
                btnDelete.getStyleClass().add("table-action-delete");
                btnDelete.setOnAction(e -> {
                    Message msg = getTableView().getItems().get(getIndex());
                    handleAdminDelete(msg);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
            }
        });
    }

    private void setupLogTable() {
        colLogAction.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAction()));
        colLogAction.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            { setGraphic(badge); setContentDisplay(ContentDisplay.GRAPHIC_ONLY); }
            @Override
            protected void updateItem(String action, boolean empty) {
                super.updateItem(action, empty);
                if (empty || action == null) { badge.setText(null); badge.setStyle(""); return; }
                badge.setText(action);
                String base = "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;";
                badge.setStyle(base + ("DELETE".equals(action)
                        ? "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;"
                        : "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E;"));
            }
        });

        colLogUser.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getUserName() != null ? c.getValue().getUserName() : "User #" + c.getValue().getUserId()
        ));
        colLogOriginal.setCellValueFactory(c -> {
            String txt = c.getValue().getOriginalContent();
            return new SimpleStringProperty(txt != null && txt.length() > 50 ? txt.substring(0, 47) + "…" : txt);
        });
        colLogNew.setCellValueFactory(c -> {
            String txt = c.getValue().getNewContent();
            if (txt == null || txt.isBlank()) return new SimpleStringProperty("—");
            return new SimpleStringProperty(txt.length() > 50 ? txt.substring(0, 47) + "…" : txt);
        });
        colLogDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() == null ? "—" : dateFormat.format(c.getValue().getCreatedAt())
        ));
    }

    // ── Actions ──────────────────────────────────────────────────────

    private void handleAdminDelete(Message message) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer le message");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer définitivement ce message ? L'action sera journalisée.");

        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) return;

        try {
            int performerId = adminId > 0 ? adminId : message.getIdUserId();
            String performerName = (adminName != null && !adminName.isBlank())
                    ? adminName
                    : "User #" + performerId;
            messageService.supprimerAvecLog(message, performerId, performerName, messageLogService);
            loadMessages();
            loadLog();
        } catch (SQLException e) {
            showError("Erreur suppression : " + e.getMessage());
        }
    }

    private String resolveUsername(int userId) {
        return userDisplayCache.computeIfAbsent(userId, id -> {
            try {
                User u = userService.findById(id);
                if (u != null) return u.getPrenom() + " " + u.getNom();
            } catch (Exception ignored) {}
            return "User #" + id;
        });
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
