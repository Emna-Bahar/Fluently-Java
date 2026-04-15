package com.example.pijava_fluently.entites;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Reservation {

    private int id;
    private LocalDate dateReservation;
    private String statut;          // "en attente" | "acceptée" | "refusée" | "annulée"
    private int idSessionId;
    private int idUserId;
    private Boolean presence;
    private String commentaire;
    private LocalDateTime dateConfirmation;

    // ── Constructeur vide ──────────────────────────────────────────
    public Reservation() {}

    // ── Constructeur sans id (pour création) ──────────────────────
    public Reservation(LocalDate dateReservation, String statut,
                       int idSessionId, int idUserId) {
        this.dateReservation = dateReservation;
        this.statut          = statut;
        this.idSessionId     = idSessionId;
        this.idUserId        = idUserId;
    }

    // ── Constructeur complet ───────────────────────────────────────
    public Reservation(int id, LocalDate dateReservation, String statut,
                       int idSessionId, int idUserId, Boolean presence,
                       String commentaire, LocalDateTime dateConfirmation) {
        this.id                = id;
        this.dateReservation   = dateReservation;
        this.statut            = statut;
        this.idSessionId       = idSessionId;
        this.idUserId          = idUserId;
        this.presence          = presence;
        this.commentaire       = commentaire;
        this.dateConfirmation  = dateConfirmation;
    }

    // ── Getters & Setters ──────────────────────────────────────────
    public int getId()                                  { return id; }
    public void setId(int id)                           { this.id = id; }
    public LocalDate getDateReservation()               { return dateReservation; }
    public void setDateReservation(LocalDate d)         { this.dateReservation = d; }
    public String getStatut()                           { return statut; }
    public void setStatut(String statut)                { this.statut = statut; }
    public int getIdSessionId()                         { return idSessionId; }
    public void setIdSessionId(int id)                  { this.idSessionId = id; }
    public int getIdUserId()                            { return idUserId; }
    public void setIdUserId(int id)                     { this.idUserId = id; }
    public Boolean getPresence()                        { return presence; }
    public void setPresence(Boolean presence)           { this.presence = presence; }
    public String getCommentaire()                      { return commentaire; }
    public void setCommentaire(String c)                { this.commentaire = c; }
    public LocalDateTime getDateConfirmation()          { return dateConfirmation; }
    public void setDateConfirmation(LocalDateTime d)    { this.dateConfirmation = d; }

    @Override
    public String toString() {
        return "Reservation{id=" + id + ", statut='" + statut
                + "', session=" + idSessionId + ", user=" + idUserId + "}";
    }
}