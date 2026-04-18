package com.example.pijava_fluently.entites;

public class Flashcard {
    private String question;
    private String[] options;
    private int correctAnswer; // 1-4
    private String explanation; // Théorie / explication

    public Flashcard(String question, String[] options, int correctAnswer, String explanation) {
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
    }

    public String getQuestion() { return question; }
    public String[] getOptions() { return options; }
    public int getCorrectAnswer() { return correctAnswer; }
    public String getExplanation() { return explanation; }
}