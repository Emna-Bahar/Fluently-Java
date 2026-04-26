package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.MessageLog;
import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageLogService {

    private final Connection connection;

    public MessageLogService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    /**
     * Log an admin/user DELETE action on a message.
     */
    public void logDelete(int messageId, int groupeId, int userId, String userName,
                          String originalContent, int performedById) throws SQLException {
        String query = """
                INSERT INTO message_log
                (action, message_id, groupe_id, user_id, user_name,
                 original_content, new_content, created_at, updated_at,
                 created_by_id, updated_by_id)
                VALUES ('DELETE', ?, ?, ?, ?, ?, NULL, NOW(), NOW(), ?, ?)
                """;
        try (PreparedStatement st = connection.prepareStatement(query)) {
            st.setInt(1, messageId);
            st.setInt(2, groupeId);
            st.setInt(3, userId);
            st.setString(4, userName);
            st.setString(5, originalContent);
            st.setInt(6, performedById);
            st.setInt(7, performedById);
            st.executeUpdate();
        }
    }

    /**
     * Log an admin/user EDIT action on a message.
     */
    public void logEdit(int messageId, int groupeId, int userId, String userName,
                        String originalContent, String newContent, int performedById) throws SQLException {
        String query = """
                INSERT INTO message_log
                (action, message_id, groupe_id, user_id, user_name,
                 original_content, new_content, created_at, updated_at,
                 created_by_id, updated_by_id)
                VALUES ('EDIT', ?, ?, ?, ?, ?, ?, NOW(), NOW(), ?, ?)
                """;
        try (PreparedStatement st = connection.prepareStatement(query)) {
            st.setInt(1, messageId);
            st.setInt(2, groupeId);
            st.setInt(3, userId);
            st.setString(4, userName);
            st.setString(5, originalContent);
            st.setString(6, newContent);
            st.setInt(7, performedById);
            st.setInt(8, performedById);
            st.executeUpdate();
        }
    }

    /**
     * Retrieve all log entries, most recent first.
     */
    public List<MessageLog> recupererTous() throws SQLException {
        String query = """
                SELECT id, action, message_id, groupe_id, user_id, user_name,
                       original_content, new_content, created_at, updated_at,
                       created_by_id, updated_by_id
                FROM message_log
                ORDER BY created_at DESC
                """;
        return executeQuery(query, -1);
    }

    /**
     * Retrieve log entries for a specific group, most recent first.
     */
    public List<MessageLog> recupererParGroupe(int groupeId) throws SQLException {
        String query = """
                SELECT id, action, message_id, groupe_id, user_id, user_name,
                       original_content, new_content, created_at, updated_at,
                       created_by_id, updated_by_id
                FROM message_log
                WHERE groupe_id = ?
                ORDER BY created_at DESC
                """;
        return executeQuery(query, groupeId);
    }

    private List<MessageLog> executeQuery(String query, int groupeId) throws SQLException {
        List<MessageLog> logs = new ArrayList<>();
        try (PreparedStatement st = connection.prepareStatement(query)) {
            if (groupeId >= 0) st.setInt(1, groupeId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    MessageLog log = new MessageLog();
                    log.setId(rs.getInt("id"));
                    log.setAction(rs.getString("action"));
                    log.setMessageId(rs.getInt("message_id"));
                    log.setGroupeId(rs.getInt("groupe_id"));
                    log.setUserId(rs.getInt("user_id"));
                    log.setUserName(rs.getString("user_name"));
                    log.setOriginalContent(rs.getString("original_content"));
                    log.setNewContent(rs.getString("new_content"));
                    log.setCreatedAt(rs.getTimestamp("created_at"));
                    log.setUpdatedAt(rs.getTimestamp("updated_at"));
                    log.setCreatedById(rs.getInt("created_by_id"));
                    log.setUpdatedById(rs.getInt("updated_by_id"));
                    logs.add(log);
                }
            }
        }
        return logs;
    }
}
