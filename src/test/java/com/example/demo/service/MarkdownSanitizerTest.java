package com.example.demo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownSanitizerTest {

    private final MarkdownSanitizer sanitizer = new MarkdownSanitizer();

    @Test
    void testNormalizesUserReportedSpacingAndLinefeeds() {
        String input = """
                <!-- wp: paragraph -->
                <p>Leadership ought to oversee a diverse array of technologies, including cloud computing, data management — and specialized firm apps that handle daily workloads for staff. Security gaps often exist between other platforms because they may not talk with one another well or share threat intelligence automatically between teams. Addressing these vulnerabilities requires consistent auditing processes to maintain trust in digital services provided to clients and employees alike.</p>
                <!-- /wp: paragraph -->

                <!-- wp: heading -->
                <h4>Securing Infrastructure and Strategic Summary</h4>
                <!-- /wp: heading -->
                """;

        String expected = """
                <!-- wp:paragraph --><p>Leadership ought to oversee a diverse array of technologies, including cloud computing, data management — and specialized firm apps that handle daily workloads for staff. Security gaps often exist between other platforms because they may not talk with one another well or share threat intelligence automatically between teams. Addressing these vulnerabilities requires consistent auditing processes to maintain trust in digital services provided to clients and employees alike.</p><!-- /wp:paragraph -->
                <!-- wp:heading --><h4>Securing Infrastructure and Strategic Summary</h4><!-- /wp:heading -->""";

        String actual = sanitizer.process(input);
        assertEquals(expected, actual);
    }

    @Test
    void testStripsMarkdownFencesAndCleansCarriageReturns() {
        String input = "```html\r\n<!-- wp:heading -->\r\n<h2>Title</h2>\r\n<!-- /wp:heading -->\r\n\r\n<!-- wp:paragraph -->\r\n<p>Content line</p>\r\n<!-- /wp:paragraph -->\r\n```";

        String expected = "<!-- wp:heading --><h2>Title</h2><!-- /wp:heading -->\n<!-- wp:paragraph --><p>Content line</p><!-- /wp:paragraph -->";

        String actual = sanitizer.process(input);
        assertEquals(expected, actual);
    }

    @Test
    void testNormalizesTagNamesWithExtraSpaces() {
        String input = "<!--   wp:   paragraph   -->\n<p>Test</p>\n<!--   /wp:   paragraph   -->";
        String expected = "<!-- wp:paragraph --><p>Test</p><!-- /wp:paragraph -->";

        String actual = sanitizer.process(input);
        assertEquals(expected, actual);
    }
}
