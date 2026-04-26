package com.example.pijava_fluently.controller;

import javafx.animation.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.*;
import javafx.util.Duration;

public class PersonnageSVG {

    public enum Expression { NEUTRE, CONTENT, TRISTE, SURPRISE }

    private Expression expressionCourante = Expression.NEUTRE;
    private Canvas     canvas;
    private GraphicsContext gc;
    private Pane       conteneur;
    private Timeline   animationClignement;
    private String     langue;

    private double oeilScaleY = 1.0;

    private static final double W = 200;
    private static final double H = 280;

    public Pane creerPersonnage(String langue) {
        this.langue    = langue;
        this.conteneur = new Pane();
        conteneur.setPrefSize(W, H);
        conteneur.setMaxSize(W, H);
        canvas = new Canvas(W, H);
        gc     = canvas.getGraphicsContext2D();
        conteneur.getChildren().add(canvas);
        dessiner();
        demarrerAnimations();
        return conteneur;
    }

    private void dessiner() {
        gc.clearRect(0, 0, W, H);
        switch (langue) {
            case "English"  -> dessinerJames();
            case "Espagnol" -> dessinerCarlos();
            default         -> dessinerPierre();
        }
    }

    // ════════════════════════════════════════════════
    //  PIERRE — Parisien  🇫🇷
    //  Couleurs : bleu marine + rouge + beige
    // ════════════════════════════════════════════════
    private void dessinerPierre() {

        // ── Jambes ──
        gc.setFill(Color.web("#1E3A6E"));
        gc.fillRoundRect(72, 210, 22, 60, 10, 10);
        gc.fillRoundRect(106, 210, 22, 60, 10, 10);

        // Chaussures
        gc.setFill(Color.web("#1A1A2E"));
        gc.fillRoundRect(66, 262, 34, 14, 10, 10);
        gc.fillRoundRect(100, 262, 34, 14, 10, 10);

        // ── Corps — veste bleue marine ──
        gc.setFill(Color.web("#1E3A6E"));
        gc.fillRoundRect(54, 155, 92, 70, 18, 18);

        // Boutons de la veste
        gc.setFill(Color.web("#F8FAFC"));
        gc.fillOval(96, 168, 8, 8);
        gc.fillOval(96, 184, 8, 8);
        gc.fillOval(96, 200, 8, 8);

        // ── Foulard rouge ──
        gc.setFill(Color.web("#EF4444"));
        gc.fillRoundRect(66, 150, 68, 22, 12, 12);
        // Nœud
        gc.setFill(Color.web("#DC2626"));
        gc.fillOval(88, 152, 24, 18);

        // ── Cou ──
        gc.setFill(Color.web("#FDBCB4"));
        gc.fillRoundRect(88, 138, 24, 24, 8, 8);

        // ── Tête — cercle propre beige ──
        gc.setFill(Color.web("#FDBCB4"));
        gc.fillOval(48, 58, 104, 100);

        // ── Béret bleu marine ──
        gc.setFill(Color.web("#1A2E4A"));
        gc.fillRoundRect(46, 78, 108, 16, 8, 8);  // bande
        gc.setFill(Color.web("#2A4880"));
        // Dôme du béret (ellipse décalée à droite comme un vrai béret)
        gc.fillOval(58, 42, 90, 48);
        // Bouton
        gc.setFill(Color.web("#1A2E4A"));
        gc.fillOval(134, 40, 12, 12);

        // ── Visage ──
        dessinerVisage(100, 118, "#1E3A6E");

        // ── Moustache  ──
        gc.setFill(Color.web("#5D4037"));
        // Gauche
        gc.fillRoundRect(72, 140, 22, 8, 8, 8);
        // Droite
        gc.fillRoundRect(106, 140, 22, 8, 8, 8);
    }

