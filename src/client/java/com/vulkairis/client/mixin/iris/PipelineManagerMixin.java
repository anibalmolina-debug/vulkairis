package com.vulkairis.client.mixin.iris;

import net.irisshaders.iris.pipeline.PipelineManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = PipelineManager.class, remap = false)
public class PipelineManagerMixin {
    @Unique
    private static final Logger VULKAIRIS_LOGGER = LogManager.getLogger("Vulkairis/PipelineManagerMixin");

    /**
     * @author Vulkairis
     * @reason Safe no-op under Vulkan; OpenGL texture units do not exist and calling raw GL causes segfaults.
     */
    @Overwrite(remap = false)
    private void resetTextureState() {
        VULKAIRIS_LOGGER.debug("[Vulkairis] Safely bypassed Iris resetTextureState under Vulkan");
    }
}
