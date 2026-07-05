package com.example.demo;

import org.springframework.ai.tool.annotation.Tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Component;

@Component
public class CodeTools {

    @Tool(description = "Reads the contents of a source code file or text file from the local filesystem. Input is the absolute file path.")
    public String readFile(String absolutePath) {
        try {
            Path path = Paths.get(absolutePath);
            if (!Files.exists(path)) {
                return "Error: File does not exist at " + absolutePath;
            }
            return Files.readString(path);
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    public record WriteRequest(String absolutePath, String content) {}

    @Tool(description = "Writes or overwrites a file with the provided content. Use this to save refactored code, apply fixes, or create new files. Input requires the absolute file path and the complete new content of the file.")
    public String writeFile(WriteRequest request) {
        try {
            Path path = Paths.get(request.absolutePath());
            
            // Create parent directories if they don't exist
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            
            Files.writeString(path, request.content());
            return "Successfully wrote file to " + request.absolutePath();
        } catch (Exception e) {
            return "Error writing file: " + e.getMessage();
        }
    }
}
