package com.logAnalyzer.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApplicationConfigTest {

    @Test
    void shouldExposeSpringAiOpenAiBaseUrlAndApiKeySettings() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.yaml")) {
            assertNotNull(inputStream, "application.yaml should be present");

            Map<String, Object> root = new Yaml().load(inputStream);
            Map<String, Object> spring = (Map<String, Object>) root.get("spring");
            Map<String, Object> ai = (Map<String, Object>) spring.get("ai");
            Map<String, Object> openai = (Map<String, Object>) ai.get("openai");

            assertEquals("${SPRING_AI_OPENAI_BASE_URL:${OPENAI_BASE_URL:https://integrate.api.nvidia.com}}", openai.get("base-url"));
            assertEquals("${SPRING_AI_OPENAI_API_KEY:${OPENAI_API_KEY:}}", openai.get("api-key"));
        } catch (Exception e) {
            throw new AssertionError("Failed to validate Spring AI OpenAI settings", e);
        }
    }
}
