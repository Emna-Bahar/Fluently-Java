package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageService {

    private final Connection connection;
    private Boolean isEpingleNumeric;

    public MessageService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public List<Message> recupererParGroupe(int idGroupe) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String query = """
                SELECT id, contenu, type_message, date_creation, date_modif, statut_message, id_groupe_id, id_user_id
                FROM `message`
                WHERE id_groupe_id = ?
                ORDER BY date_creation DESC, date_modif DESC, id DESC
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, idGroupe);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Message message = new Message();
                    message.setId(resultSet.getInt("id"));
                    message.setContenu(resultSet.getString("contenu"));
                    message.setTypeMessage(resultSet.getString("type_message"));
                    message.setDateCreation(resultSet.getTimestamp("date_creation"));
                    message.setDateModif(resultSet.getTimestamp("date_modif"));
                    message.setStatutMessage(resultSet.getString("statut_message"));
                    message.setIdGroupeId(resultSet.getInt("id_groupe_id"));
                    message.setIdUserId(resultSet.getInt("id_user_id"));
                    messages.add(message);
                }
            }
        }
        return messages;
    }

    public void ajouter(Message message) throws SQLException {
        String query = """
                INSERT INTO `message`
                (contenu, type_message, emoji_react, is_epingle, date_creation, date_modif, statut_message, id_groupe_id, id_user_id)
                VALUES (?, ?, ?, ?, NOW(), NOW(), ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, message.getContenu());
            statement.setString(2, message.getTypeMessage());
            statement.setString(3, "");

            if (isEpingleNumericType()) {
                statement.setInt(4, 0);
            } else {
                statement.setString(4, "NON");
            }

            statement.setString(5, message.getStatutMessage());
            statement.setInt(6, message.getIdGroupeId());
            statement.setInt(7, message.getIdUserId());
            statement.executeUpdate();
        }
    }

    private boolean isEpingleNumericType() throws SQLException {
        if (isEpingleNumeric != null) {
            return isEpingleNumeric;
        }

        String query = "SHOW COLUMNS FROM `message` LIKE 'is_epingle'";
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                isEpingleNumeric = true;
                return true;
            }

            String type = resultSet.getString("Type");
            String normalized = type == null ? "" : type.toLowerCase();
            isEpingleNumeric = normalized.startsWith("tinyint")
                    || normalized.startsWith("int")
                    || normalized.startsWith("smallint")
                    || normalized.startsWith("bigint")
                    || normalized.startsWith("bit")
                    || normalized.startsWith("boolean");
            return isEpingleNumeric;
        }
    }

    public int compterParticipantsParGroupe(int idGroupe) throws SQLException {
        String query = "SELECT COUNT(DISTINCT id_user_id) AS total FROM `message` WHERE id_groupe_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, idGroupe);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("total");
                }
            }
        }
        return 0;
    }

    public void modifierContenu(int idMessage, String nouveauContenu) throws SQLException {
        String query = "UPDATE `message` SET contenu = ?, date_modif = NOW() WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, nouveauContenu);
            statement.setInt(2, idMessage);
            statement.executeUpdate();
        }
    }

    public void supprimer(int idMessage) throws SQLException {
        String query = "DELETE FROM `message` WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, idMessage);
            statement.executeUpdate();
        }
    }

    /**
     * Delete a message and write an entry to message_log.
     * Call this instead of supprimer() whenever a user/admin action should be audited.
     */
    public void supprimerAvecLog(Message message, int performedById, String performedByName,
                                  MessageLogService logService) throws SQLException {
        logService.logDelete(
                message.getId(), message.getIdGroupeId(),
                message.getIdUserId(), "User #" + message.getIdUserId(),
                message.getContenu(), performedById
        );
        supprimer(message.getId());
    }

    /**
     * Edit a message's content and write an entry to message_log.
     */
    public void modifierContenuAvecLog(Message message, String nouveauContenu,
                                        int performedById, String performedByName,
                                        MessageLogService logService) throws SQLException {
        logService.logEdit(
                message.getId(), message.getIdGroupeId(),
                message.getIdUserId(), "User #" + message.getIdUserId(),
                message.getContenu(), nouveauContenu, performedById
        );
        modifierContenu(message.getId(), nouveauContenu);
    }

    /**
     * Fetch a single message by ID (needed for logging before delete/edit).
     */
    public Message recupererParId(int idMessage) throws SQLException {
        String query = """
                SELECT id, contenu, type_message, date_creation, date_modif,
                       statut_message, id_groupe_id, id_user_id
                FROM `message` WHERE id = ?
                """;
        try (PreparedStatement st = connection.prepareStatement(query)) {
            st.setInt(1, idMessage);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    Message m = new Message();
                    m.setId(rs.getInt("id"));
                    m.setContenu(rs.getString("contenu"));
                    m.setTypeMessage(rs.getString("type_message"));
                    m.setDateCreation(rs.getTimestamp("date_creation"));
                    m.setDateModif(rs.getTimestamp("date_modif"));
                    m.setStatutMessage(rs.getString("statut_message"));
                    m.setIdGroupeId(rs.getInt("id_groupe_id"));
                    m.setIdUserId(rs.getInt("id_user_id"));
                    return m;
                }
            }
        }
        return null;
    }

    public boolean estParticipant(int idGroupe, int idUser) throws SQLException {
        String query = "SELECT 1 FROM `message` WHERE id_groupe_id = ? AND id_user_id = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, idGroupe);
            statement.setInt(2, idUser);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
