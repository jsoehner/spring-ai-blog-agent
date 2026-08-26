package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ContentPipeline {

    private static final Logger log = LoggerFactory.getLogger(ContentPipeline.class);

    private final List<ContentProcessor> processors;

    public ContentPipeline() {
        processors = new ArrayList<>();
        // Order matters here: sanitize markdown/blocks, validate HTML, inject SEO
        processors.add(new MarkdownSanitizer());
        processors.add(new HtmlValidator());
        processors.add(new SeoMetadataInjector());
    }

    public String process(String content) {
        String result = content;
        for (ContentProcessor processor : processors) {
            try {
                result = processor.process(result);
            } catch (Exception e) {
                log.error("Error in processor {}: {}", processor.getClass().getSimpleName(), e.getMessage());
                // Continue with previous result or throw exception depending on requirements
                // For now, we'll log and continue
            }
        }
        return result;
    }
}
