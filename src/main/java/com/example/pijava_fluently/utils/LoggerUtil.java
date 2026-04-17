package com.example.pijava_fluently.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerUtil {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static void info(String message, Object... args) {
        log("INFO", message, args);
    }

    public static void warning(String message, Object... args) {
        log("WARN", message, args);
    }

    public static void error(String message, Object... args) {
        log("ERROR", message, args);
    }

    public static void error(String message, Throwable throwable) {
        System.err.println(formatMessage("ERROR", message));
        throwable.printStackTrace();
    }

    private static void log(String level, String message, Object... args) {
        String formattedMessage = formatMessage(level, message);
        if (args.length > 0) {
            StringBuilder sb = new StringBuilder(formattedMessage);
            for (int i = 0; i < args.length; i += 2) {
                if (i + 1 < args.length) {
                    sb.append(" | ").append(args[i]).append("=").append(args[i + 1]);
                }
            }
            System.out.println(sb.toString());
        } else {
            System.out.println(formattedMessage);
        }
    }

    private static String formatMessage(String level, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        return String.format("[%s] [%s] %s", timestamp, level, message);
    }
}