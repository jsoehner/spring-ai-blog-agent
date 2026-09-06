package com.example.demo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SentenceDeduplicatorTest {

    private final SentenceDeduplicator deduplicator = new SentenceDeduplicator();

    @Test
    void testRemovesDuplicateSentencesAcrossParagraphs() {
        String input = """
                <!-- wp:paragraph --><p>Security is foundational to system architecture. Good design prevents misuse and protects user data.</p><!-- /wp:paragraph -->
                <!-- wp:heading --><h2>Next Section</h2><!-- /wp:heading -->
                <!-- wp:paragraph --><p>Security is foundational to system architecture. Continuous monitoring is also essential for operational health.</p><!-- /wp:paragraph -->
                """;

        String expected = """
                <!-- wp:paragraph --><p>Security is foundational to system architecture. Good design prevents misuse and protects user data.</p><!-- /wp:paragraph -->
                <!-- wp:heading --><h2>Next Section</h2><!-- /wp:heading -->
                <!-- wp:paragraph --><p>Continuous monitoring is also essential for operational health.</p><!-- /wp:paragraph -->""";

        String actual = deduplicator.process(input);
        assertEquals(expected, actual);
    }

    @Test
    void testRemovesFullyDuplicateParagraphBlocks() {
        String input = """
                <!-- wp:paragraph --><p>Integrating security at the architectural level allows for the implementation of privacy as default.</p><!-- /wp:paragraph -->
                <!-- wp:paragraph --><p>Integrating security at the architectural level allows for the implementation of privacy as default.</p><!-- /wp:paragraph -->
                """;

        String expected = """
                <!-- wp:paragraph --><p>Integrating security at the architectural level allows for the implementation of privacy as default.</p><!-- /wp:paragraph -->""";

        String actual = deduplicator.process(input);
        assertEquals(expected, actual);
    }
}