    // ════════════════════════════════════════════════
    //  JAMES — Gentleman britannique  🇬🇧
    //  Couleurs : gris anthracite + bordeaux + beige rosé
    // ════════════════════════════════════════════════
    private void dessinerJames() {

        // ── Jambes ──
        gc.setFill(Color.web("#374151"));
        gc.fillRoundRect(72, 210, 22, 60, 10, 10);
        gc.fillRoundRect(106, 210, 22, 60, 10, 10);

        gc.setFill(Color.web("#111827"));
        gc.fillRoundRect(66, 262, 34, 14, 10, 10);
        gc.fillRoundRect(100, 262, 34, 14, 10, 10);

        // ── Corps — costume gris ──
        gc.setFill(Color.web("#374151"));
        gc.fillRoundRect(52, 155, 96, 70, 18, 18);

        // Revers (forme trapèze)
        gc.setFill(Color.web("#4B5563"));
        gc.fillPolygon(
                new double[]{80, 100, 100, 80},
                new double[]{155, 155, 185, 205},
                4);
        gc.fillPolygon(
                new double[]{120, 100, 100, 120},
                new double[]{155, 155, 185, 205},
                4);

        // ── Cravate bordeaux ──
        gc.setFill(Color.web("#991B1B"));
        gc.fillPolygon(
                new double[]{94, 106, 104, 100, 96},
                new double[]{155, 155, 180, 218, 180},
                5);

        // ── Cou ──
        gc.setFill(Color.web("#FFE0D6"));
        gc.fillRoundRect(88, 138, 24, 24, 8, 8);

        // ── Tête — cercle rosé ──
        gc.setFill(Color.web("#FFE0D6"));
        gc.fillOval(48, 58, 104, 100);

        // Favoris (rectangles arrondis sur les côtés)
        gc.setFill(Color.web("#8B6914"));
        gc.fillRoundRect(48, 90, 16, 40, 8, 8);
        gc.fillRoundRect(136, 90, 16, 40, 8, 8);

        // ── Chapeau melon ──
        // Bord large
        gc.setFill(Color.web("#1F2937"));
        gc.fillRoundRect(38, 82, 124, 14, 6, 6);
        // Corps du chapeau
        gc.fillRoundRect(58, 30, 84, 56, 12, 12);
        // Ruban marron sur le chapeau
        gc.setFill(Color.web("#78350F"));
        gc.fillRect(58, 76, 84, 10);

        // ── Monocle ──
        gc.setStroke(Color.web("#D97706"));
        gc.setLineWidth(3);
        gc.strokeOval(116, 104, 30, 30);
        // Chaîne
        gc.setLineWidth(2);
        gc.strokeLine(146, 122, 155, 145);

        // ── Visage ──
        dessinerVisage(100, 118, "#1F2937");

        // Moustache guidon
        gc.setFill(Color.web("#78350F"));
        gc.fillRoundRect(72, 140, 14, 7, 6, 6);
        gc.fillRoundRect(114, 140, 14, 7, 6, 6);
        gc.fillRoundRect(85, 140, 30, 7, 4, 4);
    }

    // ════════════════════════════════════════════════
    //  CARLOS — Matador espagnol  🇪🇸
    //  Couleurs : rouge vif + or + teint hâlé
    // ════════════════════════════════════════════════
    private void dessinerCarlos() {

        // ── Jambes ──
        gc.setFill(Color.web("#1A1A1A"));
        gc.fillRoundRect(72, 210, 22, 60, 10, 10);
        gc.fillRoundRect(106, 210, 22, 60, 10, 10);

        gc.setFill(Color.web("#0A0A0A"));
        gc.fillRoundRect(66, 262, 34, 14, 10, 10);
        gc.fillRoundRect(100, 262, 34, 14, 10, 10);

        // ── Corps — veste rouge matador ──
        gc.setFill(Color.web("#DC2626"));
        gc.fillRoundRect(50, 155, 100, 70, 18, 18);

        // Épaulettes dorées
        gc.setFill(Color.web("#F59E0B"));
        gc.fillRoundRect(44, 155, 22, 14, 8, 8);
        gc.fillRoundRect(134, 155, 22, 14, 8, 8);

        // Broderies dorées
        gc.setFill(Color.web("#FCD34D"));
        gc.fillRoundRect(90, 162, 8, 55, 4, 4);
        // Petits motifs
        gc.fillOval(86, 175, 8, 8);
        gc.fillOval(106, 175, 8, 8);
        gc.fillOval(86, 193, 8, 8);
        gc.fillOval(106, 193, 8, 8);

        // ── Cou ──
        gc.setFill(Color.web("#C4956A"));
        gc.fillRoundRect(88, 138, 24, 24, 8, 8);

        // ── Tête — teint hâlé ──
        gc.setFill(Color.web("#C4956A"));
        gc.fillOval(48, 58, 104, 100);

        // Cheveux noirs
        gc.setFill(Color.web("#1A0F08"));
        gc.fillOval(48, 58, 104, 44);
        // Côtés des cheveux
        gc.fillRoundRect(48, 70, 16, 40, 8, 8);
        gc.fillRoundRect(136, 70, 16, 40, 8, 8);

        // ── Chapeau de matador (tricorne) ──
        gc.setFill(Color.web("#0A0A0A"));
        // Bande frontale
        gc.fillRoundRect(48, 82, 104, 12, 4, 4);
        // Corps
        gc.fillRoundRect(62, 38, 76, 48, 8, 8);
        // Bords relevés gauche et droit
        gc.fillPolygon(
                new double[]{34, 64, 64, 42},
                new double[]{76, 82, 72, 60},
                4);
        gc.fillPolygon(
                new double[]{166, 136, 136, 158},
                new double[]{76, 82, 72, 60},
                4);
        // Galons dorés
        gc.setFill(Color.web("#F59E0B"));
        gc.fillRoundRect(62, 38, 76, 6, 4, 4);
        gc.fillRoundRect(62, 80, 76, 4, 2, 2);

        // ── Visage ──
        dessinerVisage(100, 118, "#1A0F08");

        // Moustache fine Zorro
        gc.setFill(Color.web("#1A0F08"));
        gc.fillRoundRect(76, 140, 16, 6, 6, 6);
        gc.fillRoundRect(108, 140, 16, 6, 6, 6);
    }

