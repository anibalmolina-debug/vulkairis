package com.vulkairis.rendering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages rendering passes: Color+Depth scene pass, intermediate shader passes,
 * and final composite pass.
 */
public class VulkanRenderPassManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vulkairis/RenderPassManager");

    public enum PassType {
        COLOR_DEPTH_SCENE,
        SHADER_INTERMEDIATE,
        FINAL_COMPOSITE
    }

    private PassType currentPass = PassType.COLOR_DEPTH_SCENE;

    public void beginPass(PassType passType) {
        this.currentPass = passType;
        LOGGER.debug("[Vulkairis/Pass] Began pass: {}", passType);
    }

    public void endPass() {
        LOGGER.debug("[Vulkairis/Pass] Ended pass: {}", currentPass);
    }

    public PassType getCurrentPass() {
        return currentPass;
    }
}
