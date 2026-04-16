package com.example.pijava_fluently.entites;

public class QuizQuestion {
    private final String question;
    private final String[] options;
    private final int correctAnswer;

    public QuizQuestion(String question, String[] options, int correctAnswer) {
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
    }

    public String getQuestion() { return question; }
    public String[] getOptions() { return options; }
    public int getCorrectAnswer() { return correctAnswer; }
}
