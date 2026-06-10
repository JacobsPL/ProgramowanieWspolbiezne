package org.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {

    private static final String CONFIG_FILE = "application.properties";
    private static final AppConfig INSTANCE = new AppConfig();
    private final Properties properties = new Properties();

    private AppConfig() {
        try (InputStream inputStream = AppConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing configuration file: " + CONFIG_FILE);
            }
            properties.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load configuration file: " + CONFIG_FILE, e);
        }
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public String getString(String key) {
        return getRequiredProperty(key);
    }

    public int getInt(String key) {
        return Integer.parseInt(getRequiredProperty(key));
    }

    public long getLong(String key) {
        return Long.parseLong(getRequiredProperty(key));
    }

    private String getRequiredProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing configuration property: " + key);
        }
        return value;
    }
}
