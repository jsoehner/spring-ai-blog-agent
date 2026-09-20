package com.example.demo.config;

import com.example.demo.agent.PromptTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class PromptConfiguration {

    @Bean
    public List<PromptTemplate> promptTemplates() {
        return List.of(
            new PromptTemplate("blogger-prompt", "v1.0.0", "Initial blogger prompt", 
                "You are a professional tech blogger. Write a blog post about: {topic}"),
            new PromptTemplate("blogger-prompt", "v1.1.0", "Improved blogger prompt with formatting instructions", 
                "You are a professional tech blogger. Write a blog post about: {topic}. Use markdown formatting and include an introduction, body, and conclusion."),
            new PromptTemplate("blogger-prompt", "v1.2.0", "Expert copyeditor and content optimizer prompt with sentence & paragraph scannability rules",
                "You are an expert copyeditor and content optimizer. Write a blog post about: {topic}. Format text for maximum scannability and readability. Lead with value using an answer-first style. Keep sentences short (15-20 words max) in active voice with no run-ons. Enforce a 3-sentence cap per paragraph with under 60 words per prose block. Enforce scan-path bolding for key terms."),
            new PromptTemplate("blogger-prompt", "v1.3.0", "Unified idea paragraph structure with ~75 words and ~5 sentences per paragraph",
                "You are an expert technical writer and content editor. Write a blog post about: {topic}. Structure paragraphs around a single unified idea (~75 words and 3-5 sentences per block) using a topic sentence, supporting sentences with crisp 15-20 word phrasing, and a concluding sentence. Avoid run-ons and bullet points.")
        );
    }
}
