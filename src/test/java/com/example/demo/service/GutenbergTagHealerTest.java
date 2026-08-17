package com.example.demo.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GutenbergTagHealerTest {

    private final GutenbergTagHealer healer = new GutenbergTagHealer();

    @Test
    public void testHealsMissingParagraphTags() {
        String input = "<!-- wp:paragraph -->This is a paragraph without tags.<!-- /wp:paragraph -->";
        String expected = "<!-- wp:paragraph --><p>This is a paragraph without tags.</p><!-- /wp:paragraph -->";
        assertEquals(expected, healer.process(input));
    }

    @Test
    public void testDoesNotDuplicateExistingParagraphTags() {
        String input = "<!-- wp:paragraph --><p>This has tags already.</p><!-- /wp:paragraph -->";
        assertEquals(input, healer.process(input));
    }

    @Test
    public void testHealsMissingHeadingTags() {
        String input = "<!-- wp:heading -->This is a heading without tags<!-- /wp:heading -->";
        String expected = "<!-- wp:heading --><h2>This is a heading without tags</h2><!-- /wp:heading -->";
        assertEquals(expected, healer.process(input));
    }

    @Test
    public void testDoesNotDuplicateExistingHeadingTags() {
        String input = "<!-- wp:heading --><h2>This has tags already</h2><!-- /wp:heading -->";
        assertEquals(input, healer.process(input));
        
        String inputH3 = "<!-- wp:heading --><h3>This is an H3</h3><!-- /wp:heading -->";
        assertEquals(inputH3, healer.process(inputH3));
    }

    @Test
    public void testMixedContent() {
        String input = "<!-- wp:paragraph -->Para 1<!-- /wp:paragraph -->\n" +
                       "<!-- wp:heading -->Heading 1<!-- /wp:heading -->\n" +
                       "<!-- wp:paragraph --><p>Para 2 with tags</p><!-- /wp:paragraph -->";
                       
        String expected = "<!-- wp:paragraph --><p>Para 1</p><!-- /wp:paragraph -->\n" +
                          "<!-- wp:heading --><h2>Heading 1</h2><!-- /wp:heading -->\n" +
                          "<!-- wp:paragraph --><p>Para 2 with tags</p><!-- /wp:paragraph -->";
                          
        assertEquals(expected, healer.process(input));
    }
}
