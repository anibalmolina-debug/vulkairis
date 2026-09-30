package com.vulkairis.rendering;

import com.vulkairis.vulkan.pipeline.PipelineKey;
import com.vulkairis.vulkan.pipeline.PipelineManager;

/**
 * Tracks OpenGL / Blaze3D rasterization, blend, depth, and scissor states
 * to build deterministic Vulkan pipeline keys.
 */
public class RenderStateTracker {
    private boolean blendEnabled = false;
    private int blendModeOrdinal = 0;
    private boolean depthTest = true;
    private boolean depthMask = true;
    private boolean cull = true;
    private int viewportWidth = 1920;
    private int viewportHeight = 1080;

    private static final RenderStateTracker INSTANCE = new RenderStateTracker();

    public static RenderStateTracker getInstance() {
        return INSTANCE;
    }

    public void setBlend(boolean enabled, int blendModeOrdinal) {
        this.blendEnabled = enabled;
        this.blendModeOrdinal = blendModeOrdinal;
    }

    public void setDepth(boolean test, boolean mask) {
        this.depthTest = test;
        this.depthMask = mask;
    }

    public void setCulling(boolean cull) {
        this.cull = cull;
    }

    public void setViewport(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    public PipelineKey createPipelineKey(long vertModule, long fragModule, String formatName) {
        return new PipelineKey(
                vertModule,
                fragModule,
                formatName,
                blendEnabled ? blendModeOrdinal : -1,
                depthTest,
                depthMask,
                cull
        );
    }

    public boolean isBlendEnabled() {
        return blendEnabled;
    }

    public boolean isDepthTest() {
        return depthTest;
    }

    public boolean isDepthMask() {
        return depthMask;
    }

    public boolean isCull() {
        return cull;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }
}
