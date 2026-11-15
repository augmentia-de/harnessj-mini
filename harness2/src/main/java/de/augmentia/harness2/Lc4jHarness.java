package de.augmentia.harness2;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Lc4jHarness {
    private final ChatModel model;
    private final List<ChatMessage> messages = new ArrayList<>();
    private int nTurns = 0;
    private final List<ToolSpecification> toolSpecs;

    public Lc4jHarness(ChatModel model) {
        this.model = model;
        this.toolSpecs = buildToolSpecs();
    }

    private List<ToolSpecification> buildToolSpecs() {
        List<ToolSpecification> specs = new ArrayList<>();
        // bash
        specs.add(ToolSpecification.builder().name("bash").description("Run a bash command")
                .parameters(JsonObjectSchema.builder().addStringProperty("cmd", "The bash command").required("cmd").build()).build());
        // read_file
        specs.add(ToolSpecification.builder().name("read_file").description("Read a file with line numbers")
                .parameters(JsonObjectSchema.builder().addStringProperty("path", "File path")
                        .addIntegerProperty("offset", "Start line offset").addIntegerProperty("limit", "Max lines").required("path").build()).build());
        // write_file
        specs.add(ToolSpecification.builder().name("write_file").description("Write content to a file")
                .parameters(JsonObjectSchema.builder().addStringProperty("path", "File path")
                        .addStringProperty("content", "Content to write").required("path", "content").build()).build());
        // search_replace
        specs.add(ToolSpecification.builder().name("search_replace").description("Replace unique search string in file")
                .parameters(JsonObjectSchema.builder().addStringProperty("path", "File path")
                        .addStringProperty("search", "Text to search").addStringProperty("replace", "Replacement text")
                        .addBooleanProperty("fuzzy", "Use fuzzy matching").required("path", "search", "replace").build()).build());
        return specs;
    }

    public String chat(String userMessage) {
        return chat(userMessage, Path.of("."));
    }

    public String chat(String userMessage, Path cwd) {
        if (messages.isEmpty()) {
            messages.add(SystemMessage.from("You are a SWE agent.\n\n" + ContextDiscovery.discover(cwd)));
        }
        messages.add(UserMessage.from(userMessage));

        while (true) {
            ChatRequest request = ChatRequest.builder().messages(messages).toolSpecifications(toolSpecs).build();
            ChatResponse response = model.chat(request);
            nTurns++;
            AiMessage aiMessage = response.aiMessage();
            messages.add(aiMessage);

            if (!aiMessage.hasToolExecutionRequests()) {
                return aiMessage.text();
            }

            for (ToolExecutionRequest req : aiMessage.toolExecutionRequests()) {
                String out;
                try {
                    Map<String, Object> args = new com.fasterxml.jackson.databind.ObjectMapper().readValue(req.arguments(), Map.class);
                    out = Tools.dispatch(req.name(), args);
                } catch (Exception e) {
                    out = "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
                }
                messages.add(ToolExecutionResultMessage.from(req, Tools.truncate(out)));
            }
        }
    }

    public void reset() {
        messages.clear();
        nTurns = 0;
    }
}