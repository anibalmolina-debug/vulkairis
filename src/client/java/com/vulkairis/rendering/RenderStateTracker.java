package com.vulkairis.rendering;

import com.vulkairis.vulkan.pipeline.PipelineKey;
import com.vulkairis.vulkan.pipeline.PipelineManager;
import com.vulkairis.vulkan.pipeline.VulkanPipelineFactory;

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
        boolean blend = blendEnabled;
        return new PipelineKey(
                vertModule,
                fragModule,
                formatName,
                // Render pass is resolved by the renderer when it selects a pass;
                // 0 means "unspecified" for state-only keys.
                0L,
                VulkanPipelineFactory.DEFAULT_TOPOLOGY,
                depthTest,
                depthMask,
                VulkanPipelineFactory.DEFAULT_DEPTH_COMPARE_OP,
                VulkanPipelineFactory.DEFAULT_FRONT_FACE,
                cull ? VulkanPipelineFactory.BACK_FACE_CULL_MODE : VulkanPipelineFactory.NO_CULL_MODE,
                blend ? blendModeOrdinal : -1,
                blend ? VulkanPipelineFactory.TRANSLUCENT_SRC_COLOR_FACTOR : 0,
                blend ? VulkanPipelineFactory.TRANSLUCENT_DST_COLOR_FACTOR : 0,
                blend ? VulkanPipelineFactory.TRANSLUCENT_COLOR_BLEND_OP : 0,
                blend ? VulkanPipelineFactory.TRANSLUCENT_SRC_ALPHA_FACTOR : 0,
                blend ? VulkanPipelineFactory.TRANSLUCENT_DST_ALPHA_FACTOR : 0,
                blend ? VulkanPipelineFactory.TRANSLUCENT_ALPHA_BLEND_OP : 0,
                VulkanPipelineFactory.DEFAULT_COLOR_WRITE_MASK,
                VulkanPipelineFactory.DEFAULT_SAMPLE_COUNT,
                0L,
                "",
                (depthTest ? 1 : 0) | (depthMask ? 2 : 0) | (cull ? 4 : 0) | (blend ? 8 : 0)
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