    // ════════════════════════════════════════════════
    //  VISAGE COMMUN flat design
    // ════════════════════════════════════════════════
    private void dessinerVisage(double cx, double cy, String couleurYeux) {

        // ── Joues roses ──
        gc.setFill(Color.web("#FFB6C1", 0.6));
        gc.fillOval(cx - 48, cy + 6, 28, 18);
        gc.fillOval(cx + 20, cy + 6, 28, 18);

        // ── Yeux (blancs) ──
        double eyeH = 20 * oeilScaleY;
        double eyeOffY = (20 - eyeH) / 2.0;
        gc.setFill(Color.WHITE);
        gc.fillRoundRect(cx - 28, cy - 14 + eyeOffY, 22, eyeH, 11, 11);
        gc.fillRoundRect(cx + 6,  cy - 14 + eyeOffY, 22, eyeH, 11, 11);

        // ── Iris ──
        gc.setFill(Color.web(couleurYeux));
        gc.fillOval(cx - 22, cy - 10 + eyeOffY, 12, 12 * oeilScaleY);
        gc.fillOval(cx + 10, cy - 10 + eyeOffY, 12, 12 * oeilScaleY);

        // ── Reflets ──
        gc.setFill(Color.WHITE);
        gc.fillOval(cx - 20, cy - 10 + eyeOffY, 5, 5);
        gc.fillOval(cx + 12, cy - 10 + eyeOffY, 5, 5);

        // ── Sourcils ──
        gc.setFill(Color.web("#3E2723"));
        switch (expressionCourante) {
            case CONTENT -> {
                // Sourcils relevés (heureux)
                gc.fillRoundRect(cx - 30, cy - 28, 22, 6, 6, 6);
                gc.fillRoundRect(cx + 8,  cy - 28, 22, 6, 6, 6);
            }
            case TRISTE -> {
                // Sourcils froncés vers le bas (intérieur bas)
                gc.fillPolygon(
                        new double[]{cx-30, cx-8, cx-8, cx-30},
                        new double[]{cy-22, cy-26, cy-20, cy-18}, 4);
                gc.fillPolygon(
                        new double[]{cx+8, cx+30, cx+30, cx+8},
                        new double[]{cy-26, cy-22, cy-18, cy-20}, 4);
            }
            case SURPRISE -> {
                // Sourcils très hauts
                gc.fillRoundRect(cx - 30, cy - 34, 22, 6, 6, 6);
                gc.fillRoundRect(cx + 8,  cy - 34, 22, 6, 6, 6);
            }
            default -> {
                gc.fillRoundRect(cx - 30, cy - 24, 22, 6, 6, 6);
                gc.fillRoundRect(cx + 8,  cy - 24, 22, 6, 6, 6);
            }
        }

        // ── Nez (petit point discret) ──
        gc.setFill(Color.web("#D4917A", 0.5));
        gc.fillOval(cx - 5, cy + 8, 10, 7);

        // ── Bouche ──
        dessinerBouche(cx, cy);
    }

