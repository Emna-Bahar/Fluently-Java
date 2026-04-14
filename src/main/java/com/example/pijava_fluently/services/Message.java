package com.example.pijava_fluently.services;

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
}
