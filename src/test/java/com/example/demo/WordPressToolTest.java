package com.example.demo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WordPressToolTest {

    private WordPressTool wordPressTool;
    private final String testTitle = "Test WP Draft Title";
    private final String expectedBaseName = "test-wp-draft-title";
    private final Path expectedFile = Paths.get("output", expectedBaseName + ".html");
    private final Path expectedWpFile = Paths.get("output", expectedBaseName + "_wp.html");

    @BeforeEach
    void setUp() throws IOException {
        wordPressTool = new WordPressTool();
        // Ensure clean state
        Files.deleteIfExists(expectedFile);
        Files.deleteIfExists(expectedWpFile);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(expectedFile);
        Files.deleteIfExists(expectedWpFile);
    }

    @Test
    void testCreateDraftPostSuccess() {
        WordPressTool.DraftRequest request = new WordPressTool.DraftRequest(testTitle, "<p>Hello world</p>");
        String response = wordPressTool.createDraftPost(request);

        assertTrue(response.contains("Successfully saved draft locally"));
        assertTrue(Files.exists(expectedFile));
        assertTrue(Files.exists(expectedWpFile));
    }

    @Test
    void testCreateDraftPostFailure() throws IOException {
        // Simulate write failure by creating a directory where the file should be written
        // A FileWriter cannot write to a directory, causing an IOException.
        Files.createDirectories(expectedFile);

        WordPressTool.DraftRequest request = new WordPressTool.DraftRequest(testTitle, "<p>Hello world</p>");
        String response = wordPressTool.createDraftPost(request);

        assertTrue(response.contains("Failed to save draft locally"));

        // Cleanup directory
        Files.deleteIfExists(expectedFile);
    }
}
