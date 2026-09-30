package com.vulkairis.core;

import com.vulkairis.config.ConfigManager;
import com.vulkairis.config.VulkairisConfig;
import com.vulkairis.diagnostics.DiagnosticManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Central controller coordinating Vulkairis backend state, rendering path,
 * bridge subsystems, and fallback transitions.
 */
public class VulkairisController {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vulkairis/Controller");
    private static final VulkairisController INSTANCE = new VulkairisController();

    private volatile BackendState state = BackendState.DISABLED;
    private volatile String activeShaderPackName = "None (Internal)";
    private final AtomicLong frameCount = new AtomicLong(0);
    private final AtomicLong compiledShadersCount = new AtomicLong(0);
    private final AtomicLong createdPipelinesCount = new AtomicLong(0);

    private VulkairisController() {
    }

    public static VulkairisController getInstance() {
        return INSTANCE;
    }

    public synchronized void initialize() {
        LOGGER.info("[Vulkairis] Initializing Vulkairis Controller...");
        VulkairisConfig config = ConfigManager.loadConfig();

        if (!config.isEnabled()) {
            LOGGER.info("[Vulkairis] Mod is disabled via configuration.");
            this.state = BackendState.DISABLED;
            return;
        }

        this.state = BackendState.INITIALIZING;
        DiagnosticManager.log(DiagnosticManager.Category.VULKAN, "Starting Vulkairis controller initialization");

        try {
            // Further subsystem startup will transition state to READY once verified
            LOGGER.info("[Vulkairis] Controller initial state configured.");
        } catch (Throwable t) {
            triggerFallback("Controller initialization failed", t.getMessage());
        }
    }

    public BackendState getState() {
        return this.state;
    }

    public synchronized void setState(BackendState newState) {
        LOGGER.info("[Vulkairis] Backend state transition: {} -> {}", this.state, newState);
        this.state = newState;
    }

    public synchronized void triggerFallback(String reason, String details) {
        LOGGER.warn("[Vulkairis] Triggering FALLBACK mode. Reason: {} ({})", reason, details);
        this.state = BackendState.FALLBACK;
        DiagnosticManager.recordFallback(reason, details);
    }

    public boolean isVulkanActive() {
        return this.state.isOperational() && ConfigManager.getConfig().isUseVulkanRenderer();
    }

    public String getActiveShaderPackName() {
        return activeShaderPackName;
    }

    public void setActiveShaderPackName(String activeShaderPackName) {
        this.activeShaderPackName = (activeShaderPackName != null) ? activeShaderPackName : "None (Internal)";
    }

    public long incrementFrameCount() {
        return frameCount.incrementAndGet();
    }

    public long getFrameCount() {
        return frameCount.get();
    }

    public void incrementShaderCount() {
        compiledShadersCount.incrementAndGet();
    }

    public long getCompiledShadersCount() {
        return compiledShadersCount.get();
    }

    public void incrementPipelineCount() {
        createdPipelinesCount.incrementAndGet();
    }

    public long getCreatedPipelinesCount() {
        return createdPipelinesCount.get();
    }

    public void reset() {
        frameCount.set(0);
        this.state = BackendState.INITIALIZING;
    }
}
