package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ContentPipeline {

    private final List<ContentProcessor> processors = new ArrayList<>();

    public ContentPipeline() {
        // Order matters here
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
