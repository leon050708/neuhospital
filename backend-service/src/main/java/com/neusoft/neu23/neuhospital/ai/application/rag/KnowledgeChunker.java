package com.neusoft.neu23.neuhospital.ai.application.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KnowledgeChunker {

    private final int maxChunkChars;
    private final int overlapChars;
    private final int minChunkChars;

    public KnowledgeChunker() {
        this(900, 120, 240);
    }

    public KnowledgeChunker(int maxChunkChars, int overlapChars, int minChunkChars) {
        this.maxChunkChars = maxChunkChars;
        this.overlapChars = overlapChars;
        this.minChunkChars = minChunkChars;
    }

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> paragraphs = splitParagraphs(text);
        List<String> mergedParagraphs = mergeShortParagraphs(paragraphs);
        return buildChunks(mergedParagraphs);
    }

    private List<String> splitParagraphs(String text) {
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        String[] rawParagraphs = normalized.split("\\n\\s*\\n");
        List<String> paragraphs = new ArrayList<>();
        for (String rawParagraph : rawParagraphs) {
            String compact = rawParagraph.trim();
            if (!compact.isBlank()) {
                if (!compact.contains("\n")) {
                    paragraphs.add(compact);
                    continue;
                }
                for (String line : compact.split("\n")) {
                    String trimmed = line.trim();
                    if (!trimmed.isBlank()) {
                        paragraphs.add(trimmed);
                    }
                }
            }
        }
        return paragraphs;
    }

    private List<String> mergeShortParagraphs(List<String> paragraphs) {
        List<String> merged = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (current.isEmpty()) {
                current.append(paragraph);
                continue;
            }

            int candidateLength = current.length() + 1 + paragraph.length();
            boolean shouldMerge = current.length() < minChunkChars || paragraph.length() < minChunkChars / 2;
            if (shouldMerge && candidateLength <= maxChunkChars) {
                current.append('\n').append(paragraph);
                continue;
            }

            merged.add(current.toString());
            current = new StringBuilder(paragraph);
        }

        if (!current.isEmpty()) {
            merged.add(current.toString());
        }
        return merged;
    }

    private List<String> buildChunks(List<String> mergedParagraphs) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : mergedParagraphs) {
            if (paragraph.length() > maxChunkChars) {
                flushCurrentChunk(chunks, current);
                splitOversizedParagraph(chunks, paragraph);
                continue;
            }

            if (current.isEmpty()) {
                current.append(paragraph);
                continue;
            }

            if (current.length() + 1 + paragraph.length() <= maxChunkChars) {
                current.append('\n').append(paragraph);
                continue;
            }

            chunks.add(current.toString().trim());
            current = new StringBuilder(createOverlap(chunks.get(chunks.size() - 1))).append(paragraph);
        }

        flushCurrentChunk(chunks, current);
        return chunks;
    }

    private void splitOversizedParagraph(List<String> chunks, String paragraph) {
        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(start + maxChunkChars, paragraph.length());
            if (end < paragraph.length()) {
                end = findBetterBreak(paragraph, start, end);
            }
            String chunk = paragraph.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            if (end >= paragraph.length()) {
                break;
            }
            start = Math.max(end - overlapChars, start + 1);
        }
    }

    private int findBetterBreak(String paragraph, int start, int end) {
        int searchStart = Math.min(end - 1, paragraph.length() - 1);
        int lowerBound = Math.max(start + minChunkChars, start + 1);
        for (int i = searchStart; i >= lowerBound; i--) {
            char current = paragraph.charAt(i);
            if (current == '。' || current == '；' || current == '！' || current == '？') {
                return i + 1;
            }
        }
        return end;
    }

    private void flushCurrentChunk(List<String> chunks, StringBuilder current) {
        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }
    }

    private String createOverlap(String chunk) {
        int start = Math.max(0, chunk.length() - overlapChars);
        return chunk.substring(start).trim() + "\n";
    }
}
