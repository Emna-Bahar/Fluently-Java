package com.example.pijava_fluently.entites;

public class Question {
    private int id;
    private String enonce;
    private String type;
    private int scoreMax;
    private int testId;

    public Question() {}

    public Question(String enonce, String type, int scoreMax, int testId) {
        this.enonce = enonce;
        this.type = type;
        this.scoreMax = scoreMax;
        this.testId = testId;
    }

    public Question(int id, String enonce, String type, int scoreMax, int testId) {
        this.id = id;
        this.enonce = enonce;
        this.type = type;
        this.scoreMax = scoreMax;
        this.testId = testId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getEnonce() { return enonce; }
    public void setEnonce(String enonce) { this.enonce = enonce; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getScoreMax() { return scoreMax; }
    public void setScoreMax(int scoreMax) { this.scoreMax = scoreMax; }
    public int getTestId() { return testId; }
    public void setTestId(int testId) { this.testId = testId; }

    @Override
    public String toString() { return enonce; }
}