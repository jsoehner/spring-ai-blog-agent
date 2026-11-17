package com.example.demo.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebCrawlerConfigTest {

    private final WebCrawlerConfig config = new WebCrawlerConfig();

    @Test
    void testAllowsExactAndSubdomainAllowlistedUrls() {
        assertTrue(config.isSafeUrl("https://owasp.org/www-project-top-ten/"));
        assertTrue(config.isSafeUrl("https://en.wikipedia.org/wiki/Cryptography"));
    }

    @Test
    void testBlocksDomainSuffixBypass() {
        assertFalse(config.isSafeUrl("https://evilowasp.org/payload"));
        assertFalse(config.isSafeUrl("https://notwikipedia.org/test"));
    }

    @Test
    void testBlocksLocalhostPrivateIpsAndNonHttpSchemes() {
        assertFalse(config.isSafeUrl("http://127.0.0.1:8080/admin"));
        assertFalse(config.isSafeUrl("http://localhost:8181/v1/data"));
        assertFalse(config.isSafeUrl("http://169.254.169.254/latest/meta-data/"));
        assertFalse(config.isSafeUrl("file:///etc/passwd"));
    }
}
