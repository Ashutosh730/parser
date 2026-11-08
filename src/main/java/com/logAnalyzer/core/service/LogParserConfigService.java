package com.logAnalyzer.core.service;

import com.logAnalyzer.core.repository.LogParserConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LogParserConfigService {

    private final LogParserConfigRepository repository;
    private final Map<String, List<Pattern>> languagePatterns = new ConcurrentHashMap<>();
    private final Map<String, Pattern> frameworkPatterns = new ConcurrentHashMap<>();

    @PostConstruct
    @Transactional(readOnly = true)
    public void refresh() {
        languagePatterns.clear();
        frameworkPatterns.clear();
        repository.findAll().forEach(config -> {
            List<Pattern> patterns = Arrays.stream(config.getLanguageLogRegex())
                    .map(String::trim)
                    .filter(regex -> !regex.isEmpty())
                    .map(Pattern::compile)
                    .toList();
            languagePatterns.putIfAbsent(config.getLanguage(), patterns);
            if (config.getFramework() != null && config.getFrameworkLogRegex() != null) {
                frameworkPatterns.put(frameworkKey(config.getLanguage(), config.getFramework()), Pattern.compile(config.getFrameworkLogRegex().trim()));
            }
        });
    }

    @Transactional(readOnly = true)
    public List<Pattern> getLanguagePatterns(String language) {
        List<Pattern> patterns = languagePatterns.get(language);
        if (patterns == null || patterns.isEmpty()) {
            throw new IllegalStateException("No parser regex configured for language: " + language);
        }
        return patterns;
    }

    @Transactional(readOnly = true)
    public Pattern getFrameworkPattern(String language, String framework) {
        Pattern pattern = frameworkPatterns.get(frameworkKey(language, framework));
        if (pattern == null) {
            throw new IllegalStateException(
                    "No parser regex configured for framework: " + language + "/" + framework);
        }
        return pattern;
    }

    private String frameworkKey(String language, String framework) {
        return language + ":" + framework;
    }
}
