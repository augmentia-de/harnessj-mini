package de.augmentia.harness2.tools;

import de.augmentia.harness2.Lc4jHarness;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class HarnessService {
    @ConfigProperty(name = "llm.base-url", defaultValue = "https://..providere../v1")
    String baseUrl;
    @ConfigProperty(name = "llm.api-key", defaultValue = "")
    String apiKey;
    @ConfigProperty(name = "llm.model", defaultValue = "...model-mini")
    String model;

    private Lc4jHarness harness;

    public synchronized Lc4jHarness getHarness() {
        if (harness == null) {
            ChatModel chatModel = OpenAiChatModel.builder()
                    .baseUrl(baseUrl)
                    .apiKey(apiKey.isBlank() ? "no-key" : apiKey)
                    .modelName(model)
                    .customHeaders(Map.of("x-opencode-session", UUID.randomUUID().toString()))
                    .build();
            harness = new Lc4jHarness(chatModel);
        }
        return harness;
    }
}