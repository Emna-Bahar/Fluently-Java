package com.example.pijava_fluently.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads API keys and secrets from config.properties (never committed to Git).
 * Usage: ConfigLoader.get("anthropic.api.key")
 */
public class ConfigLoader {

    private static final Properties props = new Properties();

    static {
        try (InputStream is = ConfigLoader.class.getResourceAsStream(
                "/com/example/pijava_fluently/config.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                System.err.println("⚠ config.properties not found! Copy config.properties.example and fill in your keys.");
            }
        } catch (IOException e) {
            System.err.println("⚠ Could not load config.properties: " + e.getMessage());
        }
    }

    public static String get(String key) {
        return props.getProperty(key, "");
    }
}