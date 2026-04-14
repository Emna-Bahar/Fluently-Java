package com.example.pijava_fluently.entites;

import java.time.LocalDate;

public class Reponse {
    private int id;
    private String contenuRep;
    private boolean isCorrect;
    private int score;
    private LocalDate dateReponse;
    private int questionId;

    public Reponse() {}

    public Reponse(String contenuRep, boolean isCorrect, int score, LocalDate dateReponse, int questionId) {
        this.contenuRep = contenuRep;
        this.isCorrect = isCorrect;
        this.score = score;
        this.dateReponse = dateReponse;
        this.questionId = questionId;
    }

    public Reponse(int id, String contenuRep, boolean isCorrect, int score, LocalDate dateReponse, int questionId) {
        this.id = id;
        this.contenuRep = contenuRep;
        this.isCorrect = isCorrect;
        this.score = score;
        this.dateReponse = dateReponse;
        this.questionId = questionId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getContenuRep() { return contenuRep; }
    public void setContenuRep(String contenuRep) { this.contenuRep = contenuRep; }
    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public LocalDate getDateReponse() { return dateReponse; }
    public void setDateReponse(LocalDate dateReponse) { this.dateReponse = dateReponse; }
    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }

    @Override
    public String toString() { return contenuRep; }
}