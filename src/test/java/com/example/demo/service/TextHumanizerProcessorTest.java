package com.example.demo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextHumanizerProcessorTest {

    @Test
    void testProcessHumanizesText() {
        TextHumanizerProcessor processor = new TextHumanizerProcessor("python3", "src/main/resources/scripts/humanize.py", true);
        String input = "Furthermore, it is imperative to delve into key considerations.";
        String output = processor.process(input);

        assertNotNull(output);
        assertFalse(output.isBlank());
        // Verify output is modified or non-empty string
        logOutput(output);
    }

    @Test
    void testDisabledProcessorReturnsOriginalText() {
        TextHumanizerProcessor processor = new TextHumanizerProcessor("python3", "src/main/resources/scripts/humanize.py", false);
        String input = "Furthermore, it is imperative to delve into key considerations.";
        String output = processor.process(input);

        assertEquals(input, output);
    }

    private void logOutput(String output) {
        System.out.println("Humanized result: " + output);
    }
}
