package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Session;
import com.example.pijava_fluently.services.Sessionservice;
import com.example.pijava_fluently.services.Reservationservice;
import com.example.pijava_fluently.entites.Reservation;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CalendrierController — style unifié light (violet/bleu) pour prof ET étudiant.
 * ✅ FIX : appliquerTheme() supprimé — le même style clair s'applique toujours.
 * ✅ FIX : pas de fallback diagnostic dans chargerSessionsUtilisateur().
 */
public class CalendrierController {

    @FXML private Label    labelMoisAnnee;
    @FXML private GridPane grilleCalendrier;
    @FXML private VBox     panneauDetail;
    @FXML private Label    labelDetail;
    @FXML private VBox     listeSessionsJour;
    @FXML private HBox     legendeBox;
    @FXML private Label    compteurSessions;
    @FXML private VBox     rootCalendrier;

    private final Sessionservice     sessionService     = new Sessionservice();
    private final Reservationservice reservationService = new Reservationservice();

    private YearMonth     moisCourant   = YearMonth.now();
    private int           currentUserId = -1;
    private boolean       isProfesseur  = false;
    private Map<LocalDate, List<Session>> sessionsByDate = new HashMap<>();

    private static final Map<String, String[]> STATUT_COLORS = Map.of(
            "planifiée", new String[]{"#3B82F6", "#DBEAFE"},
            "en cours",  new String[]{"#F59E0B", "#FEF3C7"},
            "terminée",  new String[]{"#10B981", "#D1FAE5"},
            "annulée",   new String[]{"#EF4444", "#FEE2E2"}
    );

