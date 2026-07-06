package com.neusoft.neu23.neuhospital.ai.application.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class KnowledgeTextCleaner {

    public String clean(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        List<String> normalizedLines = normalizeLines(rawText);
        Map<String, Integer> frequency = countCandidateHeaders(normalizedLines);

        List<String> cleanedLines = new ArrayList<>();
        boolean lastBlank = false;
        for (String line : normalizedLines) {
            if (shouldDropLine(line, frequency)) {
                continue;
            }
            if (line.isBlank()) {
                if (!lastBlank && !cleanedLines.isEmpty()) {
                    cleanedLines.add("");
                }
                lastBlank = true;
                continue;
            }
            cleanedLines.add(line);
            lastBlank = false;
        }

        while (!cleanedLines.isEmpty() && cleanedLines.get(cleanedLines.size() - 1).isBlank()) {
            cleanedLines.remove(cleanedLines.size() - 1);
        }
        return String.join("\n", cleanedLines);
    }

    private List<String> normalizeLines(String rawText) {
        String normalized = rawText.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\u3000', ' ');

        String[] rawLines = normalized.split("\n");
        List<String> lines = new ArrayList<>(rawLines.length);
        for (String rawLine : rawLines) {
            String compacted = rawLine.trim().replaceAll("[ \\t]+", " ");
            lines.add(compacted);
        }
        return lines;
    }

    private Map<String, Integer> countCandidateHeaders(List<String> normalizedLines) {
        Map<String, Integer> frequency = new LinkedHashMap<>();
        for (String line : normalizedLines) {
            if (line.isBlank()) {
                continue;
            }
            if (line.length() <= 32) {
                frequency.merge(line, 1, Integer::sum);
            }
        }
        return frequency;
    }

    private boolean shouldDropLine(String line, Map<String, Integer> frequency) {
        if (line.isBlank()) {
            return false;
        }
        if (line.matches("^第\\s*\\d+\\s*页$")) {
            return true;
        }
        return line.length() <= 32 && frequency.getOrDefault(line, 0) > 1;
    }
}
