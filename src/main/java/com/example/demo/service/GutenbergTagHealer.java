package com.example.demo.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GutenbergTagHealer implements ContentProcessor {

    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile(
            "(<!--\\s*wp:paragraph\\s*-->)(.*?)(<!--\\s*/wp:paragraph\\s*-->)",
            Pattern.DOTALL
    );

    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "(<!--\\s*wp:heading\\s*-->)(.*?)(<!--\\s*/wp:heading\\s*-->)",
            Pattern.DOTALL
    );

    @Override
    public String process(String content) {
        if (content == null) return null;

        // Fix paragraphs: ensure they are wrapped in <p>...</p> inside Gutenberg comments
        Matcher pMatcher = PARAGRAPH_PATTERN.matcher(content);
        StringBuilder sb = new StringBuilder();
        while (pMatcher.find()) {
            String openComment = pMatcher.group(1);
            String innerText = pMatcher.group(2).trim();
            String closeComment = pMatcher.group(3);

            if (!innerText.startsWith("<p>") && !innerText.startsWith("<p ")) {
                innerText = "<p>" + innerText;
            }
            if (!innerText.endsWith("</p>")) {
                innerText = innerText + "</p>";
            }
            pMatcher.appendReplacement(sb, Matcher.quoteReplacement(openComment + innerText + closeComment));
        }
        pMatcher.appendTail(sb);
        String step1 = sb.toString();

        // Fix headings: ensure they are wrapped in <h2>...</h2> (or other h-tags) inside Gutenberg comments
        Matcher hMatcher = HEADING_PATTERN.matcher(step1);
        sb = new StringBuilder();
        while (hMatcher.find()) {
            String openComment = hMatcher.group(1);
            String innerText = hMatcher.group(2).trim();
            String closeComment = hMatcher.group(3);

            if (!innerText.startsWith("<h1") && !innerText.startsWith("<h2") && !innerText.startsWith("<h3") && !innerText.startsWith("<h4") && !innerText.startsWith("<h5") && !innerText.startsWith("<h6")) {
                innerText = "<h2>" + innerText;
            }
            if (!innerText.endsWith("</h1>") && !innerText.endsWith("</h2>") && !innerText.endsWith("</h3>") && !innerText.endsWith("</h4>") && !innerText.endsWith("</h5>") && !innerText.endsWith("</h6>")) {
                if (innerText.startsWith("<h1")) {
                    innerText = innerText + "</h1>";
                } else if (innerText.startsWith("<h3")) {
                    innerText = innerText + "</h3>";
                } else if (innerText.startsWith("<h4")) {
                    innerText = innerText + "</h4>";
                } else if (innerText.startsWith("<h5")) {
                    innerText = innerText + "</h5>";
                } else if (innerText.startsWith("<h6")) {
                    innerText = innerText + "</h6>";
                } else {
                    innerText = innerText + "</h2>";
                }
            }
            hMatcher.appendReplacement(sb, Matcher.quoteReplacement(openComment + innerText + closeComment));
        }
        hMatcher.appendTail(sb);

        return sb.toString();
    }
}
