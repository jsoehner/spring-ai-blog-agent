package com.example.demo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Supplier;

public class RetryUtils {
    private static final Logger log = LoggerFactory.getLogger(RetryUtils.class);
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    public static <T> T executeWithRetry(String actionName, Supplier<T> action) {
        int attempts = 0;
        while (true) {
            try {
                return action.get();
            } catch (Exception e) {
                attempts++;
                if (attempts >= MAX_RETRIES) {
                    log.error("Action '{}' failed after {} attempts: {}", actionName, MAX_RETRIES, e.getMessage());
                    throw new RuntimeException("Action " + actionName + " failed after maximum retries", e);
                }
                long backoff = INITIAL_BACKOFF_MS * (long) Math.pow(2, attempts - 1);
                log.warn("Action '{}' failed (attempt {}/{}). Retrying in {}ms...", actionName, attempts, MAX_RETRIES, backoff);
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
    }
}
