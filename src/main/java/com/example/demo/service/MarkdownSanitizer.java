package com.example.demo.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownSanitizer implements ContentProcessor {

    private static final Pattern MARKDOWN_FENCE = Pattern.compile("^```(?:html)?\\s*([\\s\\S]*?)\\s*```$", Pattern.CASE_INSENSITIVE);
    private static final Pattern WP_OPEN_TAG_SPACES = Pattern.compile("<!--\\s*wp:\\s*([a-zA-Z0-9/_-]+)(\\s+[\\s\\S]*?)?\\s*-->");
    private static final Pattern WP_CLOSE_TAG_SPACES = Pattern.compile("<!--\\s*/wp:\\s*([a-zA-Z0-9/_-]+)\\s*-->");
    private static final Pattern WP_BLOCK_INTERNAL_SPACING = Pattern.compile("(<!--\\s*wp:[a-zA-Z0-9/_-]+(?:\\s+[^>]*)?-->)\\s*([\\s\\S]*?)\\s*(<!--\\s*/wp:[a-zA-Z0-9/_-]+\\s*-->)");

    @Override
    public String process(String content) {
        if (content == null) return null;
        String text = content.replace("\r\n", "\n").replace("\r", "\n").trim();

        // Strip markdown code fences if present
        Matcher fenceMatcher = MARKDOWN_FENCE.matcher(text);
        if (fenceMatcher.matches()) {
            text = fenceMatcher.group(1).trim();
        } else {
            if (text.startsWith("```html")) {
                text = text.substring(7);
            } else if (text.startsWith("```")) {
                text = text.substring(3);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            text = text.trim();
        }

        // Normalize spaces inside wp comment tags: <!-- wp: paragraph --> -> <!-- wp:paragraph -->
        text = WP_OPEN_TAG_SPACES.matcher(text).replaceAll(mr -> {
            String tag = mr.group(1);
            String attrs = mr.group(2) != null ? mr.group(2).trim() : "";
            return attrs.isEmpty() ? "<!-- wp:" + tag + " -->" : "<!-- wp:" + tag + " " + attrs + " -->";
        });
        text = WP_CLOSE_TAG_SPACES.matcher(text).replaceAll(mr -> "<!-- /wp:" + mr.group(1) + " -->");

        // Remove linebreaks and extra spaces between block comments and their enclosed HTML element
        text = WP_BLOCK_INTERNAL_SPACING.matcher(text).replaceAll(mr -> {
            String openTag = mr.group(1).trim();
            String inner = mr.group(2).trim();
            String closeTag = mr.group(3).trim();

            if ((inner.startsWith("<p>") && inner.endsWith("</p>")) ||
                (inner.matches("^<h[1-6]>.*</h[1-6]>$"))) {
                int openTagEnd = inner.indexOf('>');
                int closeTagStart = inner.lastIndexOf('<');
                String tagOpen = inner.substring(0, openTagEnd + 1);
                String tagClose = inner.substring(closeTagStart);
                String body = inner.substring(openTagEnd + 1, closeTagStart).trim().replaceAll("\\s+", " ");
                inner = tagOpen + body + tagClose;
            }

            return openTag + inner + closeTag;
        });

        // Remove empty/blank lines and excess linefeeds
        String[] lines = text.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(trimmed);
            }
        }

        return sb.toString();
    }
}
