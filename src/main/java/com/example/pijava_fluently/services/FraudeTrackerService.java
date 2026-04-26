package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.LoggerUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Stocke les infractions de chaque étudiant dans un fichier JSON local.
 * Pas de nouvelle table BD requise.
 */
public class FraudeTrackerService {

    private static final String FRAUDE_DIR  = "fraude_logs/";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record Infraction(
            String date,
            String raison,
            int    testId,
            String testTitre
    ) {}

    public record ProfilFraude(
            int              userId,
            int              totalInfractions,
            int              maxInfractionsParTest,
            List<Infraction> historique
    ) {}

    // ── Enregistrer une infraction ────────────────────────────────
    public void enregistrer(int userId, String raison,
                            int testId, String testTitre) {
        try {
            Files.createDirectories(Paths.get(FRAUDE_DIR));
            File f = new File(FRAUDE_DIR + "user_" + userId + ".json");

            List<Infraction> liste = charger(userId);
            liste.add(new Infraction(
                    LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                    raison, testId, testTitre));

            MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValue(f, liste);
            LoggerUtil.info("Infraction enregistrée",
                    "userId", String.valueOf(userId), "raison", raison);

        } catch (Exception e) {
            LoggerUtil.error("Erreur enregistrement fraude", e);
        }
    }

    // ── Charger les infractions d'un utilisateur ──────────────────
    @SuppressWarnings("unchecked")
    public List<Infraction> charger(int userId) {
        try {
            File f = new File(FRAUDE_DIR + "user_" + userId + ".json");
            if (!f.exists()) return new ArrayList<>();
            return MAPPER.readValue(f,
                    MAPPER.getTypeFactory().constructCollectionType(
                            List.class, Infraction.class));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // ── Compter les infractions sur le dernier test ───────────────
    public int compterInfractionsTestPrecedent(int userId, int testId) {
        return (int) charger(userId).stream()
                .filter(i -> i.testId() == testId)
                .count();
    }

    // ── Max tentatives autorisées selon historique ────────────────
    /**
     * Règle progressive :
     * 0-2 infractions totales → 3 tentatives
     * 3-5 infractions totales → 2 tentatives
     * 6+ infractions totales  → 1 seule tentative
     */
    public int getMaxTentativesAutorisees(int userId) {
        int total = charger(userId).size();
        if (total >= 6) return 1;
        if (total >= 3) return 2;
        return 3;
    }

    // ── Profil complet (pour l'admin) ─────────────────────────────
    public ProfilFraude getProfilComplet(int userId) {
        List<Infraction> hist = charger(userId);

        // Compter par test
        Map<Integer, Long> parTest = new HashMap<>();
        for (Infraction i : hist) {
            parTest.merge(i.testId(), 1L, Long::sum);
        }
        int maxParTest = parTest.values().stream()
                .mapToInt(Long::intValue).max().orElse(0);

        return new ProfilFraude(userId, hist.size(), maxParTest, hist);
    }

    // ── Lister tous les utilisateurs ayant des infractions ────────
    public List<Integer> getUsersAvecInfractions() {
        List<Integer> ids = new ArrayList<>();
        File dir = new File(FRAUDE_DIR);
        if (!dir.exists()) return ids;
        for (File f : Objects.requireNonNull(dir.listFiles())) {
            String name = f.getName();
            if (name.startsWith("user_") && name.endsWith(".json")) {
                try {
                    int id = Integer.parseInt(
                            name.replace("user_", "").replace(".json", ""));
                    ids.add(id);
                } catch (NumberFormatException ignored) {}
            }
        }
        return ids;
    }
}