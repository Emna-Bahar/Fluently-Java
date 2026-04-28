package com.example.pijava_fluently.utils;

import javafx.scene.layout.Pane;

/**
 * ReminderScheduler — délègue tout à NotificationBell.
 * Gardé pour compatibilité avec les controllers existants.
 */
public class ReminderScheduler {

    private static ReminderScheduler instance;
    private NotificationBell notifBell;

    private ReminderScheduler() {}

    public static ReminderScheduler getInstance() {
        if (instance == null) instance = new ReminderScheduler();
        return instance;
    }

    /** Nouveau — avec NotificationBell */
    public void start(int userId, boolean isProfesseur, Pane container,
                      NotificationBell bell) {
        if (this.notifBell != null) this.notifBell.stop();
        this.notifBell = bell;
        System.out.println("✅ ReminderScheduler démarré (NotificationBell) userId=" + userId);
    }

    /** Compatibilité ancien code */
    public void start(int userId, boolean isProfesseur, Pane container) {
        System.out.println("ℹ ReminderScheduler.start() — utilisez NotificationBell directement");
    }

    public NotificationBell getNotifBell() { return notifBell; }

    public void stop() {
        if (notifBell != null) notifBell.stop();
        System.out.println("🛑 ReminderScheduler arrêté");
    }
}