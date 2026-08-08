package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HtmlValidator implements ContentProcessor {
    @Override
    public String process(String content) {
        if (content == null) return null;
        
        // Basic validation checks
        if (!content.contains("<h1") && !content.contains("<h2")) {
            log.warn("Warning: Content does not appear to contain any headings.");
        }
        
        if (content.split("\n").length < 5) {
            log.warn("Warning: Content has fewer than 5 lines/paragraphs.");
        }

        return content;
    }
}
