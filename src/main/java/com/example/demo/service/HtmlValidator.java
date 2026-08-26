package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlValidator implements ContentProcessor {
    private static final Logger log = LoggerFactory.getLogger(HtmlValidator.class);
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
