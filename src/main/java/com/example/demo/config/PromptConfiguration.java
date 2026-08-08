package com.example.demo.config;

import com.example.demo.service.PromptTemplate;
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
                "You are a professional tech blogger. Write a blog post about: {topic}. Use markdown formatting and include an introduction, body, and conclusion.")
        );
    }
}
