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
        String baseName = request.title().replaceAll("\\s+", "-").toLowerCase();
        String fileName = "output/" + baseName + ".html";
        String wpFileName = "output/" + baseName + "_wp.html";

        System.out.println("WordPressTool: Saving draft locally to " + fileName + " and " + wpFileName + "! Title: " + request.title());
        
        try {
            File file = new File(fileName);
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("<h1>" + request.title() + "</h1>");
                writer.write(request.content());
            }
            
            File wpFile = new File(wpFileName);
            if (wpFile.getParentFile() != null && !wpFile.getParentFile().exists()) {
                wpFile.getParentFile().mkdirs();
            }
            try (FileWriter wpWriter = new FileWriter(wpFile)) {
                wpWriter.write("<h1>" + request.title() + "</h1>");
                wpWriter.write(request.content());
            }
            System.out.println("Saved blog post to local file: " + fileName);
            return "Successfully saved draft locally to " + file.getAbsolutePath() + " and " + wpFile.getAbsolutePath() + ".\nYou can now open this file in your browser or text editor and paste it directly into WordPress!\nSaved blog post to local file: " + fileName;
        } catch (IOException e) {
            return "Failed to save draft locally: " + e.getMessage();
        }
    }
}
