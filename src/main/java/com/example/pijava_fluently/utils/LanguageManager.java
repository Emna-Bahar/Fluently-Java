package com.example.pijava_fluently.utils;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;

import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    public enum Lang { FR, EN }

    private static LanguageManager instance;
    private final ObjectProperty<Lang> language = new SimpleObjectProperty<>(Lang.FR);

    private static final Map<String, String[]> T = new HashMap<>();
    static {
        put("nav.home",       "Accueil",      "Home");
        put("nav.languages",  "Langues",      "Languages");
        put("nav.tests",      "Tests",        "Tests");
        put("nav.groups",     "Groupes",      "Groups");
        put("nav.sessions",   "Sessions",     "Sessions");
        put("nav.objectives", "Objectifs",    "Objectives");
        put("nav.online",     "(online)",     "(online)");

        put("login.tab",          "Connexion",                        "Sign In");
        put("register.tab",       "Créer un compte",                  "Create Account");
        put("login.welcome",      "Bon retour ! 👋",                  "Welcome back! 👋");
        put("login.subtitle",     "Connecte-toi pour continuer ton apprentissage", "Sign in to continue your learning");
        put("login.email",        "Adresse email",                    "Email address");
        put("login.password",     "Mot de passe",                     "Password");
        put("login.forgot",       "Mot de passe oublié ?",            "Forgot password?");
        put("login.btn",          "Se connecter →",                   "Sign in →");
        put("login.face",         "🔐 Connexion par Visage",          "🔐 Face Login");
        put("login.no_account",   "Pas encore de compte ?",           "Don't have an account?");
        put("login.create",       "Créer un compte",                  "Create account");
        put("login.google",       "SIGN IN WITH GOOGLE",              "SIGN IN WITH GOOGLE");

        put("reg.title",          "Rejoins Fluently 🚀",              "Join Fluently 🚀");
        put("reg.subtitle",       "Commence ton voyage linguistique dès aujourd'hui", "Start your language journey today");
        put("reg.firstname",      "Prénom",                           "First name");
        put("reg.lastname",       "Nom d'utilisateur",                "Username");
        put("reg.role",           "Rôle",                             "Role");
        put("reg.student",        "🎓  Étudiant",                     "🎓  Student");
        put("reg.teacher",        "📚  Professeur",                   "📚  Teacher");
        put("reg.password",       "Mot de passe",                     "Password");
        put("reg.confirm",        "Confirmer le mot de passe",        "Confirm password");
        put("reg.face",           "Visage (Face ID)",                 "Face (Face ID)");
        put("reg.capture",        "📷 Capturer mon visage",           "📷 Capture my face");
        put("reg.btn",            "Créer mon compte →",               "Create my account →");
        put("reg.have_account",   "Déjà inscrit ?",                   "Already have an account?");
        put("reg.signin",         "Se connecter",                     "Sign in");
        put("reg.signup_google",  "SIGN UP WITH GOOGLE",              "SIGN UP WITH GOOGLE");

        put("brand.tagline1",     "Learn Languages.",                 "Learn Languages.");
        put("brand.tagline2",     "Master the World.",                "Master the World.");
        put("brand.stat.students","Étudiants",                        "Students");
        put("brand.stat.langs",   "Langues",                          "Languages");
        put("brand.stat.satis",   "Satisfaction",                     "Satisfaction");

        put("lang.btn",           "🌐 EN",                            "🌐 FR");
    }

    private static void put(String key, String fr, String en) {
        T.put(key, new String[]{fr, en});
    }

    private LanguageManager() {}

    public static LanguageManager getInstance() {
        if (instance == null) instance = new LanguageManager();
        return instance;
    }

    public String t(String key) {
        String[] vals = T.get(key);
        if (vals == null) return key;
        return language.get() == Lang.FR ? vals[0] : vals[1];
    }

    // Old toggle (kept for compatibility)
    public void toggle() {
        language.set(language.get() == Lang.FR ? Lang.EN : Lang.FR);
    }

    // New toggle — translates entire scene
    public void toggle(Scene scene) {
        Lang newLang = language.get() == Lang.FR ? Lang.EN : Lang.FR;
        language.set(newLang);
        String langCode = newLang == Lang.EN ? "en" : "fr";
        if (scene != null) {
            new Thread(() ->
                    TranslationManager.translateScene(
                            (javafx.scene.Parent) scene.getRoot(), langCode)
            ).start();
        }
    }

    public Lang getLang() { return language.get(); }
    public ObjectProperty<Lang> languageProperty() { return language; }
}