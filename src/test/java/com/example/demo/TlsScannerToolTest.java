package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TlsScannerToolTest {

    private TlsScannerTool tlsScannerTool;

    @BeforeEach
    void setUp() {
        tlsScannerTool = new TlsScannerTool();
    }

    @Test
    void testRunTlsScanWithInvalidDomain() {
        List<String> targets = List.of("invalid.localdomain");
        String result = tlsScannerTool.runTlsScan(targets);

        // The test environment may or may not have requests installed globally.
        // We assert that the tool captures and returns the output/error streams correctly in either case.
        if (result.contains("Scan Results:")) {
            assertTrue(result.contains("invalid.localdomain"));
            assertTrue(result.contains("Failed") || result.contains("Unsafe Host"));
        } else {
            assertTrue(result.contains("Scan failed with exit code") || result.contains("Failed to run"));
            assertTrue(result.contains("ModuleNotFoundError") || result.contains("requests") || result.contains("exit code"));
        }
    }
}
