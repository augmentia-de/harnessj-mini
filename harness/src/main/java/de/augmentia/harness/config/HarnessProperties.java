package de.augmentia.harness.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "harness")
public record HarnessProperties(
        String baseUrl,
        String apiKey,
        String model,
        String sessionId,
        int maxTurns,
        double maxCost,
        int compactAtTokens,
        int maxRetries,
        long retryBackoffMs
) {
    public HarnessProperties {
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://api.openai.com/v1";
        if (model == null || model.isBlank()) model = "gpt-4o-mini";
        if (maxTurns <= 0) maxTurns = 50;
        if (maxCost <= 0) maxCost = 5.0;
        if (compactAtTokens <= 0) compactAtTokens = 100000;
        if (sessionId == null) sessionId = "";
        if (maxRetries <= 0) maxRetries = 3;
        if (retryBackoffMs <= 0) retryBackoffMs = 1000;
    }
}