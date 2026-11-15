package de.augmentia.scaffold;

import de.augmentia.harness.FuzzyEdit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FuzzyEditTest {
    @Test
    void exactMatch() {
        String text = "hello world";
        String result = FuzzyEdit.replace(text, "world", "earth");
        assertThat(result).isEqualTo("hello earth");
    }

    @Test
    void fuzzyMatchWhitespaceDrift(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("t.txt");
        Files.writeString(f, "line one\nline two\nline three");
        String text = Files.readString(f);
        String result = FuzzyEdit.replace(text, "line  two", "LINE TWO");
        // fuzzy line-level should find best block
        assertThat(result).isNotNull();
    }

    @Test
    void noMatchReturnsNull() {
        String result = FuzzyEdit.replace("abc", "zzzzzzzz", "x");
        assertThat(result).isNull();
    }
}