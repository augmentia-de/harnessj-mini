package de.augmentia.harness;

import java.util.*;

/**
 * Edit-Tool Contract (Recommendation 5): fuzzy cascade for weaker models.
 * Tries exact match first, then whitespace-normalized, then line-level Levenshtein.
 */
public final class FuzzyEdit {
    private FuzzyEdit() {}

    public static String replace(String text, String search, String replace) {
        if (text == null || search == null) return null;
        // 1. exact
        if (text.contains(search)) {
            int count = countOccurrences(text, search);
            if (count == 1) return text.replace(search, replace);
        }
        // 2. whitespace-normalized
        String normText = normalizeWs(text);
        String normSearch = normalizeWs(search);
        int idx = normText.indexOf(normSearch);
        if (idx >= 0) {
            // map back approximately: replace first occurrence in original by locating lines
            // Fallback: do line-based replacement
        }
        // 3. line-level fuzzy: find best matching block
        String[] tLines = text.split("\n", -1);
        String[] sLines = search.split("\n", -1);
        int bestStart = -1;
        double bestScore = 0.0;
        for (int i = 0; i + sLines.length <= tLines.length; i++) {
            double score = 0;
            for (int j = 0; j < sLines.length; j++) {
                score += similarity(tLines[i + j], sLines[j]);
            }
            score /= sLines.length;
            if (score > bestScore) { bestScore = score; bestStart = i; }
        }
        if (bestStart >= 0 && bestScore >= 0.65) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bestStart; i++) sb.append(tLines[i]).append("\n");
            sb.append(replace);
            if (!replace.endsWith("\n")) sb.append("\n");
            for (int i = bestStart + sLines.length; i < tLines.length; i++) {
                sb.append(tLines[i]);
                if (i < tLines.length - 1) sb.append("\n");
            }
            return sb.toString();
        }
        return null; // no match
    }

    private static String normalizeWs(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    private static int countOccurrences(String text, String search) {
        int count = 0, idx = 0;
        while ((idx = text.indexOf(search, idx)) >= 0) { count++; idx += search.length(); }
        return count;
    }

    private static double similarity(String a, String b) {
        if (a.equals(b)) return 1.0;
        int max = Math.max(a.length(), b.length());
        if (max == 0) return 1.0;
        return 1.0 - (double) levenshtein(a, b) / max;
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[b.length()];
    }
}