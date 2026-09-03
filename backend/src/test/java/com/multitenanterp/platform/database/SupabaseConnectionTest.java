package com.multitenanterp.platform.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class SupabaseConnectionTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_SUPABASE_CONNECTION_TEST", matches = "true")
    void connectsToConfiguredDatabase() throws Exception {
        String url = requiredEnvironmentVariable("DB_URL");
        String username = requiredEnvironmentVariable("DB_USERNAME");
        String password = requiredEnvironmentVariable("DB_PASSWORD");

        try (var connection = DriverManager.getConnection(url, username, password);
             var statement = connection.prepareStatement("SELECT 1");
             var result = statement.executeQuery()) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(1);
        }
    }

    private String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
