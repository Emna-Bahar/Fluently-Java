package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Session;
import com.example.pijava_fluently.utils.MyDatabase;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class RecommandationService {

    private static String GROQ_URL;
    private static String GROQ_KEY;
    private static String GROQ_MODEL;

    static {
        // Load configuration from environment variables
        GROQ_URL = System.getenv("GROQ_API_URL");
        GROQ_KEY = System.getenv("GROQ_API_KEY");
        GROQ_MODEL = System.getenv("GROQ_MODEL");

        // Fallback to system properties if environment variables not set
        if (GROQ_URL == null) GROQ_URL = System.getProperty("groq.api.url", "https://api.groq.com/openai/v1/chat/completions");
        if (GROQ_KEY == null) GROQ_KEY = System.getProperty("groq.api.key");
        if (GROQ_MODEL == null) GROQ_MODEL = System.getProperty("groq.model", "llama-3.3-70b-versatile");

        // Don't fail during initialization - check when actually used
        // if (GROQ_KEY == null || GROQ_KEY.trim().isEmpty()) {
        //     throw new ExceptionInInitializerError("GROQ_API_KEY environment variable or system property must be set");
        // }
    }

    private static final String STATUT_ANNULEE   = "annulée";
    private static final String STATUT_PLANIFIEE = "planifiée";
    private static final String STATUT_EN_COURS  = "en cours";

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    private final Map<Integer, String[]> lastRaisons    = new LinkedHashMap<>();
    private final Map<Integer, String>   aiExplanations = new LinkedHashMap<>();

    public Map<Integer, String[]> getLastRaisons()    { return lastRaisons; }
    public Map<Integer, String>   getAIExplanations() { return aiExplanations; }

    public List<Session> getRecommandations(int userId) throws SQLException {
        lastRaisons.clear();
        aiExplanations.clear();

        // Check if Groq API is properly configured
        if (GROQ_KEY == null || GROQ_KEY.trim().isEmpty()) {
            System.out.println("[RecommandationService] GROQ_API_KEY not configured, using SQL fallback");
            return fallbackSQL(userId);
        }

        try {
        try {
            String        userProfile = buildUserProfile(userId);
            List<Session> candidates  = fetchCandidateSessions(userId);

            if (candidates.isEmpty()) return Collections.emptyList();

            String sessionsContext = buildSessionsContext(candidates);
            int nbARecommander = Math.min(3, candidates.size());
            String aiJson = callGroqAPI(userProfile, sessionsContext, nbARecommander);

            List<Session> result = parseAIResponse(aiJson, candidates);

            for (Session s : result) {
                String expl = aiExplanations.getOrDefault(s.getId(),
                        "Recommandé par l'IA selon ton profil.");
                lastRaisons.put(s.getId(), new String[]{
                        "IA — Personnalisée",
                        expl,
                        "Analysé par Groq IA (Llama3)",
                        "Basé sur tes langues et ton historique"
                });
            }
            return result;

        } catch (Exception e) {
            System.err.println("[Groq IA] Indisponible, fallback SQL : " + e.getMessage());
            return fallbackSQL(userId);
        }
    }

    private String buildUserProfile(int userId) throws SQLException {
        StringBuilder sb = new StringBuilder();

        String sqlLangues =
                "SELECT DISTINCT l.nom, g.nom as groupe_nom, n.titre as niveau_titre " +
                        "FROM groupe_user gu " +
                        "JOIN groupe g ON g.id  = gu.groupe_id " +
                        "JOIN langue l ON l.id  = g.id_langue_id " +
                        "JOIN niveau n ON n.id  = g.id_niveau_id " +
                        "WHERE gu.user_id = ?";

        sb.append("LANGUES ET GROUPES DE L'ÉTUDIANT:\n");
        try (PreparedStatement ps = cnx.prepareStatement(sqlLangues)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            boolean found = false;
            while (rs.next()) {
                found = true;
                sb.append("  - Langue: ").append(rs.getString("nom"))
                        .append(", Groupe: ").append(rs.getString("groupe_nom"))
                        .append(", Niveau: ").append(rs.getString("niveau_titre"))
                        .append("\n");
            }
            if (!found) sb.append("  (aucun groupe inscrit)\n");
        }

        String sqlHisto =
                "SELECT s.description, s.nom as session_nom, l.nom as langue, r.statut as resa_statut " +
                        "FROM reservation r " +
                        "JOIN session s ON s.id = r.id_session_id " +
                        "JOIN groupe  g ON g.id = s.id_group_id " +
                        "JOIN langue  l ON l.id = g.id_langue_id " +
                        "WHERE r.id_user_id = ? AND r.statut != '" + STATUT_ANNULEE + "' " +
                        "ORDER BY r.date_reservation DESC LIMIT 5";

        sb.append("\nHISTORIQUE RÉCENT DES RÉSERVATIONS:\n");
        try (PreparedStatement ps = cnx.prepareStatement(sqlHisto)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            boolean found = false;
            while (rs.next()) {
                found = true;
                String nomSession = rs.getString("session_nom");
                String display = (nomSession != null && !nomSession.isBlank())
                        ? nomSession : rs.getString("description");
                sb.append("  - '").append(display)
                        .append("' en ").append(rs.getString("langue"))
                        .append(", statut: ").append(rs.getString("resa_statut"))
                        .append("\n");
            }
            if (!found) sb.append("  (aucune réservation précédente)\n");
        }

        return sb.toString();
    }

    private List<Session> fetchCandidateSessions(int userId) throws SQLException {
        String sql =
                "SELECT s.*, l.nom as langue_nom, g.nom as groupe_nom, " +
                        "       n.titre as niveau_titre, " +
                        "       (gu.user_id IS NOT NULL) as est_dans_groupe " +
                        "FROM session s " +
                        "JOIN groupe g ON g.id = s.id_group_id " +
                        "JOIN langue l ON l.id = g.id_langue_id " +
                        "JOIN niveau n ON n.id = g.id_niveau_id " +
                        "LEFT JOIN groupe_user gu ON gu.groupe_id = g.id AND gu.user_id = ? " +
                        "WHERE s.statut IN ('" + STATUT_PLANIFIEE + "', '" + STATUT_EN_COURS + "') " +
                        "AND s.id NOT IN (" +
                        "    SELECT id_session_id FROM reservation " +
                        "    WHERE id_user_id = ? AND statut != '" + STATUT_ANNULEE + "'" +
                        ") " +
                        "AND s.capacite_max > (" +
                        "    SELECT COUNT(*) FROM reservation r2 " +
                        "    WHERE r2.id_session_id = s.id AND r2.statut != '" + STATUT_ANNULEE + "'" +
                        ") " +
                        "ORDER BY est_dans_groupe DESC, s.rating DESC, s.date_heure ASC " +
                        "LIMIT 15";

        List<Session> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Session s = mapSession(rs);
                String realDesc = rs.getString("description") != null
                        ? rs.getString("description") : "";
                String nomSession = rs.getString("nom") != null
                        ? rs.getString("nom") : "";
                s.setDescription(
                        "[LANGUE:"      + rs.getString("langue_nom")  +
                                "|GROUPE:"      + rs.getString("groupe_nom")  +
                                "|NIVEAU:"      + rs.getString("niveau_titre") +
                                "|DANS_GROUPE:" + rs.getInt("est_dans_groupe") +
                                "|NOM:"         + nomSession + "]" +
                                realDesc
                );
                list.add(s);
            }
        }

        System.out.println("[Reco] " + list.size() + " session(s) candidate(s) pour userId=" + userId);
        return list;
    }

    private String buildSessionsContext(List<Session> sessions) {
        StringBuilder sb = new StringBuilder("SESSIONS DISPONIBLES:\n");
        for (Session s : sessions) {
            String desc     = s.getDescription() != null ? s.getDescription() : "";
            String meta     = desc.contains("]")
                    ? desc.substring(0, desc.indexOf("]") + 1) : "";
            String realDesc = desc.contains("]")
                    ? desc.substring(desc.indexOf("]") + 1).trim() : desc;
            String nomAffiche = (s.getNom() != null && !s.getNom().isBlank())
                    ? s.getNom() : "(sans nom)";
            sb.append("  ID:").append(s.getId())
                    .append(" NOM:").append(nomAffiche)
                    .append(" ").append(meta)
                    .append(" | Note:")
                    .append(s.getRating() != null ? s.getRating() + "/5" : "non notée")
                    .append(" | Prix:")
                    .append(s.getPrix() != null ? s.getPrix() + "TND" : "gratuit")
                    .append(" | Description:")
                    .append(realDesc.isBlank() ? "(aucune)" : realDesc)
                    .append("\n");
        }
        return sb.toString();
    }

    private String callGroqAPI(String userProfile, String sessionsContext,
                               int nbSessions) throws Exception {

        String prompt =
                "Tu es un assistant pédagogique pour une application d'apprentissage des langues.\n\n" +
                        "Voici le profil de l'étudiant :\n" + userProfile + "\n\n" +
                        sessionsContext + "\n\n" +
                        "INSTRUCTIONS :\n" +
                        "1. Recommande exactement " + nbSessions + " session(s) parmi celles listées.\n" +
                        "2. Priorise ABSOLUMENT les sessions où DANS_GROUPE=1.\n" +
                        "3. Si moins de " + nbSessions + " session(s) avec DANS_GROUPE=1, " +
                        "prends les sessions dans la même langue que l'étudiant.\n" +
                        "4. En dernier recours seulement, prends les mieux notées.\n" +
                        "5. Les explications doivent être en français, chaleureuses et personnalisées.\n" +
                        "6. Mentionne le NOM de la session dans l'explication.\n" +
                        "7. Ne recommande JAMAIS deux fois la même session (IDs différents).\n" +
                        "8. Utilise UNIQUEMENT les IDs qui apparaissent dans la liste SESSIONS DISPONIBLES.\n" +
                        "9. Ne génère PAS de session_id null ou inexistant.\n\n" +
                        "Réponds UNIQUEMENT avec ce JSON exact (tableau de " + nbSessions +
                        " éléments), sans texte avant ni après :\n" +
                        "{\n" +
                        "  \"recommandations\": [\n" +
                        "    {\n" +
                        "      \"session_id\": 12,\n" +
                        "      \"raison_courte\": \"Parfait pour ton niveau\",\n" +
                        "      \"explication_complete\": \"Cette session est idéale...\",\n" +
                        "      \"points_forts\": [\"Même langue\", \"Bonne note\", \"Places disponibles\"]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", GROQ_MODEL);

        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.put(userMessage);
        requestBody.put("messages", messages);

        HttpURLConnection conn = (HttpURLConnection)
                new URL(GROQ_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type",  "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + GROQ_KEY);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        java.io.InputStream stream = (status >= 200 && status < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();
        String rawResponse = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

        System.out.println("[Groq] Status HTTP : " + status);

        JSONObject jsonResponse = new JSONObject(rawResponse);
        if (!jsonResponse.has("choices")) {
            String errorMsg = jsonResponse.has("error")
                    ? jsonResponse.getJSONObject("error").optString("message", "Erreur inconnue")
                    : "Réponse inattendue de Groq";
            throw new RuntimeException("[Groq] Erreur API : " + errorMsg);
        }

        return jsonResponse
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }

    private List<Session> parseAIResponse(String aiJson, List<Session> candidates) {
        Map<Integer, Session> byId = new LinkedHashMap<>();
        for (Session s : candidates) {
            String desc = s.getDescription() != null ? s.getDescription() : "";
            if (desc.contains("]"))
                s.setDescription(desc.substring(desc.indexOf("]") + 1).trim());
            byId.put(s.getId(), s);
        }

        String cleaned = aiJson;
        int start = aiJson.indexOf("{");
        int end   = aiJson.lastIndexOf("}");
        if (start != -1 && end != -1)
            cleaned = aiJson.substring(start, end + 1);

        List<Session> result  = new ArrayList<>();
        Set<Integer>  dejaDis = new HashSet<>();

        try {
            JSONArray recos = new JSONObject(cleaned).getJSONArray("recommandations");
            for (int i = 0; i < recos.length(); i++) {
                JSONObject reco = recos.getJSONObject(i);

                if (reco.isNull("session_id")) {
                    System.err.println("[Groq] session_id null ignoré à l'index " + i);
                    continue;
                }

                int id;
                try {
                    id = reco.getInt("session_id");
                } catch (Exception ex) {
                    System.err.println("[Groq] session_id invalide ignoré à l'index " + i);
                    continue;
                }

                String    court  = reco.optString("raison_courte", "Recommandé par l'IA");
                String    longEx = reco.optString("explication_complete", "");
                JSONArray points = reco.optJSONArray("points_forts");

                StringBuilder full = new StringBuilder(court);
                if (!longEx.isBlank())
                    full.append("\n\n").append(longEx);
                if (points != null) {
                    full.append("\n\n✅ Points forts :");
                    for (int j = 0; j < points.length(); j++)
                        full.append("\n  • ").append(points.getString(j));
                }

                aiExplanations.put(id, full.toString());

                if (byId.containsKey(id) && !dejaDis.contains(id)) {
                    result.add(byId.get(id));
                    dejaDis.add(id);
                } else if (!byId.containsKey(id)) {
                    System.err.println("[Groq] session_id=" + id + " ignoré (n'existe pas)");
                }
            }
        } catch (Exception e) {
            System.err.println("[Groq] Parse JSON échoué : " + e.getMessage());
            return candidates.subList(0, Math.min(3, candidates.size()));
        }

        if (result.size() < candidates.size()) {
            for (Session s : candidates) {
                if (!dejaDis.contains(s.getId())) {
                    result.add(s);
                    dejaDis.add(s.getId());
                    if (result.size() >= Math.min(3, candidates.size())) break;
                }
            }
        }

        return result;
    }

    private List<Session> fallbackSQL(int userId) throws SQLException {
        List<Session> reco = getParGroupe(userId);
        for (Session s : reco)
            lastRaisons.put(s.getId(), new String[]{
                    "Niveau 1 — Groupe",
                    "Tu es inscrit dans le groupe associé à cette session",
                    "Session active avec places disponibles", ""
            });

        if (reco.size() < 3) {
            List<Session> langue = getParLangue(userId, reco);
            for (Session s : langue)
                lastRaisons.put(s.getId(), new String[]{
                        "Niveau 2 — Langue",
                        "Cette session est dans une langue que tu pratiques",
                        "Session active avec places disponibles", ""
                });
            reco.addAll(langue);
        }

        if (reco.size() < 3) {
            List<Session> fb = getFallbackDisponibles(userId, reco);
            for (Session s : fb)
                lastRaisons.put(s.getId(), new String[]{
                        "Niveau 3 — Fallback",
                        "Parmi les sessions les mieux notées disponibles", "", ""
                });
            reco.addAll(fb);
        }

        return reco.size() > 3 ? reco.subList(0, 3) : reco;
    }

    private List<Session> getParGroupe(int userId) throws SQLException {
        String sql =
                "SELECT s.* FROM session s " +
                        "JOIN groupe_user gu ON gu.groupe_id = s.id_group_id " +
                        "WHERE gu.user_id = ? " +
                        "AND s.statut IN ('" + STATUT_PLANIFIEE + "','" + STATUT_EN_COURS + "') " +
                        "AND s.id NOT IN (" +
                        "    SELECT id_session_id FROM reservation " +
                        "    WHERE id_user_id = ? AND statut != '" + STATUT_ANNULEE + "'" +
                        ") " +
                        "AND s.capacite_max > (" +
                        "    SELECT COUNT(*) FROM reservation r2 " +
                        "    WHERE r2.id_session_id = s.id AND r2.statut != '" + STATUT_ANNULEE + "'" +
                        ") " +
                        "ORDER BY s.rating DESC LIMIT 3";
        return executer(sql, userId, userId);
    }

    private List<Session> getParLangue(int userId, List<Session> dejaPresents)
            throws SQLException {
        String excl = construireExclusions(dejaPresents);
        String sql =
                "SELECT s.* FROM session s " +
                        "JOIN groupe g ON g.id = s.id_group_id " +
                        "WHERE g.id_langue_id IN (" +
                        "    SELECT g2.id_langue_id FROM groupe_user gu2 " +
                        "    JOIN groupe g2 ON g2.id = gu2.groupe_id WHERE gu2.user_id = ?) " +
                        "AND s.statut IN ('" + STATUT_PLANIFIEE + "','" + STATUT_EN_COURS + "') " +
                        "AND s.id NOT IN (" +
                        "    SELECT id_session_id FROM reservation " +
                        "    WHERE id_user_id = ? AND statut != '" + STATUT_ANNULEE + "'" +
                        ") " +
                        "AND s.id NOT IN (" + excl + ") " +
                        "AND s.capacite_max > (" +
                        "    SELECT COUNT(*) FROM reservation r2 " +
                        "    WHERE r2.id_session_id = s.id AND r2.statut != '" + STATUT_ANNULEE + "'" +
                        ") " +
                        "ORDER BY s.rating DESC LIMIT " + (3 - dejaPresents.size());
        return executer(sql, userId, userId);
    }

    private List<Session> getFallbackDisponibles(int userId, List<Session> dejaPresents)
            throws SQLException {
        String excl = construireExclusions(dejaPresents);
        String sql =
                "SELECT s.* FROM session s " +
                        "WHERE s.statut IN ('" + STATUT_PLANIFIEE + "','" + STATUT_EN_COURS + "') " +
                        "AND s.id NOT IN (" +
                        "    SELECT id_session_id FROM reservation " +
                        "    WHERE id_user_id = ? AND statut != '" + STATUT_ANNULEE + "'" +
                        ") " +
                        "AND s.id NOT IN (" + excl + ") " +
                        "AND s.capacite_max > (" +
                        "    SELECT COUNT(*) FROM reservation r2 " +
                        "    WHERE r2.id_session_id = s.id AND r2.statut != '" + STATUT_ANNULEE + "'" +
                        ") " +
                        "ORDER BY s.rating DESC LIMIT " + (3 - dejaPresents.size());
        return executer(sql, userId);
    }

    private String construireExclusions(List<Session> sessions) {
        if (sessions.isEmpty()) return "0";
        return sessions.stream()
                .map(s -> String.valueOf(s.getId()))
                .reduce((a, b) -> a + "," + b).orElse("0");
    }

    private List<Session> executer(String sql, int... params) throws SQLException {
        List<Session> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setInt(i + 1, params[i]);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapSession(rs));
        }
        return list;
    }

    private Session mapSession(ResultSet rs) throws SQLException {
        Session s = new Session();
        s.setId(rs.getInt("id"));
        Timestamp ts = rs.getTimestamp("date_heure");
        if (ts != null) s.setDateHeure(ts.toLocalDateTime());
        s.setStatut(rs.getString("statut"));
        s.setLienReunion(rs.getString("lien_reunion"));
        s.setIdGroupId(rs.getInt("id_group_id"));
        s.setIdUserId(rs.getInt("id_user_id"));
        int rating = rs.getInt("rating"); s.setRating(rs.wasNull() ? null : rating);
        int duree  = rs.getInt("duree");  s.setDuree(rs.wasNull() ? null : duree);
        double prix = rs.getDouble("prix"); s.setPrix(rs.wasNull() ? null : prix);
        s.setDescription(rs.getString("description"));
        int cap = rs.getInt("capacite_max"); s.setCapaciteMax(rs.wasNull() ? null : cap);
        s.setNom(rs.getString("nom"));
        return s;
    }
}