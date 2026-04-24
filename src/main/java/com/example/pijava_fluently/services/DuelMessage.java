package com.example.pijava_fluently.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DuelMessage {

    public enum Action {
        QUESTIONS, READY, ANSWER, FINISHED, END, NAME
    }

    public Action  action;

    // ── QUESTIONS (hôte → client) ─────────────────────────────────
    // On n'envoie PAS les entités Question/Reponse directement
    // pour éviter les problèmes de sérialisation (LocalDate etc.)
    public List<QuestionDto>  questions;
    public int                scoreMaxTotal;
    public String             testTitre;

    // ── ANSWER ────────────────────────────────────────────────────
    public int     questionIndex;
    public int     reponseId;
    public boolean isCorrect;
    public int testId; // Ajouter dans DuelMessage

    // ── FINISHED ──────────────────────────────────────────────────
    public int scoreFinal;

    // ── END ───────────────────────────────────────────────────────
    public String winnerName;

    // ── NAME ──────────────────────────────────────────────────────
    public String playerName;

    public DuelMessage() {}
    public DuelMessage(Action action) { this.action = action; }

    // ─────────────────────────────────────────────────────────────
    //  DTO Question — sans LocalDate, sans champs inutiles
    // ─────────────────────────────────────────────────────────────
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuestionDto {
        public int              id;
        public String           enonce;
        public String           type;
        public int              scoreMax;
        public List<ReponseDto> reponses;

        public QuestionDto() {}

        public QuestionDto(int id, String enonce,
                           String type, int scoreMax,
                           List<ReponseDto> reponses) {
            this.id       = id;
            this.enonce   = enonce;
            this.type     = type;
            this.scoreMax = scoreMax;
            this.reponses = reponses;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  DTO Réponse — sans LocalDate
    // ─────────────────────────────────────────────────────────────
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReponseDto {
        public int     id;
        public String  contenuRep;
        public boolean isCorrect;
        public int     score;

        public ReponseDto() {}

        public ReponseDto(int id, String contenuRep,
                          boolean isCorrect, int score) {
            this.id         = id;
            this.contenuRep = contenuRep;
            this.isCorrect  = isCorrect;
            this.score      = score;
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Sérialisation
    // ─────────────────────────────────────────────────────────────
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String toJson() throws Exception {
        return MAPPER.writeValueAsString(this);
    }

    public static DuelMessage fromJson(String json) throws Exception {
        return MAPPER.readValue(json, DuelMessage.class);
    }
}