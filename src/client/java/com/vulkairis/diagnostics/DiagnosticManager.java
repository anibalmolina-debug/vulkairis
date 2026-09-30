package com.vulkairis.diagnostics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Structured diagnostic recording and logging system for Vulkairis.
 */
public class DiagnosticManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vulkairis/Diagnostics");

    public enum Category {
        VULKAN,
        SHADER,
        PIPELINE,
        RESOURCE,
        FRAMEBUFFER,
        COMPATIBILITY,
        FALLBACK
    }

    public record DiagnosticRecord(
            Instant timestamp,
            Category category,
            String message,
            String details,
            Throwable throwable
    ) {
        @Override
        public String toString() {
            return String.format("[%s] [%s] %s%s",
                    timestamp,
                    category,
                    message,
                    (details != null && !details.isBlank()) ? " (" + details + ")" : "");
        }
    }

    private static final List<DiagnosticRecord> HISTORY = new CopyOnWriteArrayList<>();
    private static final int MAX_HISTORY = 200;

    private static String lastFallbackReason = null;

    public static void log(Category category, String message) {
        log(category, message, null, null);
    }

    public static void log(Category category, String message, String details) {
        log(category, message, details, null);
    }

    public static void log(Category category, String message, String details, Throwable throwable) {
        DiagnosticRecord record = new DiagnosticRecord(Instant.now(), category, message, details, throwable);
        if (HISTORY.size() >= MAX_HISTORY) {
            HISTORY.remove(0);
        }
        HISTORY.add(record);

        if (category == Category.FALLBACK) {
            lastFallbackReason = message + (details != null ? ": " + details : "");
            LOGGER.warn("[Vulkairis/Fallback] {}", record);
        } else if (throwable != null) {
            LOGGER.error("[Vulkairis/{}] {} - Details: {}", category, message, details, throwable);
        } else {
            LOGGER.info("[Vulkairis/{}] {}", category, record);
        }
    }

    public static void recordFallback(String reason, String details) {
        log(Category.FALLBACK, reason, details);
    }

    public static String getLastFallbackReason() {
        return lastFallbackReason != null ? lastFallbackReason : "None (Backend healthy)";
    }

    public static List<DiagnosticRecord> getRecentRecords(int limit) {
        int size = HISTORY.size();
        if (size == 0) return Collections.emptyList();
        int start = Math.max(0, size - limit);
        return Collections.unmodifiableList(new ArrayList<>(HISTORY.subList(start, size)));
    }

    public static void clearHistory() {
        HISTORY.clear();
        lastFallbackReason = null;
    }
}
