package com.example.demo.service;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class StorageService implements StorageService {

    @Override
    public void saveBlogPost(String topic, String content) throws IOException {
        Path targetFile = getSafePath(topic, ".html");
        
        if (!Files.exists(targetFile.getParent())) {
            Files.createDirectories(targetFile.getParent());
        }
        
        Files.writeString(targetFile, content);
        System.out.println("Saved blog post to local file: " + targetFile.toString());
    }

    public Path getSafePath(String topic, String extension) {
        // Sanitize filename to prevent path traversal
        String safeBaseName = topic.replaceAll("[^a-zA-Z0-9\\s-]", "").strip();
        String filename = safeBaseName.replaceAll("\\s+", "-").toLowerCase() + extension;
        
        Path baseDir = Paths.get("output").toAbsolutePath().normalize();
        Path targetFile = baseDir.resolve(filename).normalize();
        
        if (!targetFile.startsWith(baseDir)) {
            throw new SecurityException("Path traversal attempt detected!");
        }
        
        return targetFile;
    }
}