    private void dessinerBouche(double cx, double cy) {
        switch (expressionCourante) {

            case CONTENT -> {
                // Sourire large avec dents
                gc.setFill(Color.web("#1A1A1A"));
                gc.fillRoundRect(cx - 20, cy + 20, 40, 18, 12, 12);
                gc.setFill(Color.WHITE);
                gc.fillRoundRect(cx - 17, cy + 20, 34, 10, 6, 6);
            }

            case TRISTE -> {
                // Bouche en U inversé
                gc.setFill(Color.web("#1A1A1A"));
                gc.fillRoundRect(cx - 16, cy + 24, 32, 12, 10, 10);
                // Cacher la partie haute pour faire un arc vers le bas
                gc.setFill(Color.web("#FDBCB4"));  // couleur peau
                gc.fillRect(cx - 16, cy + 24, 32, 8);
                // Larme
                gc.setFill(Color.web("#93C5FD", 0.9));
                gc.fillPolygon(
                        new double[]{cx - 28, cx - 22, cx - 25},
                        new double[]{cy + 4,  cy + 4,  cy + 20},
                        3);
            }

            case SURPRISE -> {
                // Bouche "O"
                gc.setFill(Color.web("#1A1A1A"));
                gc.fillOval(cx - 12, cy + 18, 24, 22);
                gc.setFill(Color.web("#7F1D1D", 0.6));
                gc.fillOval(cx - 8, cy + 22, 16, 14);
            }

            default -> {
                // Sourire discret
                gc.setFill(Color.web("#1A1A1A"));
                gc.fillRoundRect(cx - 14, cy + 22, 28, 10, 8, 8);
                gc.setFill(Color.web("#FDBCB4"));
                gc.fillRect(cx - 14, cy + 22, 28, 6);
            }
        }
    }

    // ════════════════════════════════════════════════
    //  ANIMATIONS
    // ════════════════════════════════════════════════
    private void demarrerAnimations() {
        // Clignement toutes les 3 secondes
        animationClignement = new Timeline(
                new KeyFrame(Duration.seconds(3.0), e -> lancerClignement())
        );
        animationClignement.setCycleCount(Animation.INDEFINITE);
        animationClignement.play();

        // Légère respiration (translate Y)
        Timeline respiration = new Timeline(
                new KeyFrame(Duration.ZERO,        e -> canvas.setTranslateY(0)),
                new KeyFrame(Duration.seconds(1.2), e -> canvas.setTranslateY(4)),
                new KeyFrame(Duration.seconds(2.4), e -> canvas.setTranslateY(0))
        );
        respiration.setCycleCount(Animation.INDEFINITE);
        respiration.setAutoReverse(false);
        respiration.play();
    }

    private void lancerClignement() {
        Timeline clin = new Timeline(
                new KeyFrame(Duration.ZERO,        e -> { oeilScaleY = 1.0;  dessiner(); }),
                new KeyFrame(Duration.millis(70),  e -> { oeilScaleY = 0.15; dessiner(); }),
                new KeyFrame(Duration.millis(140), e -> { oeilScaleY = 1.0;  dessiner(); })
        );
        clin.play();
    }

    public void changerExpression(Expression expr) {
        this.expressionCourante = expr;
        dessiner();

        // Saut si CONTENT
        if (expr == Expression.CONTENT) {
            Timeline saut = new Timeline(
                    new KeyFrame(Duration.ZERO,        e -> canvas.setTranslateY(0)),
                    new KeyFrame(Duration.millis(100), e -> canvas.setTranslateY(-20)),
                    new KeyFrame(Duration.millis(200), e -> canvas.setTranslateY(-10)),
                    new KeyFrame(Duration.millis(300), e -> canvas.setTranslateY(-20)),
                    new KeyFrame(Duration.millis(400), e -> canvas.setTranslateY(0))
            );
            saut.play();
        }

        // Frisson si TRISTE
        if (expr == Expression.TRISTE) {
            Timeline frisson = new Timeline(
                    new KeyFrame(Duration.ZERO,        e -> canvas.setTranslateX(0)),
                    new KeyFrame(Duration.millis(60),  e -> canvas.setTranslateX(-7)),
                    new KeyFrame(Duration.millis(120), e -> canvas.setTranslateX(7)),
                    new KeyFrame(Duration.millis(180), e -> canvas.setTranslateX(-5)),
                    new KeyFrame(Duration.millis(240), e -> canvas.setTranslateX(5)),
                    new KeyFrame(Duration.millis(300), e -> canvas.setTranslateX(0))
            );
            frisson.play();
        }
    }

    public void arreterAnimations() {
        if (animationClignement != null) animationClignement.stop();
    }
}