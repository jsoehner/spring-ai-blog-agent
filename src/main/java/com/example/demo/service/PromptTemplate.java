package com.example.demo.service;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class PromptTemplate {
    private String name;
    private String version;
    private String description;
    private String content;
}