    // ══════════════════════════════════════════════════════════════
    // INIT
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        if (panneauDetail != null) {
            panneauDetail.setVisible(false);
            panneauDetail.setManaged(false);
        }
        // ✅ Style unifié light appliqué immédiatement (pas de thème dark)
        appliquerStyleLight();
        construireLegendre();
    }

    /**
     * ✅ FIX PRINCIPAL : même style light pour prof ET étudiant.
     * L'ancien appliquerTheme() forçait un fond dark quand isProfesseur=true.
     */
    private void appliquerStyleLight() {
        if (rootCalendrier == null) return;
        rootCalendrier.setStyle(
                "-fx-background-color:linear-gradient(to bottom,#F0F4FF,#EBF0FF);" +
                        "-fx-padding:24;"
        );
    }

    public void setUserId(int id, boolean isProfesseur) {
        this.currentUserId = id;
        this.isProfesseur  = isProfesseur;
        // ✅ Pas d'appliquerTheme() ici — on garde toujours le style light
        chargerEtAfficher();
    }

    // ══════════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════════

    @FXML private void moisPrecedent()     { moisCourant = moisCourant.minusMonths(1); chargerEtAfficher(); }
    @FXML private void moisSuivant()       { moisCourant = moisCourant.plusMonths(1);  chargerEtAfficher(); }
    @FXML private void revenirAujourdhui() { moisCourant = YearMonth.now();            chargerEtAfficher(); }

    // ══════════════════════════════════════════════════════════════
    // CHARGEMENT
    // ══════════════════════════════════════════════════════════════

    private void chargerEtAfficher() {
        sessionsByDate.clear();
        try {
            List<Session> sessions = chargerSessionsUtilisateur();
            System.out.println("📅 Calendrier " + (isProfesseur ? "prof" : "étudiant") +
                    " : " + sessions.size() + " sessions pour " + moisCourant);
            for (Session s : sessions) {
                if (s.getDateHeure() == null) continue;
                LocalDate date = s.getDateHeure().toLocalDate();
                sessionsByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(s);
            }
            if (compteurSessions != null) {
                int t = sessions.size();
                compteurSessions.setText(t + " session" + (t > 1 ? "s" : ""));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement calendrier : " + e.getMessage());
        }
        afficherCalendrier();
    }

    /**
     * ✅ FIX CRITIQUE : pas de fallback diagnostic.
     * Si l'étudiant n'a aucune réservation acceptée/en attente → liste vide → calendrier vide.
     * C'est le comportement CORRECT.
     */
    private List<Session> chargerSessionsUtilisateur() throws SQLException {
        if (currentUserId == -1) return new ArrayList<>();

        if (isProfesseur) {
            // Prof : toutes ses sessions filtrées par mois courant
            return sessionService.recuperer().stream()
                    .filter(s -> s.getDateHeure() != null
                            && s.getDateHeure().getYear()       == moisCourant.getYear()
                            && s.getDateHeure().getMonthValue() == moisCourant.getMonthValue())
                    .collect(Collectors.toList());
        }

        // ── ÉTUDIANT ──────────────────────────────────────────────
        List<Reservation> resas = reservationService.recupererParEtudiant(currentUserId);
        System.out.println("  → " + resas.size() + " réservations brutes userId=" + currentUserId);

        // Collecte des IDs de sessions acceptées ou en attente uniquement
        Set<Integer> sessionIds = new HashSet<>();
        for (Reservation r : resas) {
            if (r.getStatut() == null) continue;
            String st = r.getStatut().trim().toLowerCase()
                    .replace("é", "e").replace("è", "e").replace("ê", "e");
            if (st.contains("accept") || st.contains("attente") || st.contains("confirm")) {
                sessionIds.add(r.getIdSessionId());
            }
        }

        System.out.println("  → " + sessionIds.size() + " sessionIds après filtre statut : " + sessionIds);

        // ✅ PAS DE FALLBACK — si 0 sessions acceptées, le calendrier est vide (correct)
        if (sessionIds.isEmpty()) {
            System.out.println("  ℹ Aucune réservation acceptée/en attente → calendrier vide");
            return new ArrayList<>();
        }

        // Charger toutes les sessions puis croiser avec les IDs + filtrer par mois
        List<Session> toutes = sessionService.recuperer();
        System.out.println("  → " + toutes.size() + " sessions totales en BD");

        List<Session> result = toutes.stream()
                .filter(s -> sessionIds.contains(s.getId()))
                .peek(s -> System.out.println("    ✅ Match session id=" + s.getId()
                        + " nom=" + s.getNom() + " date=" + s.getDateHeure()))
                .filter(s -> s.getDateHeure() != null
                        && s.getDateHeure().getYear()       == moisCourant.getYear()
                        && s.getDateHeure().getMonthValue() == moisCourant.getMonthValue())
                .collect(Collectors.toList());

        System.out.println("  → " + result.size() + " sessions dans le calendrier pour " + moisCourant);
        return result;
    }

    // ══════════════════════════════════════════════════════════════
    // AFFICHAGE CALENDRIER
    // ══════════════════════════════════════════════════════════════

    private void afficherCalendrier() {
        if (grilleCalendrier == null) return;
        grilleCalendrier.getChildren().clear();
        grilleCalendrier.getRowConstraints().clear();

        // ✅ Style unifié light pour la grille (prof et étudiant identiques)
        grilleCalendrier.setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:20;" +
                        "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.10),24,0,0,8);" +
                        "-fx-border-color:#EDE9FE;" +
                        "-fx-border-radius:20;-fx-border-width:1;-fx-padding:12;"
        );

        String[] moisFr = {"JANVIER","FÉVRIER","MARS","AVRIL","MAI","JUIN",
                "JUILLET","AOÛT","SEPTEMBRE","OCTOBRE","NOVEMBRE","DÉCEMBRE"};
        if (labelMoisAnnee != null)
            labelMoisAnnee.setText(moisFr[moisCourant.getMonthValue() - 1] + "  " + moisCourant.getYear());

        // En-têtes jours
        String[] jours = {"LUN","MAR","MER","JEU","VEN","SAM","DIM"};
        for (int i = 0; i < 7; i++) {
            Label h = new Label(jours[i]);
            h.setMaxWidth(Double.MAX_VALUE);
            h.setAlignment(Pos.CENTER);
            boolean isWE = (i == 5 || i == 6);
            h.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:10 4 10 4;" +
                    "-fx-text-fill:" + (isWE ? "#7C3AED" : "#94A3B8") + ";");
            grilleCalendrier.add(h, i, 0);
        }

        LocalDate premier    = moisCourant.atDay(1);
        int       decalage   = premier.getDayOfWeek().getValue() - 1;
        int       nbJours    = moisCourant.lengthOfMonth();
        LocalDate aujourdhui = LocalDate.now();

        int col = decalage, row = 1;
        for (int jour = 1; jour <= nbJours; jour++) {
            LocalDate d = moisCourant.atDay(jour);
            List<Session> s = sessionsByDate.getOrDefault(d, new ArrayList<>());
            grilleCalendrier.add(construireCellule(d, s, aujourdhui), col, row);
            if (++col == 7) { col = 0; row++; }
        }

        RowConstraints hdr = new RowConstraints(40);
        grilleCalendrier.getRowConstraints().add(hdr);
        int nbLignes = row + (col > 0 ? 1 : 0);
        for (int i = 1; i <= nbLignes; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setMinHeight(88);
            rc.setPrefHeight(96);
            grilleCalendrier.getRowConstraints().add(rc);
        }

        grilleCalendrier.setOpacity(0);
        FadeTransition fd = new FadeTransition(Duration.millis(300), grilleCalendrier);
        fd.setFromValue(0);
        fd.setToValue(1);
        fd.play();
    }

    // ══════════════════════════════════════════════════════════════
    // CELLULE — style light unifié
    // ══════════════════════════════════════════════════════════════

    private VBox construireCellule(LocalDate date, List<Session> sessions, LocalDate aujourdhui) {
        boolean aujo      = date.equals(aujourdhui);
        boolean passe     = date.isBefore(aujourdhui);
        boolean hasEvents = !sessions.isEmpty();

        VBox cell = new VBox(3);
        cell.setPadding(new Insets(6, 5, 5, 5));
        cell.setMinHeight(88);
        cell.setCursor(javafx.scene.Cursor.HAND);

        String cellStyle = getCellStyle(aujo, passe, hasEvents);
        cell.setStyle(cellStyle);

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_RIGHT);
        Label numLbl = new Label(String.valueOf(date.getDayOfMonth()));

        if (aujo) {
            numLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:white;" +
                    "-fx-background-color:#7C3AED;-fx-background-radius:50;" +
                    "-fx-min-width:26;-fx-min-height:26;-fx-alignment:CENTER;-fx-padding:2 6 2 6;");
        } else if (passe) {
            numLbl.setStyle("-fx-font-size:13px;-fx-text-fill:#CBD5E1;");
        } else {
            numLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1E293B;");
        }

        if (hasEvents && !aujo) {
            Region dot = new Region();
            dot.setPrefSize(6, 6);
            dot.setMinSize(6, 6);
            dot.setStyle("-fx-background-color:" + getStatutColor(sessions.get(0).getStatut(), 0) +
                    ";-fx-background-radius:50;");
            HBox.setMargin(dot, new Insets(6, 0, 0, 4));
            topRow.getChildren().add(dot);
        }
        topRow.getChildren().add(numLbl);
        cell.getChildren().add(topRow);

        // Badges sessions (max 2)
        int max = Math.min(sessions.size(), 2);
        for (int i = 0; i < max; i++) {
            Session s    = sessions.get(i);
            String c     = getStatutColor(s.getStatut(), 0);
            String bg    = getStatutColor(s.getStatut(), 1);
            String heure = s.getDateHeure() != null
                    ? s.getDateHeure().format(DateTimeFormatter.ofPattern("HH:mm")) : "";
            String nom   = s.getNom() != null && !s.getNom().isBlank()
                    ? (s.getNom().length() > 10 ? s.getNom().substring(0, 8) + "…" : s.getNom())
                    : "Session";
            Label badge = new Label("● " + heure + " " + nom);
            badge.setMaxWidth(Double.MAX_VALUE);
            badge.setStyle("-fx-background-color:" + bg + ";" +
                    "-fx-text-fill:" + c + ";-fx-font-size:9px;-fx-font-weight:bold;" +
                    "-fx-background-radius:5;-fx-padding:2 5 2 5;");
            cell.getChildren().add(badge);
            badge.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(200), badge);
            ft.setDelay(Duration.millis(i * 80 + 100));
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }

        if (sessions.size() > 2) {
            Label plus = new Label("+" + (sessions.size() - 2) + " autres");
            plus.setStyle("-fx-font-size:9px;-fx-text-fill:#94A3B8;-fx-padding:1 0 0 2;");
            cell.getChildren().add(plus);
        }

        cell.setOnMouseEntered(e -> cell.setStyle(cellStyle +
                "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.18),14,0,0,4);" +
                "-fx-scale-x:1.03;-fx-scale-y:1.03;"));
        cell.setOnMouseExited(e -> cell.setStyle(cellStyle));
        cell.setOnMouseClicked(e -> afficherDetailJour(date, sessions));
        return cell;
    }

    /**
     * ✅ Style light unifié — même rendu pour prof et étudiant.
     */
    private String getCellStyle(boolean aujo, boolean passe, boolean hasEvents) {
        if (aujo)
            return "-fx-background-color:#F5F3FF;-fx-background-radius:12;" +
                    "-fx-border-color:#7C3AED;-fx-border-radius:12;-fx-border-width:2;";
        if (hasEvents)
            return "-fx-background-color:#FAF5FF;-fx-background-radius:12;" +
                    "-fx-border-color:#DDD6FE;-fx-border-radius:12;-fx-border-width:1;";
        if (passe)
            return "-fx-background-color:#F9FAFB;-fx-background-radius:12;" +
                    "-fx-border-color:#F1F5F9;-fx-border-radius:12;-fx-border-width:1;";
        return "-fx-background-color:white;-fx-background-radius:12;" +
                "-fx-border-color:#E2E8F0;-fx-border-radius:12;-fx-border-width:1;";
    }

    // ══════════════════════════════════════════════════════════════
    // PANNEAU DÉTAIL
    // ══════════════════════════════════════════════════════════════

    private void afficherDetailJour(LocalDate date, List<Session> sessions) {
        if (panneauDetail == null || listeSessionsJour == null) return;
        panneauDetail.setVisible(true);
        panneauDetail.setManaged(true);
        listeSessionsJour.getChildren().clear();

        // ✅ Style light unifié pour le panneau détail
        panneauDetail.setStyle(
                "-fx-background-color:white;" +
                        "-fx-border-color:#EDE9FE;" +
                        "-fx-background-radius:18;-fx-border-radius:18;-fx-border-width:1;" +
                        "-fx-padding:18;" +
                        "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.12),16,0,0,5);"
        );

        panneauDetail.setOpacity(0);
        panneauDetail.setTranslateX(20);
        FadeTransition fd = new FadeTransition(Duration.millis(250), panneauDetail);
        fd.setFromValue(0); fd.setToValue(1);
        TranslateTransition td = new TranslateTransition(Duration.millis(250), panneauDetail);
        td.setFromX(20); td.setToX(0);
        td.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fd, td).play();

        String[] moisFr = {"janv.","févr.","mars","avr.","mai","juin",
                "juil.","août","sept.","oct.","nov.","déc."};
        String jourSemaine = switch (date.getDayOfWeek()) {
            case MONDAY    -> "Lundi";
            case TUESDAY   -> "Mardi";
            case WEDNESDAY -> "Mercredi";
            case THURSDAY  -> "Jeudi";
            case FRIDAY    -> "Vendredi";
            case SATURDAY  -> "Samedi";
            default        -> "Dimanche";
        };

        if (labelDetail != null) {
            labelDetail.setText(jourSemaine + " " + date.getDayOfMonth() +
                    " " + moisFr[date.getMonthValue() - 1] + " " + date.getYear());
            labelDetail.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#1E293B;");
        }

        if (sessions.isEmpty()) {
            VBox empty = new VBox(8);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(30));
            Label ic  = new Label("📭");
            ic.setStyle("-fx-font-size:32px;");
            Label msg = new Label("Aucune session");
            msg.setStyle("-fx-font-size:13px;-fx-text-fill:#94A3B8;");
            empty.getChildren().addAll(ic, msg);
            listeSessionsJour.getChildren().add(empty);
            return;
        }

        int delay = 0;
        for (Session s : sessions) {
            VBox card = construireCarteSessionDetail(s);
            card.setOpacity(0);
            card.setTranslateY(10);
            listeSessionsJour.getChildren().add(card);
            FadeTransition f = new FadeTransition(Duration.millis(220), card);
            f.setDelay(Duration.millis(delay));
            f.setFromValue(0);
            f.setToValue(1);
            TranslateTransition t = new TranslateTransition(Duration.millis(220), card);
            t.setDelay(Duration.millis(delay));
            t.setFromY(10);
            t.setToY(0);
            t.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(f, t).play();
            delay += 65;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // CARTE SESSION DÉTAIL — style light unifié
    // ══════════════════════════════════════════════════════════════

    private VBox construireCarteSessionDetail(Session s) {
        String couleur = getStatutColor(s.getStatut(), 0);
        String bgClair = getStatutColor(s.getStatut(), 1);
        String nom = (s.getNom() != null && !s.getNom().isBlank())
                ? s.getNom() : "Session #" + s.getId();

        VBox card = new VBox(10);
        card.setPadding(new Insets(14, 16, 14, 16));
        // ✅ Fond clair pour les deux rôles
        card.setStyle(
                "-fx-background-color:" + bgClair + ";" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:" + couleur + ";-fx-border-radius:0;" +
                        "-fx-border-width:0 0 0 4;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,3);"
        );

        // Nom + statut
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill:" + couleur + ";-fx-font-size:14px;");
        Label nomLbl = new Label(nom);
        nomLbl.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#1E293B;-fx-wrap-text:true;");
        nomLbl.setWrapText(true);
        HBox.setHgrow(nomLbl, Priority.ALWAYS);
        Label badge = new Label(s.getStatut() != null ? s.getStatut() : "—");
        badge.setStyle("-fx-background-color:" + couleur + ";-fx-text-fill:white;" +
                "-fx-font-size:9px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 8 3 8;");
        topRow.getChildren().addAll(dot, nomLbl, badge);

        // Infos (heure, durée, prix)
        HBox infoRow = new HBox(10);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        if (s.getDateHeure() != null) {
            Label hL = new Label("🕐 " + s.getDateHeure().format(DateTimeFormatter.ofPattern("HH:mm")));
            hL.setStyle(styleInfo());
            infoRow.getChildren().add(hL);
        }
        if (s.getDuree() != null) {
            Label dL = new Label("⏱ " + s.getDuree() + " min");
            dL.setStyle(styleInfo());
            infoRow.getChildren().add(dL);
        }
        if (s.getPrix() != null) {
            Label pL = new Label("💰 " + String.format("%.0f", s.getPrix()) + " TND");
            pL.setStyle(styleInfo());
            infoRow.getChildren().add(pL);
        }
        card.getChildren().addAll(topRow, infoRow);

        // Description
        if (s.getDescription() != null && !s.getDescription().isBlank()) {
            String desc = s.getDescription().length() > 60
                    ? s.getDescription().substring(0, 57) + "…" : s.getDescription();
            Label descL = new Label(desc);
            descL.setStyle("-fx-font-size:10px;-fx-text-fill:#64748B;-fx-wrap-text:true;");
            descL.setWrapText(true);
            card.getChildren().add(descL);
        }

        // Bouton rejoindre
        if (s.getLienReunion() != null && !s.getLienReunion().isBlank()) {
            Button btnJitsi = new Button("🎥  Rejoindre la réunion");
            btnJitsi.setMaxWidth(Double.MAX_VALUE);
            btnJitsi.setStyle("-fx-background-color:" + couleur + ";-fx-text-fill:white;" +
                    "-fx-font-size:11px;-fx-font-weight:bold;" +
                    "-fx-background-radius:10;-fx-padding:7 14 7 14;-fx-cursor:hand;");
            btnJitsi.setOnMouseEntered(e -> btnJitsi.setOpacity(0.85));
            btnJitsi.setOnMouseExited(e  -> btnJitsi.setOpacity(1.0));
            btnJitsi.setOnAction(e ->
                    com.example.pijava_fluently.utils.JitsiUtil.ouvrirDansNavigateur(s.getLienReunion()));
            card.getChildren().add(btnJitsi);
        }

        return card;
    }

    // ══════════════════════════════════════════════════════════════
    // LÉGENDE
    // ══════════════════════════════════════════════════════════════

    private void construireLegendre() {
        if (legendeBox == null) return;
        legendeBox.getChildren().clear();

        // ✅ Style light pour la légende
        legendeBox.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:12;" +
                        "-fx-padding:10 18 10 18;" +
                        "-fx-border-color:#E2E8F0;" +
                        "-fx-border-radius:12;-fx-border-width:1;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.04),8,0,0,2);"
        );

        String[][] items = {
                {"planifiée", "#3B82F6"},
                {"en cours",  "#F59E0B"},
                {"terminée",  "#10B981"},
                {"annulée",   "#EF4444"}
        };
        for (String[] item : items) {
            HBox entry = new HBox(6);
            entry.setAlignment(Pos.CENTER_LEFT);
            entry.setPadding(new Insets(4, 12, 4, 12));
            entry.setStyle("-fx-background-color:" + item[1] + "22;-fx-background-radius:20;");
            Region d = new Region();
            d.setPrefSize(8, 8);
            d.setMinSize(8, 8);
            d.setStyle("-fx-background-color:" + item[1] + ";-fx-background-radius:50;");
            Label lbl = new Label(item[0]);
            lbl.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:" + item[1] + ";");
            entry.getChildren().addAll(d, lbl);
            legendeBox.getChildren().add(entry);
        }
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════

    private String styleInfo() {
        // ✅ Couleur light pour les deux rôles
        return "-fx-font-size:11px;-fx-text-fill:#64748B;-fx-font-weight:bold;";
    }

    private String getStatutColor(String statut, int index) {
        String[] colors = STATUT_COLORS.getOrDefault(
                statut != null ? statut : "",
                new String[]{"#7C3AED", "#F5F3FF"});
        return index < colors.length ? colors[index] : colors[0];
    }
}