package com.vulkairis.client.iris;

import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.texture.DepthCopyStrategy;

public class VulkanDepthCopyStrategy implements DepthCopyStrategy {
    public static final VulkanDepthCopyStrategy INSTANCE = new VulkanDepthCopyStrategy();

    @Override
    public boolean needsDestFramebuffer() {
        return false;
    }

    @Override
    public void copy(GlFramebuffer src, int srcTexture, GlFramebuffer dst, int dstTexture, int width, int height) {
        // Handled under Vulkan backend via Vulkan render passes or VkCmdCopyImage
    }
}
