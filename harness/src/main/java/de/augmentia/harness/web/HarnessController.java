package de.augmentia.harness.web;

import de.augmentia.harness.MinimalHarness;
import de.augmentia.harness.config.HarnessProperties;
import de.augmentia.harness.model.OpenAiModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
public class HarnessController {
    @Autowired
    private HarnessProperties props;

    private MinimalHarness harness;
    private final Object harnessLock = new Object();

    private MinimalHarness getHarness() {
        synchronized (harnessLock) {
            if (harness == null) {
                OpenAiModel model = new OpenAiModel(props.baseUrl(), props.apiKey(), props.model(), props.sessionId());
                harness = new MinimalHarness(model, props.maxTurns(), props.maxCost(), props.compactAtTokens());
            }
            return harness;
        }
    }

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @PostMapping(value = "/run", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter run(@RequestBody String task) {
        SseEmitter emitter = new SseEmitter(300_000L);
        executor.submit(() -> {
            try {
                // Run in a capturing manner: we can't easily stream turns, so send final result.
                emitter.send(SseEmitter.event().name("start").data("Task: " + task));
                String result = getHarness().chat(task);
                emitter.send(SseEmitter.event().name("result").data(result));
                emitter.complete();
            } catch (Exception e) {
                try { emitter.send(SseEmitter.event().name("error").data(e.getMessage())); } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    @GetMapping("/")
    public String index() {
        return "Harness Web UI: POST /api/run with task body (text/plain or JSON).";
    }
}