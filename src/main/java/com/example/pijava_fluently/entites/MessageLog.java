package com.example.pijava_fluently.entites;

import java.sql.Timestamp;

public class MessageLog {
    private int id;
    private String action;
    private int messageId;
    private int groupeId;
    private int userId;
    private String userName;
    private String originalContent;
    private String newContent;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private int createdById;
    private int updatedById;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public int getMessageId() { return messageId; }
    public void setMessageId(int messageId) { this.messageId = messageId; }

    public int getGroupeId() { return groupeId; }
    public void setGroupeId(int groupeId) { this.groupeId = groupeId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getOriginalContent() { return originalContent; }
    public void setOriginalContent(String originalContent) { this.originalContent = originalContent; }

    public String getNewContent() { return newContent; }
    public void setNewContent(String newContent) { this.newContent = newContent; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public int getCreatedById() { return createdById; }
    public void setCreatedById(int createdById) { this.createdById = createdById; }

    public int getUpdatedById() { return updatedById; }
    public void setUpdatedById(int updatedById) { this.updatedById = updatedById; }
}