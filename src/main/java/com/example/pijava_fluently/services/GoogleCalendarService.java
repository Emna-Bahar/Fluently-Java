package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Session;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GoogleCalendarService {

    private static GoogleCalendarService instance;
    private String accessToken  = null;
    private String refreshToken = null;
    private String clientId     = null;
    private String clientSecret = null;
    private boolean disponible  = false;

    // ── Singleton ──────────────────────────────────────────────────
    public static GoogleCalendarService getInstance() {
        if (instance == null) instance = new GoogleCalendarService();
        return instance;
    }

    private GoogleCalendarService() {
        try {
            Properties props = chargerConfig();
            clientId     = props.getProperty("google.client.id", "").trim();
            clientSecret = props.getProperty("google.client.secret", "").trim();

            // Si un access token est déjà stocké, on l'utilise directement
            String token = props.getProperty("google.access.token", "").trim();
            if (!token.isBlank()) {
                this.accessToken = token;
                this.disponible  = true;
                System.out.println("✅ Google Calendar prêt (token config.properties)");
            } else if (!clientId.isBlank() && !clientSecret.isBlank()) {
                System.out.println("ℹ Google Calendar : client_id et client_secret chargés.");
                System.out.println("  → Ajoutez google.access.token dans config.properties");
                System.out.println("  → Obtenez-le sur : https://developers.google.com/oauthplayground");
            }
        } catch (Exception e) {
            System.err.println("⚠ Google Calendar non disponible : " + e.getMessage());
        }
    }

    // ── Charger config.properties ──────────────────────────────────
    private Properties chargerConfig() throws IOException {
        Properties props = new Properties();
        InputStream in = getClass().getResourceAsStream(
                "/config.properties");
        if (in != null) {
            props.load(in);
            in.close();
        }
        return props;
    }

    // ── Ajouter une session au Google Calendar ─────────────────────
    public String ajouterSessionCalendrier(Session s, String emailProf,
                                           String emailEtudiant) {
        if (!disponible || accessToken == null) {
            System.out.println("⚠ Google Calendar non disponible — token manquant");
            return null;
        }
        try {
            String json    = construireJsonEvent(s, emailProf, emailEtudiant);
            String urlStr  = "https://www.googleapis.com/calendar/v3/calendars/primary/events"
                    + "?sendUpdates=all";
            String reponse = envoyerRequete(urlStr, "POST", json);
            if (reponse != null) {
                String id = extraireChamp(reponse, "\"id\"");
                System.out.println("✅ Session ajoutée au calendrier : " + id);
                return id;
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur ajout calendrier : " + e.getMessage());
        }
        return null;
    }

    // ── Supprimer un événement ─────────────────────────────────────
    public void supprimerSessionCalendrier(String googleEventId) {
        if (!disponible || googleEventId == null) return;
        try {
            String urlStr = "https://www.googleapis.com/calendar/v3/calendars/primary/events/"
                    + googleEventId;
            envoyerRequete(urlStr, "DELETE", null);
            System.out.println("✅ Session supprimée du calendrier");
        } catch (Exception e) {
            System.err.println("❌ Erreur suppression calendrier : " + e.getMessage());
        }
    }

    // ── Construction JSON de l'événement ──────────────────────────
    private String construireJsonEvent(Session s, String emailProf,
                                       String emailEtudiant) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        String debut = s.getDateHeure().format(fmt);
        String fin   = s.getDateHeure()
                .plusMinutes(s.getDuree() != null ? s.getDuree() : 60)
                .format(fmt);

        String nom  = (s.getNom() != null && !s.getNom().isBlank())
                ? s.getNom() : "Session Fluently #" + s.getId();
        String desc = (s.getDescription() != null
                ? s.getDescription().replace("\"", "'").replace("\n", "\\n") : "");
        String lieu = (s.getLienReunion() != null ? s.getLienReunion() : "");

        // Invités
        StringBuilder attendees = new StringBuilder("\"attendees\": [");
        if (emailProf != null && !emailProf.isBlank())
            attendees.append("{\"email\":\"").append(emailProf)
                    .append("\",\"displayName\":\"Professeur\"},");
        if (emailEtudiant != null && !emailEtudiant.isBlank())
            attendees.append("{\"email\":\"").append(emailEtudiant)
                    .append("\",\"displayName\":\"Étudiant\"},");
        if (attendees.charAt(attendees.length() - 1) == ',')
            attendees.deleteCharAt(attendees.length() - 1);
        attendees.append("]");

        // Rappels : 24h email + 24h popup + 1h email + 15min popup
        String reminders = """
            "reminders": {
                "useDefault": false,
                "overrides": [
                    {"method": "email",  "minutes": 1440},
                    {"method": "popup",  "minutes": 1440},
                    {"method": "email",  "minutes": 60},
                    {"method": "popup",  "minutes": 15}
                ]
            }
            """;

        return "{"
                + "\"summary\": \"🎓 " + nom + "\","
                + "\"description\": \"" + desc + "\","
                + "\"location\": \"" + lieu + "\","
                + "\"start\": {\"dateTime\": \"" + debut + "\", \"timeZone\": \"Africa/Tunis\"},"
                + "\"end\":   {\"dateTime\": \"" + fin   + "\", \"timeZone\": \"Africa/Tunis\"},"
                + attendees + ","
                + reminders
                + "}";
    }

    // ── Envoi requête HTTP ─────────────────────────────────────────
    private String envoyerRequete(String urlStr, String method,
                                  String body) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method.equals("DELETE") ? "DELETE" : method);
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Content-Type",  "application/json");
        conn.setRequestProperty("Accept",        "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        if (body != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        int code = conn.getResponseCode();

        // Token expiré → tenter refresh
        if (code == 401) {
            System.out.println("🔄 Token expiré, tentative de refresh...");
            if (rafraichirToken()) {
                conn.disconnect();
                return envoyerRequete(urlStr, method, body);
            }
            System.err.println("❌ Impossible de rafraîchir le token.");
            disponible = false;
            return null;
        }

        if (code == 204 || code == 200 || code == 201) {
            if (code == 204) return "";
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } else {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                System.err.println("❌ Erreur " + code + " : " + sb);
            }
            return null;
        }
    }

    // ── Rafraîchir le token OAuth2 ─────────────────────────────────
    private boolean rafraichirToken() {
        try {
            Properties props = chargerConfig();
            String rToken = props.getProperty("google.refresh.token", "").trim();
            if (rToken.isBlank() || clientId.isBlank() || clientSecret.isBlank()) {
                return false;
            }

            String params = "client_id="     + URLEncoder.encode(clientId,     StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                    + "&refresh_token=" + URLEncoder.encode(rToken,       StandardCharsets.UTF_8)
                    + "&grant_type=refresh_token";

            URL url = new URL("https://oauth2.googleapis.com/token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(params.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() == 200) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    String newToken = extraireChamp(sb.toString(), "\"access_token\"");
                    if (!newToken.isBlank()) {
                        this.accessToken = newToken;

                        // ✅ SAUVEGARDE AUTOMATIQUE dans config.properties
                        sauvegarderToken(newToken);

                        System.out.println("✅ Token rafraîchi et sauvegardé");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur refresh token : " + e.getMessage());
        }
        return false;
    }

    // ── Sauvegarde le nouveau access_token dans config.properties ──
    private void sauvegarderToken(String newAccessToken) {
        try {
            // Trouver le fichier config.properties sur le disque
            URL resourceUrl = getClass().getResource(
                    "/config.properties");
            if (resourceUrl == null) return;

            java.io.File file = new java.io.File(resourceUrl.toURI());

            // Lire le contenu actuel
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            }

            // Mettre à jour le token
            props.setProperty("google.access.token", newAccessToken);

            // Sauvegarder
            try (FileOutputStream fos = new FileOutputStream(file)) {
                props.store(fos, "Google Calendar OAuth2 — mis à jour automatiquement");
            }

            System.out.println("✅ Nouveau access_token sauvegardé dans config.properties");

        } catch (Exception e) {
            System.err.println("⚠ Impossible de sauvegarder le token : " + e.getMessage());
            // Pas grave — le token est quand même en mémoire
        }
    }

    // ── Helper extraction JSON simple ──────────────────────────────
    private String extraireChamp(String json, String cle) {
        int idx = json.indexOf(cle);
        if (idx < 0) return "";
        int debut = json.indexOf("\"", idx + cle.length() + 2) + 1;
        int fin   = json.indexOf("\"", debut);
        if (debut <= 0 || fin <= 0) return "";
        return json.substring(debut, fin);
    }

    public boolean isDisponible() { return disponible; }
    public String  getClientId()  { return clientId;   }
}