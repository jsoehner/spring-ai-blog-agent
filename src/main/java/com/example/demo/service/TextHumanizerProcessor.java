package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Component
public class TextHumanizerProcessor implements ContentProcessor {

    private static final Logger log = LoggerFactory.getLogger(TextHumanizerProcessor.class);

    private final String pythonExecutable;
    private final String scriptPath;
    private final boolean enabled;

    public TextHumanizerProcessor(
            @Value("${humanizer.python.path:python3}") String pythonExecutable,
            @Value("${humanizer.script.path:src/main/resources/scripts/humanize.py}") String scriptPath,
            @Value("${humanizer.enabled:true}") boolean enabled) {
        this.pythonExecutable = pythonExecutable;
        this.scriptPath = scriptPath;
        this.enabled = enabled;
    }

    @Override
    public String process(String content) {
        if (!enabled || content == null || content.isBlank()) {
            return content;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, scriptPath);
            Process process = pb.start();

            try (OutputStream os = process.getOutputStream()) {
                os.write(content.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            String result;
            try (InputStream is = process.getInputStream()) {
                result = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            String errorOutput;
            try (InputStream es = process.getErrorStream()) {
                errorOutput = new String(es.readAllBytes(), StandardCharsets.UTF_8);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("TextHumanizer Python script failed with exit code {}: {}", exitCode, errorOutput);
                return content;
            }

            if (result != null && !result.isBlank()) {
                log.info("TextHumanizer successfully humanized content.");
                return result;
            }

            return content;
        } catch (Exception e) {
            log.error("Error executing TextHumanizer processor: {}", e.getMessage(), e);
            return content;
        }
    }
}
