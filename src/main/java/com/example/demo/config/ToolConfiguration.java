package com.example.demo.config;

import com.example.demo.WordPressTool;
import com.example.demo.service.ToolRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfiguration {

    @Bean
    public ToolRegistry toolRegistry(WordPressTool wordpressTool) {
        ToolRegistry registry = new ToolRegistry();
        registry.registerTool(wordpressTool);
        return registry;
    }
}
