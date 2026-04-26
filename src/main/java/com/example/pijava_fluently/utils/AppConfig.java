package com.example.pijava_fluently.utils;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream is = AppConfig.class.getResourceAsStream("/config.properties")) {
            if (is != null) props.load(is);
        } catch (Exception ignored) {}
    }

    public static String get(String key) {
        return props.getProperty(key, System.getenv(key));
    }
}
