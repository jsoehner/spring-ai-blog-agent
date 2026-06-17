package com.example.demo;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Component
public class WordPressTool {

    public record DraftRequest(String title, String content) {}

    @Tool(description = "Creates a draft blog post on the WordPress site. Input requires a catchy title and the full HTML content of the blog post.")
    public String createDraftPost(DraftRequest request) {
        System.out.println("WordPressTool: Saving draft locally to blog_draft.html! Title: " + request.title());
        
        try {
            File file = new File("blog_draft.html");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("<h1>" + request.title() + "</h1>\n\n");
                writer.write(request.content());
            }
            return "Successfully saved draft locally to " + file.getAbsolutePath() + ".\nYou can now open this file in your browser or text editor and paste it directly into WordPress!";
        } catch (IOException e) {
            return "Failed to save draft locally: " + e.getMessage();
        }
    }
}
