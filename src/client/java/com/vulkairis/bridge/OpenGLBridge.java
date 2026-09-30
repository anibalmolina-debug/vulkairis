package com.vulkairis.bridge;

import com.vulkairis.rendering.RenderStateTracker;
import com.vulkairis.vulkan.descriptor.VulkanDescriptorManager;
import org.joml.Matrix4f;

/**
 * Maps high-level OpenGL and Blaze3D rendering concepts to Vulkairis / Vulkan equivalents.
 */
public class OpenGLBridge {
    /**
     * Correction matrix adapting OpenGL clip space Z [-1, 1] to Vulkan clip space Z [0, 1].
     */
    public static final Matrix4f CLIP_SPACE_CORRECTION = new Matrix4f(
            1.0f,  0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 0.0f, // Also flips Y if not already handled by compiler
            0.0f,  0.0f, 0.5f, 0.5f,
            0.0f,  0.0f, 0.0f, 1.0f
    );

    public static void setTextureBinding(long descriptorSet, int binding, int glTextureId) {
        VulkanDescriptorManager.updateTextureBinding(descriptorSet, binding, glTextureId);
    }

    public static void updateRenderState(boolean blend, int blendOrdinal, boolean depthTest, boolean depthMask, boolean cull) {
        RenderStateTracker tracker = RenderStateTracker.getInstance();
        tracker.setBlend(blend, blendOrdinal);
        tracker.setDepth(depthTest, depthMask);
        tracker.setCulling(cull);
    }
}
