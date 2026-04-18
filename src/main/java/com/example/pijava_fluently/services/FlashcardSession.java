package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Flashcard;
import java.util.ArrayList;
import java.util.List;

public class FlashcardSession {
    private List<Flashcard> flashcards;
    private List<Boolean> userAnswers;
    private List<Integer> userSelectedAnswers;
    private int currentIndex;
    private int score;

    public FlashcardSession() {
        this.flashcards = new ArrayList<>();
        this.userAnswers = new ArrayList<>();
        this.userSelectedAnswers = new ArrayList<>();
        this.currentIndex = 0;
        this.score = 0;
    }

    public void setFlashcards(List<Flashcard> flashcards) {
        this.flashcards = flashcards;
        this.userAnswers = new ArrayList<>();
        this.userSelectedAnswers = new ArrayList<>();
        this.currentIndex = 0;
        this.score = 0;
        for (int i = 0; i < flashcards.size(); i++) {
            userAnswers.add(false);
            userSelectedAnswers.add(-1);
        }
    }

    public List<Flashcard> getFlashcards() { return flashcards; }
    public int getCurrentIndex() { return currentIndex; }
    public Flashcard getCurrentFlashcard() {
        if (currentIndex < flashcards.size()) return flashcards.get(currentIndex);
        return null;
    }
    public int getScore() { return score; }
    public int getTotalQuestions() { return flashcards.size(); }
    public boolean isFinished() { return currentIndex >= flashcards.size(); }

    public void recordAnswer(int selectedOption, boolean isCorrect) {
        if (currentIndex < flashcards.size()) {
            userAnswers.set(currentIndex, isCorrect);
            userSelectedAnswers.set(currentIndex, selectedOption);
            if (isCorrect) score++;
            currentIndex++;
        }
    }

    public List<Integer> getWrongQuestionsIndices() {
        List<Integer> wrong = new ArrayList<>();
        for (int i = 0; i < userAnswers.size(); i++) {
            if (!userAnswers.get(i)) wrong.add(i);
        }
        return wrong;
    }

    public String generateAnalysis() {
        StringBuilder analysis = new StringBuilder();
        int total = flashcards.size();
        int correct = score;
        int wrong = total - correct;
        double percentage = (double) correct / total * 100;

        analysis.append("📊 ANALYSE DE VOS RÉPONSES\n");
        analysis.append("━".repeat(40)).append("\n\n");
        analysis.append("✅ Réponses correctes : ").append(correct).append("/").append(total).append("\n");
        analysis.append("❌ Réponses incorrectes : ").append(wrong).append("/").append(total).append("\n");
        analysis.append("📈 Taux de réussite : ").append(String.format("%.1f", percentage)).append("%\n\n");

        if (percentage >= 80) {
            analysis.append("🏆 EXCELLENT ! Vous maîtrisez très bien ce sujet !\n");
            analysis.append("👉 Continuez comme ça, vous êtes sur la bonne voie !\n");
        } else if (percentage >= 60) {
            analysis.append("👍 TRÈS BIEN ! Quelques petites lacunes à combler.\n");
            analysis.append("👉 Révisez les cartes où vous avez fait des erreurs.\n");
        } else if (percentage >= 40) {
            analysis.append("📚 PAS MAL ! Mais il faut plus de pratique.\n");
            analysis.append("👉 Concentrez-vous sur les explications fournies.\n");
        } else {
            analysis.append("💪 CONTINUEZ VOS EFFORTS ! C'est en forgeant qu'on devient forgeron.\n");
            analysis.append("👉 Revoyez toutes les flashcards et réessayez.\n");
        }

        analysis.append("\n📝 DÉTAIL DES ERREURS :\n");
        analysis.append("━".repeat(40)).append("\n");

        List<Integer> wrongIndices = getWrongQuestionsIndices();
        if (wrongIndices.isEmpty()) {
            analysis.append("🎉 Aucune erreur ! Félicitations !\n");
        } else {
            for (int idx : wrongIndices) {
                Flashcard card = flashcards.get(idx);
                int userAnswer = userSelectedAnswers.get(idx);
                analysis.append("\n❌ Carte ").append(idx + 1).append(" : \n");
                analysis.append("   Question : ").append(card.getQuestion()).append("\n");
                analysis.append("   Votre réponse : ").append(card.getOptions()[userAnswer - 1]).append("\n");
                analysis.append("   Bonne réponse : ").append(card.getOptions()[card.getCorrectAnswer() - 1]).append("\n");
                analysis.append("   💡 Explication : ").append(card.getExplanation()).append("\n");
            }
        }

        // Conseils généraux basés sur les types d'erreurs
        analysis.append("\n🎯 CONSEILS POUR PROGRESSER :\n");
        analysis.append("━".repeat(40)).append("\n");

        // Compter les types d'erreurs (basé sur les explications)
        int grammarErrors = 0, vocabErrors = 0, conjugationErrors = 0;
        for (int idx : wrongIndices) {
            String explanation = flashcards.get(idx).getExplanation().toLowerCase();
            if (explanation.contains("grammaire") || explanation.contains("règle")) grammarErrors++;
            if (explanation.contains("vocabulaire") || explanation.contains("mot")) vocabErrors++;
            if (explanation.contains("conjug") || explanation.contains("verbe")) conjugationErrors++;
        }

        if (grammarErrors > 0) {
            analysis.append("📖 Grammaire : Révisez les règles de grammaire de ce thème.\n");
        }
        if (vocabErrors > 0) {
            analysis.append("📝 Vocabulaire : Apprenez plus de mots sur ce sujet.\n");
        }
        if (conjugationErrors > 0) {
            analysis.append("🔤 Conjugaison : Entraînez-vous avec des exercices de conjugaison.\n");
        }

        analysis.append("\n💪 N'oubliez pas : la pratique régulière est la clé du succès !\n");

        return analysis.toString();
    }
}