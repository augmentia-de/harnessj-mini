package de.augmentia.harness.tui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import de.augmentia.harness.core.MinimalHarness;
import de.augmentia.harness.config.HarnessProperties;
import de.augmentia.harness.model.OpenAiModel;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lanterna-based TUI for the harness.
 *
 * <h2>Layout</h2>
 * <ul>
 *   <li>logBox (read-only TextBox) — chat history + results.</li>
 *   <li>inputBox (TextBox) — task input.</li>
 *   <li>Run button — triggers harness.chat() in a background thread.</li>
 * </ul>
 *
 * <h2>Flow</h2>
 * 1. User types task in inputBox.
 * 2. Run button calls getHarness().chat(task) in a background thread.
 * 3. Result is appended to logBox via GUI thread (invokeLater).
 * 4. Conversation history is preserved across turns (persistent harness instance).
 */
public class HarnessTui {
    private final HarnessProperties props;
    private MinimalHarness harness;

    public HarnessTui(HarnessProperties props) { this.props = props; }

    private MinimalHarness getHarness() {
        if (harness == null) {
            OpenAiModel model = new OpenAiModel(props.baseUrl(), props.apiKey(), props.model(), props.sessionId());
            harness = new MinimalHarness(model, props.maxTurns(), props.maxCost(), props.compactAtTokens(), null, 0, props.maxRetries(), props.retryBackoffMs());
        }
        return harness;
    }

    public void run(String task) throws IOException {
        Terminal terminal = new DefaultTerminalFactory().createTerminal();
        Screen screen = new TerminalScreen(terminal);
        screen.startScreen();

        BasicWindow window = new BasicWindow("Harness TUI");
        Panel mainPanel = new Panel(new LinearLayout(Direction.VERTICAL));

        TextBox logBox = new TextBox(new TerminalSize(80, 20));
        logBox.setReadOnly(true);
        mainPanel.addComponent(logBox);

        mainPanel.addComponent(new Label("Task:"));
        TextBox inputBox = new TextBox(new TerminalSize(80, 3));
        mainPanel.addComponent(inputBox);

        AtomicReference<MultiWindowTextGUI> guiRef = new AtomicReference<>();
        Button runBtn = new Button("Run", () -> {
            String t = inputBox.getText();
            if (t.isBlank()) return;
            logBox.setText(logBox.getText() + "\n> " + t + "\n");
            MultiWindowTextGUI gui = guiRef.get();
            new Thread(() -> {
                try {
                    String result = getHarness().chat(t);
                    if (gui != null) gui.getGUIThread().invokeLater(() -> logBox.setText(logBox.getText() + "\n=== RESULT ===\n" + result));
                } catch (Exception e) {
                    if (gui != null) gui.getGUIThread().invokeLater(() -> logBox.setText(logBox.getText() + "\nERROR: " + e.getMessage()));
                }
            }).start();
        });
        mainPanel.addComponent(runBtn);

        window.setComponent(mainPanel);
        window.setFocusedInteractable(inputBox);
        MultiWindowTextGUI gui = new MultiWindowTextGUI(screen);
        guiRef.set(gui);
        gui.addWindowAndWait(window);
        screen.stopScreen();
    }
}