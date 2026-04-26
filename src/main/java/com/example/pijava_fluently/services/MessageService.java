package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Message;
import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageService {

    public enum JoinGroupResult {
        JOINED,
        ALREADY_PARTICIPANT,
        LANGUAGE_LEVEL_MISMATCH,
        GROUP_FULL
    }

    private final Connection connection;
    private final ModerationService moderationService;
    private final SentimentService sentimentService;
    private Boolean isEpingleNumeric;

    public MessageService() {
        connection = MyDatabase.getInstance().getConnection();
        moderationService = new ModerationService();
        sentimentService = new SentimentService();
        try {
            ensureMembershipTableExists();
            ensureModerationTableExists();
            ensureMessageMetadataTableExists();
            ensureSentimentTableExists();
        } catch (SQLException e) {
            System.err.println("Warning: unable to initialize message support tables: " + e.getMessage());
        }
    }

    private void ensureMembershipTableExists() throws SQLException {
        String query = """
                CREATE TABLE IF NOT EXISTS `groupe_membre` (
                  `id` INT NOT NULL AUTO_INCREMENT,
                  `id_groupe_id` INT NOT NULL,
                  `id_user_id` INT NOT NULL,
                  `date_joined` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_groupe_membre` (`id_groupe_id`, `id_user_id`),
                  KEY `idx_groupe_membre_groupe` (`id_groupe_id`),
                  KEY `idx_groupe_membre_user` (`id_user_id`)
                )
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.execute();
        }
    }

    private void ensureModerationTableExists() throws SQLException {
        String query = """
                CREATE TABLE IF NOT EXISTS `message_moderation` (
                  `id` INT NOT NULL AUTO_INCREMENT,
                  `message_id` INT NOT NULL,
                  `provider` VARCHAR(50) NOT NULL,
                  `is_flagged` TINYINT(1) NOT NULL DEFAULT 0,
                  `top_category` VARCHAR(120) NULL,
                  `top_score` DOUBLE NULL,
                  `api_available` TINYINT(1) NOT NULL DEFAULT 1,
                  `error_message` VARCHAR(500) NULL,
                  `raw_response` LONGTEXT NULL,
                  `checked_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_message_moderation_message` (`message_id`),
                  KEY `idx_message_moderation_flagged` (`is_flagged`)
                )
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.execute();
        }
    }

    private void ensureMessageMetadataTableExists() throws SQLException {
        String query = """
                CREATE TABLE IF NOT EXISTS `message_metadata` (
                  `id` INT NOT NULL AUTO_INCREMENT,
                  `message_id` INT NOT NULL,
                  `parent_message_id` INT NULL,
                  `mentions` VARCHAR(500) NULL,
                  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_message_metadata_message` (`message_id`),
                  KEY `idx_message_metadata_parent` (`parent_message_id`)
                )
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.execute();
        }
    }

    private void ensureSentimentTableExists() throws SQLException {
        String query = """
                CREATE TABLE IF NOT EXISTS `message_sentiment` (
                  `id` INT NOT NULL AUTO_INCREMENT,
                  `message_id` INT NOT NULL,
                  `sentiment` VARCHAR(20) NOT NULL,
                  `checked_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_message_sentiment_message` (`message_id`)
                )
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.execute();
        }
    }

    public void enregistrerSentiment(int messageId, String sentiment) throws SQLException {
        if (messageId <= 0 || sentiment == null) return;
        String query = """
                INSERT INTO `message_sentiment` (message_id, sentiment)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE sentiment = VALUES(sentiment), checked_at = CURRENT_TIMESTAMP
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, messageId);
            statement.setString(2, sentiment);
            statement.executeUpdate();
        }
    }

    public String analyserSentiment(String contenu) {
        return sentimentService.analyze(contenu);
    }

    public List<Message> recupererParGroupe(int idGroupe) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String query = """
              SELECT m.id, m.contenu, m.type_message, m.date_creation, m.date_modif,
                  m.statut_message, m.id_groupe_id, m.id_user_id,
                  md.parent_message_id, md.mentions,
                  COALESCE(mm.is_flagged, 0)    AS is_flagged,
                  COALESCE(mm.api_available, 0) AS api_available,
                  ms.sentiment
              FROM `message` m
              LEFT JOIN `message_metadata`   md ON md.message_id = m.id
              LEFT JOIN `message_moderation` mm ON mm.message_id = m.id
              LEFT JOIN `message_sentiment`  ms ON ms.message_id = m.id
              WHERE m.id_groupe_id = ?
              ORDER BY m.date_creation DESC, m.date_modif DESC, m.id DESC
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
                    int parentMessageId = resultSet.getInt("parent_message_id");
                    message.setParentMessageId(resultSet.wasNull() ? null : parentMessageId);
                    message.setMentions(resultSet.getString("mentions"));
                    boolean isFlagged = resultSet.getBoolean("is_flagged");
                    boolean apiAvailable = resultSet.getBoolean("api_available");
                    message.setFlagged(isFlagged);
                    message.setModerationChecked(apiAvailable);
                    message.setSentiment(resultSet.getString("sentiment"));
                    messages.add(message);
                }
            }
        }
        return messages;
    }

    public void ajouter(Message message) throws SQLException {
        ajouterInterne(message, message.getParentMessageId(), message.getMentions());
    }

    public int ajouterEtRetournerId(Message message) throws SQLException {
        return ajouterInterne(message, message.getParentMessageId(), message.getMentions());
    }

    public int ajouterEtRetournerId(Message message, Integer parentMessageId, String mentions) throws SQLException {
        return ajouterInterne(message, parentMessageId, mentions);
    }

    private int ajouterInterne(Message message, Integer parentMessageId, String mentions) throws SQLException {
        String query = """
                INSERT INTO `message`
                (contenu, type_message, emoji_react, is_epingle, date_creation, date_modif, statut_message, id_groupe_id, id_user_id)
                VALUES (?, ?, ?, ?, NOW(), NOW(), ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
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

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int messageId = keys.getInt(1);
                    if (parentMessageId != null || (mentions != null && !mentions.isBlank())) {
                        enregistrerMetadataMessage(messageId, parentMessageId, mentions);
                    }
                    return messageId;
                }
            }
            return 0;
        }
    }

    private void enregistrerMetadataMessage(int messageId, Integer parentMessageId, String mentions) throws SQLException {
        String query = """
                INSERT INTO `message_metadata` (message_id, parent_message_id, mentions)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  parent_message_id = VALUES(parent_message_id),
                  mentions = VALUES(mentions)
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, messageId);
            if (parentMessageId == null) {
                statement.setNull(2, java.sql.Types.INTEGER);
            } else {
                statement.setInt(2, parentMessageId);
            }

            if (mentions == null || mentions.isBlank()) {
                statement.setNull(3, java.sql.Types.VARCHAR);
            } else {
                statement.setString(3, mentions);
            }
            statement.executeUpdate();
        }
    }

    public ModerationService.ModerationResult analyserMessage(String contenu) {
        return moderationService.moderate(contenu);
    }

    public void enregistrerModeration(int messageId, ModerationService.ModerationResult moderationResult) throws SQLException {
        if (messageId <= 0 || moderationResult == null) {
            return;
        }

        String query = """
                INSERT INTO `message_moderation`
                (message_id, provider, is_flagged, top_category, top_score, api_available, error_message, raw_response)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  provider = VALUES(provider),
                  is_flagged = VALUES(is_flagged),
                  top_category = VALUES(top_category),
                  top_score = VALUES(top_score),
                  api_available = VALUES(api_available),
                  error_message = VALUES(error_message),
                  raw_response = VALUES(raw_response),
                  checked_at = CURRENT_TIMESTAMP
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, messageId);
            statement.setString(2, moderationResult.getProvider());
            statement.setBoolean(3, moderationResult.isFlagged());
            statement.setString(4, moderationResult.getTopCategory());
            statement.setDouble(5, moderationResult.getTopScore());
            statement.setBoolean(6, moderationResult.isApiAvailable());
            statement.setString(7, moderationResult.getErrorMessage());
            statement.setString(8, moderationResult.getRawResponse());
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
        String query = """
                SELECT COUNT(*) AS total
                FROM (
                    SELECT id_user_id FROM `message` WHERE id_groupe_id = ?
                    UNION
                    SELECT id_user_id FROM `groupe_membre` WHERE id_groupe_id = ?
                ) participants
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, idGroupe);
            statement.setInt(2, idGroupe);
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
              SELECT m.id, m.contenu, m.type_message, m.date_creation, m.date_modif,
                  m.statut_message, m.id_groupe_id, m.id_user_id,
                  md.parent_message_id, md.mentions
              FROM `message` m
              LEFT JOIN `message_metadata` md ON md.message_id = m.id
              WHERE m.id = ?
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
                    int parentMessageId = rs.getInt("parent_message_id");
                    m.setParentMessageId(rs.wasNull() ? null : parentMessageId);
                    m.setMentions(rs.getString("mentions"));
                    return m;
                }
            }
        }
        return null;
    }

    public boolean estParticipant(int idGroupe, int idUser) throws SQLException {
        String query = """
                SELECT 1
                FROM (
                    SELECT id_user_id FROM `message` WHERE id_groupe_id = ? AND id_user_id = ?
                    UNION
                    SELECT id_user_id FROM `groupe_membre` WHERE id_groupe_id = ? AND id_user_id = ?
                ) participants
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, idGroupe);
            statement.setInt(2, idUser);
            statement.setInt(3, idGroupe);
            statement.setInt(4, idUser);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public JoinGroupResult rejoindreGroupe(int idGroupe, int idUser, int capacite) throws SQLException {
        if (capacite <= 0) {
            return JoinGroupResult.GROUP_FULL;
        }

        if (estParticipant(idGroupe, idUser)) {
            return JoinGroupResult.ALREADY_PARTICIPANT;
        }

        if (!belongsToLangueAndNiveau(idUser, idGroupe)) {
            return JoinGroupResult.LANGUAGE_LEVEL_MISMATCH;
        }

        int participantsActuels = compterParticipantsParGroupe(idGroupe);
        if (participantsActuels >= capacite) {
            return JoinGroupResult.GROUP_FULL;
        }

        String query = "INSERT INTO `groupe_membre` (id_groupe_id, id_user_id) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, idGroupe);
            statement.setInt(2, idUser);
            statement.executeUpdate();
            return JoinGroupResult.JOINED;
        }
    }

    private boolean belongsToLangueAndNiveau(int idUser, int idGroupe) throws SQLException {
        String query = """
                SELECT 1
                FROM `groupe` g
                JOIN `user_progress` up
                  ON up.langue_id = g.id_langue_id
                 AND up.niveau_actuel_id = g.id_niveau_id
                WHERE g.id = ?
                  AND up.user_id = ?
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, idGroupe);
            statement.setInt(2, idUser);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
