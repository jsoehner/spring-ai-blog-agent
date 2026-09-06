package com.example.demo.service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SentenceDeduplicator implements ContentProcessor {

    private static final Pattern WP_PARAGRAPH_PATTERN = Pattern.compile("<!--\\s*wp:paragraph\\s*-->\\s*<p>(.*?)</p>\\s*<!--\\s*/wp:paragraph\\s*-->", Pattern.DOTALL);

    @Override
    public String process(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        String[] lines = content.split("\n");
        List<String> outputLines = new ArrayList<>();
        Set<String> seenSentences = new HashSet<>();
        Set<String> seenParagraphBodies = new HashSet<>();

        for (String line : lines) {
            String trimmed = line.trim();
            Matcher matcher = WP_PARAGRAPH_PATTERN.matcher(trimmed);

            if (matcher.matches()) {
                String paragraphBody = matcher.group(1).trim();
                String normalizedBody = normalize(paragraphBody);

                // If the entire paragraph body was already seen, drop the duplicate block
                if (seenParagraphBodies.contains(normalizedBody)) {
                    continue;
                }

                // Split paragraph into sentences by sentence end punctuation
                String[] rawSentences = paragraphBody.split("(?<=[.!?])\\s+");
                List<String> uniqueSentences = new ArrayList<>();

                for (String rawSentence : rawSentences) {
                    String normSentence = normalize(rawSentence);
                    if (normSentence.length() < 15) {
                        // Short phrases/fragments are kept
                        uniqueSentences.add(rawSentence.trim());
                    } else if (seenSentences.add(normSentence)) {
                        // First time seeing this sentence
                        uniqueSentences.add(rawSentence.trim());
                    }
                }

                if (!uniqueSentences.isEmpty()) {
                    String cleanBody = String.join(" ", uniqueSentences);
                    seenParagraphBodies.add(normalize(cleanBody));
                    outputLines.add("<!-- wp:paragraph --><p>" + cleanBody + "</p><!-- /wp:paragraph -->");
                }
            } else {
                // Non-paragraph lines (headings, images, metadata)
                outputLines.add(trimmed);
            }
        }

        return String.join("\n", outputLines);
    }

    private String normalize(String text) {
        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
