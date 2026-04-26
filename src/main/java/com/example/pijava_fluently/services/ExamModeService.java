package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.entites.TestPassage;
import com.example.pijava_fluently.utils.ConfigLoader;
import com.example.pijava_fluently.utils.LoggerUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ExamModeService {

    private final Map<Integer, List<ExamEvent>> examEvents = new ConcurrentHashMap<>();

    private static final int MAX_TAB_SWITCHES = ConfigLoader.getInt("exam.max_tab_switches", 3);
    private static final int MAX_COPY_PASTES = ConfigLoader.getInt("exam.max_copy_pastes", 1);
    private static final int MAX_BLURS = ConfigLoader.getInt("exam.max_blurs", 5);
    private static final int SUSPICION_THRESHOLD_HIGH = ConfigLoader.getInt("exam.suspicion_threshold_high", 70);
    private static final int SUSPICION_THRESHOLD_MEDIUM = ConfigLoader.getInt("exam.suspicion_threshold_medium", 40);

    public enum EventType {
        TAB_SWITCH("tab_switch"),
        COPY_PASTE("copy_paste"),
        PAGE_BLUR("page_blur"),
        DEVTOOLS_ATTEMPT("devtools_attempt");

        private final String value;

        EventType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class ExamEvent {
        private final EventType type;
        private final long timestamp;
        private final Map<String, Object> details;

        public ExamEvent(EventType type, Map<String, Object> details) {
            this.type = type;
            this.timestamp = System.currentTimeMillis();
            this.details = details;
        }

        public EventType getType() { return type; }
        public long getTimestamp() { return timestamp; }
        public Map<String, Object> getDetails() { return details; }
    }

    public boolean isExamMode(Test test) {
        if (test == null) return false;
        String type = test.getType();
        return "Test de niveau".equals(type) || "Test de fin de niveau".equals(type);
    }

    public void logEvent(TestPassage passage, EventType eventType, Map<String, Object> details) {
        // Correction: vérifier si passage et son ID sont null
        if (passage == null) {
            LoggerUtil.warning("Cannot log event: passage is null");
            return;
        }

        Integer passageIdObj = passage.getId();
        if (passageIdObj == null) {
            LoggerUtil.warning("Cannot log event: passage has no ID");
            return;
        }

        int passageId = passageIdObj;  // Unboxing safe car on a vérifié null

        examEvents.computeIfAbsent(passageId, k -> new ArrayList<>())
                .add(new ExamEvent(eventType, details));

        LoggerUtil.warning("EXAM EVENT",
                "passage_id", passageId,
                "user_id", passage.getUserId(),
                "test_id", passage.getTestId(),
                "event_type", eventType.getValue(),
                "details", details);
    }

    public List<ExamEvent> getEvents(int passageId) {
        return examEvents.getOrDefault(passageId, new ArrayList<>());
    }

    public Map<String, Object> analyzeSuspiciousActivity(TestPassage passage) {
        if (passage == null) {
            return getEmptyAnalysis();
        }

        Integer passageIdObj = passage.getId();
        if (passageIdObj == null) {
            return getEmptyAnalysis();
        }

        int passageId = passageIdObj;
        List<ExamEvent> events = getEvents(passageId);

        int suspicionScore = 0;
        List<String> flags = new ArrayList<>();
        Map<String, Integer> counters = new HashMap<>();
        counters.put("tab_switches", 0);
        counters.put("copy_pastes", 0);
        counters.put("page_blurs", 0);

        for (ExamEvent event : events) {
            switch (event.getType()) {
                case TAB_SWITCH:
                    int tabSwitches = counters.get("tab_switches") + 1;
                    counters.put("tab_switches", tabSwitches);
                    if (tabSwitches > MAX_TAB_SWITCHES) {
                        suspicionScore += 20;
                        flags.add("Changements d'onglet fréquents (" + tabSwitches + " fois)");
                    }
                    break;

                case COPY_PASTE:
                    int copyPastes = counters.get("copy_pastes") + 1;
                    counters.put("copy_pastes", copyPastes);
                    suspicionScore += 30;
                    flags.add("Copier-coller détecté (" + copyPastes + " tentatives)");
                    break;

                case PAGE_BLUR:
                    int pageBlurs = counters.get("page_blurs") + 1;
                    counters.put("page_blurs", pageBlurs);
                    if (pageBlurs > MAX_BLURS) {
                        suspicionScore += 15;
                        flags.add("Perte de focus répétée (" + pageBlurs + " fois)");
                    }
                    break;

                case DEVTOOLS_ATTEMPT:
                    suspicionScore += 40;
                    flags.add("Tentative d'ouverture des outils développeur");
                    break;
            }
        }

        // Vérifier si tempsPasse est valide (différent de 0 ou null)
        // Note: tempsPasse est un int primitif, ne peut pas être null
        int tempsPasse = passage.getTempsPasse();
        Integer testId = passage.getTestId();

        if (tempsPasse > 0 && testId != null) {
            TestService testService = new TestService();
            try {
                Test test = testService.recupererParId(testId);
                if (test != null && test.getDureeEstimee() > 0) {
                    int tempsAttendu = test.getDureeEstimee() * 60;
                    if (tempsPasse < tempsAttendu * 0.3) {
                        suspicionScore += 25;
                        flags.add("Test terminé trop rapidement (" + (tempsPasse / 60) + " min)");
                    }
                }
            } catch (Exception e) {
                LoggerUtil.error("Error analyzing test duration", e);
            }
        }

        suspicionScore = Math.min(suspicionScore, 100);

        Map<String, Object> result = new HashMap<>();
        result.put("suspicion_score", suspicionScore);
        result.put("flags", flags);
        result.put("events", counters);
        result.put("total_events", events.size());
        result.put("recommendation", getRecommendation(suspicionScore));
        result.put("color", getColor(suspicionScore));

        return result;
    }

    public void clearEvents(int passageId) {
        examEvents.remove(passageId);
        LoggerUtil.info("Cleared exam events for passage", "passage_id", passageId);
    }

    private String getRecommendation(int score) {
        if (score >= SUSPICION_THRESHOLD_HIGH) {
            return "INVALIDER - Comportement très suspect";
        } else if (score >= SUSPICION_THRESHOLD_MEDIUM) {
            return "SURVEILLER - Activité suspecte détectée";
        } else {
            return "VALIDER - Comportement normal";
        }
    }

    private String getColor(int score) {
        if (score >= SUSPICION_THRESHOLD_HIGH) {
            return "danger";
        } else if (score >= SUSPICION_THRESHOLD_MEDIUM) {
            return "warning";
        } else {
            return "success";
        }
    }

    private Map<String, Object> getEmptyAnalysis() {
        Map<String, Object> result = new HashMap<>();
        result.put("suspicion_score", 0);
        result.put("flags", new ArrayList<>());
        result.put("events", new HashMap<String, Integer>());
        result.put("total_events", 0);
        result.put("recommendation", "VALIDER - Comportement normal");
        result.put("color", "success");
        return result;
    }
}