package com.example.pijava_fluently.entites;

public class FillQuestion {
    private final String phrase;
    private final String[] options;
    private final int correctAnswer;

    public FillQuestion(String phrase, String[] options, int correctAnswer) {
        this.phrase = phrase;
        this.options = options;
        this.correctAnswer = correctAnswer;
    }

    public String getPhrase() { return phrase; }
    public String[] getOptions() { return options; }
    public int getCorrectAnswer() { return correctAnswer; }
}