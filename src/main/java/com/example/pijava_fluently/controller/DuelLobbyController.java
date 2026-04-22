package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.services.DuelClient;
import com.example.pijava_fluently.services.DuelMessage;
import com.example.pijava_fluently.services.DuelServer;
import com.example.pijava_fluently.services.TestService;
import com.example.pijava_fluently.utils.LoggerUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DuelLobbyController {

    @FXML private ComboBox<Test> testComboBox;
    @FXML private Button         btnCreer;
    @FXML private VBox           panelIP;
    @FXML private Label          labelIP;
    @FXML private Label          labelStatutHote;
    @FXML private TextField      fieldIP;
    @FXML private Button         btnRejoindre;
    @FXML private Label          labelStatutClient;
    @FXML private Label labelCommande;

    private final TestService testService = new TestService();
    private DuelServer   server;
    private DuelClient   client;
    private User         currentUser;
    private HomeController homeController;

    // Stocker la référence de navigation ICI — pas au moment du clic
    private StackPane contentArea;

    @FXML
    public void initialize() {
        try {
            List<Test> tests = testService.recuperer();
            // Filtrer seulement les QCM (quiz_debutant ou Test de niveau)
            tests.removeIf(t -> t.getTitre() == null);
            testComboBox.getItems().addAll(tests);
            testComboBox.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Test t, boolean empty) {
                    super.updateItem(t, empty);
                    setText(empty || t == null ? "" : t.getTitre());
                }
            });
            testComboBox.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Test t, boolean empty) {
                    super.updateItem(t, empty);
                    setText(empty || t == null ? "" : t.getTitre());
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Récupérer le contentArea APRÈS que le FXML soit attaché à la scène
        btnCreer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                contentArea = (StackPane) newScene.lookup("#contentArea");
            }
        });
    }

    public void setCurrentUser(User user)         { this.currentUser   = user; }
    public void setHomeController(HomeController hc) { this.homeController = hc; }

    // ── Hôte : créer le duel ──────────────────────────────────────

    // Remplacer la méthode handleCreerDuel par :

    @FXML
    private void handleCreerDuel() {
        Test selectedTest = testComboBox.getValue();
        if (selectedTest == null) {
            showAlert("Choisissez un test avant de créer le duel !");
            return;
        }

        btnCreer.setDisable(true);

        server = new DuelServer(
                msg -> handleMessageHote(msg, selectedTest),
                () -> {
                    labelStatutHote.setText("✅ Adversaire connecté ! Lancement...");
                    DuelMessage nameMsg = new DuelMessage(DuelMessage.Action.NAME);
                    nameMsg.playerName = currentUser.getPrenom();
                    server.send(nameMsg);
                    lancerDuel(selectedTest, true);
                }
        );
        server.start();

        panelIP.setVisible(true);
        panelIP.setManaged(true);

        // Afficher l'IP principale (la meilleure)
        String meilleureIP = DuelServer.getBestLocalIP();
        String toutesLesIP = DuelServer.getAllLocalIPs();

        labelIP.setText(meilleureIP);

        // Ajouter une info supplémentaire si plusieurs IP
        if (!toutesLesIP.equals(meilleureIP)) {
            Label infoLabel = new Label("Autres IP possibles :\n" + toutesLesIP);
            infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280; -fx-wrap-text: true;");
            panelIP.getChildren().add(infoLabel);
        }

        labelStatutHote.setText("⏳ En attente de l'adversaire...");

        // Afficher la commande pour le pare-feu (optionnel)
        if (labelCommande != null) {
            labelCommande.setText(
                    "Si l'adversaire ne peut pas se connecter :\n" +
                            "Désactivez le pare-feu Windows temporairement\n" +
                            "Ou ajoutez une règle :\n" +
                            "netsh advfirewall firewall add rule name=\"Fluently Duel\" dir=in action=allow protocol=TCP localport=9090"
            );
            labelCommande.setStyle("-fx-font-size: 11px; -fx-text-fill: #D97706; -fx-wrap-text: true;");
        }
    }

    // Remplacer getAllLocalIPs par l'appel à DuelServer.getAllLocalIPs()
    private String getAllLocalIPs() {
        StringBuilder sb = new StringBuilder();
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces =
                    java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;

                java.util.Enumeration<java.net.InetAddress> addrs =
                        ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress addr = addrs.nextElement();
                    if (addr instanceof java.net.Inet4Address
                            && !addr.isLoopbackAddress()) {
                        if (sb.length() > 0) sb.append("  /  ");
                        sb.append(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            return DuelServer.getAllLocalIPs();
        }
        return sb.length() > 0 ? sb.toString() : DuelServer.getAllLocalIPs();
    }
    // ── Client : rejoindre le duel ────────────────────────────────
    @FXML
    private void handleRejoindre() {
        String ip = fieldIP.getText().trim();
        if (ip.isEmpty()) {
            showAlert("Entrez l'IP de l'hôte !");
            return;
        }

        btnRejoindre.setDisable(true);
        labelStatutClient.setText("🔄 Connexion à " + ip + "...");

        client = new DuelClient(
                this::handleMessageClient,
                () -> {
                    labelStatutClient.setText("✅ Connecté ! En attente du lancement...");
                    // Envoyer le nom
                    DuelMessage nameMsg = new DuelMessage(DuelMessage.Action.NAME);
                    nameMsg.playerName = currentUser.getPrenom();
                    client.send(nameMsg);
                }
        );
        client.connect(ip);
    }

    // ── Messages reçus côté HÔTE ──────────────────────────────────
    private void handleMessageHote(DuelMessage msg, Test test) {
        // L'hôte reçoit uniquement NAME du client avant le lancement
        if (msg.action == DuelMessage.Action.NAME) {
            labelStatutHote.setText("Adversaire : " + msg.playerName);
        }
    }

    // ── Messages reçus côté CLIENT ────────────────────────────────
    private void handleMessageClient(DuelMessage msg) {
        if (msg.action == DuelMessage.Action.NAME) {
            // L'hôte a envoyé son nom → on peut lancer le jeu
            labelStatutClient.setText("Hôte : " + msg.playerName + " — Lancement...");
            lancerDuel(null, false);
        }
    }

    // ── Lancer l'écran de jeu ─────────────────────────────────────
    private void lancerDuel(Test test, boolean asHost) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/example/pijava_fluently/fxml/duel-game.fxml"));
            Node vue = loader.load();
            DuelGameController ctrl = loader.getController();
            ctrl.setHomeController(homeController);
            ctrl.init(currentUser, test,
                    asHost ? server : null,
                    asHost ? null   : client);

            // Utiliser homeController si disponible
            if (homeController != null) {
                homeController.setContent(vue);
            } else if (contentArea != null) {
                contentArea.getChildren().setAll(vue);
            } else {
                // Dernier recours : chercher dans la scène courante
                StackPane ca = (StackPane) btnRejoindre.getScene().lookup("#contentArea");
                if (ca != null) ca.getChildren().setAll(vue);
            }

        } catch (IOException e) {
            LoggerUtil.error("Erreur chargement duel-game.fxml", e);
        }
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}