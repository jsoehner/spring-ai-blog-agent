package com.example.demo.service;

public class MarkdownSanitizer implements ContentProcessor {
    @Override
    public String process(String content) {
        if (content == null) return null;
        String html = content.trim();
        if (html.startsWith("```html")) {
            html = html.substring(7);
        }
        if (html.endsWith("```")) {
            html = html.substring(0, html.length() - 3);
        }
        return html.trim();
    }
}
