package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.demo.agent.HtmlValidator;
import com.example.demo.agent.MarkdownSanitizer;
import com.example.demo.agent.SeoMetadataInjector;
import com.example.demo.agent.SentenceDeduplicator;
import com.example.demo.agent.FactVerifier;
import com.example.demo.agent.ContentProcessor;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ContentPipeline {

    private static final Logger log = LoggerFactory.getLogger(ContentPipeline.class);

    private final List<ContentProcessor> processors;

    public ContentPipeline() {
        this(new FactVerifier(), new MarkdownSanitizer(), new SentenceDeduplicator(), new HtmlValidator(), new SeoMetadataInjector());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ContentPipeline(FactVerifier factVerifier,
                           MarkdownSanitizer sanitizer,
                           SentenceDeduplicator deduplicator,
                           HtmlValidator validator,
                           SeoMetadataInjector seoInjector) {
        processors = new ArrayList<>();
        // Order matters here: verify facts, sanitize markdown/blocks, deduplicate repeated content, validate HTML, inject SEO
        processors.add(factVerifier);
        processors.add(sanitizer);
        processors.add(deduplicator);
        processors.add(validator);
        processors.add(seoInjector);
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
