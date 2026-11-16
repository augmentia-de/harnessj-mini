package de.augmentia.harness;

import de.augmentia.harness.config.HarnessProperties;
import de.augmentia.harness.model.OpenAiModel;
import de.augmentia.harness.core.MinimalHarness;
import de.augmentia.harness.PermissionGate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(HarnessProperties.class)
public class HarnessApplication {

    public static void main(String[] args) {
        SpringApplication.run(HarnessApplication.class, args);
    }

    @Bean
    CommandLineRunner run(HarnessProperties props) {
        return args -> {
            if (args.length > 0 && args[0].equals("--tui")) {
                String task = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "";
                new de.augmentia.harness.tui.HarnessTui(props).run(task);
                return;
            }
            String task = args.length > 0 ? String.join(" ", args) : "Say hello and list the current directory.";
            OpenAiModel model = new OpenAiModel(props.baseUrl(), props.apiKey(), props.model(), props.sessionId());
            MinimalHarness harness = new MinimalHarness(model, props.maxTurns(), props.maxCost(), props.compactAtTokens(), PermissionGate.allowAll(), 0, props.maxRetries(), props.retryBackoffMs());
            String result = harness.run(task);
            System.out.println("=== RESULT ===");
            System.out.println(result);
        };
    }
}