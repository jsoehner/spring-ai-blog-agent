package com.example.demo.service;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceTest {

    private final StorageService storageService = new StorageService();

    @Test
    void testGetSafePathNormalizesTopic() {
        Path htmlPath = storageService.getSafePath("Why Architectural Security Requirements are necessary", ".html");
        assertTrue(htmlPath.toString().endsWith("why-architectural-security-requirements-are-necessary.html"));

        Path wpPath = storageService.getSafePath("Why Architectural Security Requirements are necessary", "_wp.html");
        assertTrue(wpPath.toString().endsWith("why-architectural-security-requirements-are-necessary_wp.html"));
    }

    @Test
    void testSaveWordPressPostCreatesFile() throws IOException {
        String topic = "Test Security Post";
        String content = "<!-- wp:paragraph --><p>Content</p><!-- /wp:paragraph -->";

        storageService.saveWordPressPost(topic, content);

        Path expectedPath = storageService.getSafePath(topic, "_wp.html");
        assertTrue(Files.exists(expectedPath));
        assertEquals(content, Files.readString(expectedPath));

        // Clean up
        Files.deleteIfExists(expectedPath);
    }
}
