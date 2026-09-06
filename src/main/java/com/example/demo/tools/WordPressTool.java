package com.example.demo.tools;

import com.example.demo.service.ExternalTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Component
public class WordPressTool implements ExternalTool {

    private static final Logger log = LoggerFactory.getLogger(WordPressTool.class);

    public record DraftRequest(String topic, String content) {}

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
        String topic = (String) inputs.get("topic");
        if (topic == null) {
            topic = (String) inputs.get("title");
        }
        String content = (String) inputs.get("content");
        return createDraftPost(new DraftRequest(topic, content));
    }

    @Tool(description = "Creates a draft blog post on the WordPress site. Input requires a catchy title and the full HTML content of the blog post.")
    public String createDraftPost(DraftRequest request) {
        String safeBaseName = request.topic().replaceAll("[^a-zA-Z0-9\\s-]", "").strip().replaceAll("\\s+", "-").toLowerCase();
        
        Path baseDir = Paths.get("output").toAbsolutePath().normalize();
        Path fileTarget = baseDir.resolve(safeBaseName + ".html").normalize();
        Path wpFileTarget = baseDir.resolve(safeBaseName + "_wp.html").normalize();
        
        if (!fileTarget.startsWith(baseDir) || !wpFileTarget.startsWith(baseDir)) {
            throw new SecurityException("Path traversal attempt detected!");
        }
        
        String fileName = fileTarget.toString();
        String wpFileName = wpFileTarget.toString();

        log.info("WordPressTool: Saving draft locally to {} and {}! Topic: {}", fileName, wpFileName, request.topic());
        
        try {
            File file = fileTarget.toFile();
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(request.content());
            }
            
            File wpFile = wpFileTarget.toFile();
            if (wpFile.getParentFile() != null && !wpFile.getParentFile().exists()) {
                wpFile.getParentFile().mkdirs();
            }
            try (FileWriter wpWriter = new FileWriter(wpFile)) {
                wpWriter.write(request.content());
            }
            log.info("Saved blog post to local file: {}", fileName);
            return "Successfully saved draft locally to " + file.getAbsolutePath() + " and " + wpFile.getAbsolutePath() + ".\nSaved blog post to local file: " + fileName;
        } catch (IOException e) {
            log.error("Failed to save draft: {}", e.getMessage());
            return "Failed to save draft locally: " + e.getMessage();
        }
    }
}
