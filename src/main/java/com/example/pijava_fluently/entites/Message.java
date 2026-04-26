package com.example.pijava_fluently.entites;

import java.sql.Timestamp;

public class Message {
    private int id;
    private String contenu;
    private String typeMessage;
    private Timestamp dateCreation;
    private Timestamp dateModif;
    private String statutMessage;
    private int idGroupeId;
    private int idUserId;
    private Integer parentMessageId;
    private String mentions;
    private boolean flagged;
    private boolean moderationChecked;
    private String sentiment; // "positive", "negative", "neutral", or null

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public String getTypeMessage() {
        return typeMessage;
    }

    public void setTypeMessage(String typeMessage) {
        this.typeMessage = typeMessage;
    }

    public Timestamp getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Timestamp dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Timestamp getDateModif() {
        return dateModif;
    }

    public void setDateModif(Timestamp dateModif) {
        this.dateModif = dateModif;
    }

    public String getStatutMessage() {
        return statutMessage;
    }

    public void setStatutMessage(String statutMessage) {
        this.statutMessage = statutMessage;
    }

    public int getIdGroupeId() {
        return idGroupeId;
    }

    public void setIdGroupeId(int idGroupeId) {
        this.idGroupeId = idGroupeId;
    }

    public int getIdUserId() {
        return idUserId;
    }

    public void setIdUserId(int idUserId) {
        this.idUserId = idUserId;
    }

    public Integer getParentMessageId() {
        return parentMessageId;
    }

    public void setParentMessageId(Integer parentMessageId) {
        this.parentMessageId = parentMessageId;
    }

    public String getMentions() {
        return mentions;
    }

    public void setMentions(String mentions) {
        this.mentions = mentions;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public boolean isModerationChecked() {
        return moderationChecked;
    }

    public void setModerationChecked(boolean moderationChecked) {
        this.moderationChecked = moderationChecked;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }
}