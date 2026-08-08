package com.example.demo;

import com.example.demo.service.ExternalTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class WordPressTool implements ExternalTool {

    public record DraftRequest(String title, String content) {}

    @Override
    public String getName() {
        return "WordPressTool";
    }

    @Override
    public String getDescription() {
        return "Creates a draft blog post on the WordPress site. Input requires a catchy title and the full HTML content of the blog post.";
    }

    @Override
    public Object execute(Map<String, Object> inputs) throws Exception {
        String title = (String) inputs.get("title");
        String content = (String) inputs.get("content");
        return createDraftPost(new DraftRequest(title, content));
    }

    @Tool(description = "Creates a draft blog post on the WordPress site. Input requires a catchy title and the full HTML content of the blog post.")
    public String createDraftPost(DraftRequest request) {
        // Sanitize title to prevent path traversal
        String safeBaseName = request.title().replaceAll("[^a-zA-Z0-9\\s-]", "").strip().replaceAll("\\s+", "-").toLowerCase();
        
        java.nio.file.Path baseDir = java.nio.file.Paths.get("output").toAbsolutePath().normalize();
        java.nio.file.Path fileTarget = baseDir.resolve(safeBaseName + ".html").normalize();
        java.nio.file.Path wpFileTarget = baseDir.resolve(safeBaseName + "_wp.html").normalize();
        
        if (!fileTarget.startsWith(baseDir) || !wpFileTarget.startsWith(baseDir)) {
            throw new SecurityException("Path traversal attempt detected!");
        }
        
        String fileName = fileTarget.toString();
        String wpFileName = wpFileTarget.toString();

        log.info("WordPressTool: Saving draft locally to {} and {}! Title: {}", fileName, wpFileName, request.title());
        
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
            log.info("Saved blog post to local file: {}", fileName);
            return "Successfully saved draft locally to " + file.getAbsolutePath() + " and " + wpFile.getAbsolutePath() + ".\nYou can now open this file in your browser or text editor and paste it directly into WordPress!\nSaved blog post to local file: " + fileName;
        } catch (IOException e) {
            log.error("Failed to save draft: {}", e.getMessage());
            return "Failed to save draft locally: " + e.getMessage();
        }
    }
}
