package com.vulkairis.config;

/**
 * Configuration model for Vulkairis with safe defaults.
 */
public class VulkairisConfig {
    public enum CompatibilityMode {
        AUTOMATIC,
        STRICT
    }

    private boolean enabled = true;
    private boolean useVulkanRenderer = true;
    private CompatibilityMode compatibilityMode = CompatibilityMode.AUTOMATIC;
    private boolean dryRunTesting = true;
    private boolean debugLogging = true;
    private boolean debugOverlay = false;
    private boolean dumpSpirv = false;

    public VulkairisConfig() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isUseVulkanRenderer() {
        return useVulkanRenderer;
    }

    public void setUseVulkanRenderer(boolean useVulkanRenderer) {
        this.useVulkanRenderer = useVulkanRenderer;
    }

    public CompatibilityMode getCompatibilityMode() {
        return compatibilityMode;
    }

    public void setCompatibilityMode(CompatibilityMode compatibilityMode) {
        this.compatibilityMode = compatibilityMode;
    }

    public boolean isDryRunTesting() {
        return dryRunTesting;
    }

    public void setDryRunTesting(boolean dryRunTesting) {
        this.dryRunTesting = dryRunTesting;
    }

    public boolean isDebugLogging() {
        return debugLogging;
    }

    public void setDebugLogging(boolean debugLogging) {
        this.debugLogging = debugLogging;
    }

    public boolean isDebugOverlay() {
        return debugOverlay;
    }

    public void setDebugOverlay(boolean debugOverlay) {
        this.debugOverlay = debugOverlay;
    }

    public boolean isDumpSpirv() {
        return dumpSpirv;
    }

    public void setDumpSpirv(boolean dumpSpirv) {
        this.dumpSpirv = dumpSpirv;
    }
}
