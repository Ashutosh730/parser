package com.logAnalyzer.ai.service;

import com.logAnalyzer.ai.entity.AiPromptConfig;
import com.logAnalyzer.ai.enums.AiPromptKey;
import com.logAnalyzer.ai.repository.AiPromptConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiPromptConfigService {

    private final AiPromptConfigRepository repository;
    private final Map<AiPromptKey, String> prompts = new EnumMap<>(AiPromptKey.class);

    @PostConstruct
    public void refresh() {
        List<AiPromptConfig> configs = repository.findAll();
        Map<AiPromptKey, String> refreshed = new EnumMap<>(AiPromptKey.class);
        for (AiPromptConfig config : configs) {
            if (config.getPromptKey() == null || config.getSystemPrompt() == null || config.getSystemPrompt().isBlank()) {
                throw new IllegalStateException("AI prompt configuration contains a blank prompt or key");
            }
            if (refreshed.put(config.getPromptKey(), config.getSystemPrompt()) != null) {
                throw new IllegalStateException(
                        "Duplicate AI prompt configuration for key: " + config.getPromptKey());
            }
        }
        synchronized (prompts) {
            prompts.clear();
            prompts.putAll(refreshed);
        }
    }

    public String getPrompt(AiPromptKey promptKey) {
        synchronized (prompts) {
            String prompt = prompts.get(promptKey);
            if (prompt == null) {
                throw new IllegalStateException("No AI prompt configured for key: " + promptKey);
            }
            return prompt;
        }
    }
}
