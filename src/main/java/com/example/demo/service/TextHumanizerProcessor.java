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
    private final String resolvedScriptPath;

    public TextHumanizerProcessor(
            @Value("${humanizer.python.path:python3}") String pythonExecutable,
            @Value("${humanizer.script.path:src/main/resources/scripts/humanize.py}") String scriptPath,
            @Value("${humanizer.enabled:true}") boolean enabled) {
        this.pythonExecutable = pythonExecutable;
        this.scriptPath = scriptPath;
        this.enabled = enabled;
        this.resolvedScriptPath = resolveScriptPath(scriptPath);
    }

    private String resolveScriptPath(String path) {
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            return path;
        }

        String resourceName = "scripts/humanize.py";
        if (path.contains("scripts/")) {
            resourceName = path.substring(path.indexOf("scripts/"));
        } else {
            int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
            if (lastSlash != -1) {
                resourceName = "scripts/" + path.substring(lastSlash + 1);
            }
        }

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (in != null) {
                File tempFile = File.createTempFile("humanize-", ".py");
                tempFile.deleteOnExit();
                try (OutputStream out = new FileOutputStream(tempFile)) {
                    in.transferTo(out);
                }
                log.info("Extracted script from classpath resource {} to {}", resourceName, tempFile.getAbsolutePath());
                return tempFile.getAbsolutePath();
            } else {
                log.warn("Could not find script path as file or classpath resource: {}", path);
            }
        } catch (IOException e) {
            log.error("Failed to extract humanizer script from classpath resource: {}", path, e);
        }
        return path;
    }

    @Override
    public String process(String content) {
        if (!enabled || content == null || content.isBlank()) {
            return content;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(pythonExecutable, resolvedScriptPath);
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
