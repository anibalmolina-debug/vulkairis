package com.vulkairis.client.mixin.iris;

import com.vulkairis.core.VulkairisController;
import com.vulkairis.translation.VulkanShaderRegistry;
import net.irisshaders.iris.gl.shader.GlShader;
import net.irisshaders.iris.gl.shader.ProgramCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = ProgramCreator.class, remap = false)
public class ProgramCreatorMixin {
    @Unique
    private static final Logger VULKAIRIS_LOGGER = LoggerFactory.getLogger("Vulkairis/ProgramCreatorMixin");

    /**
     * @author Vulkairis
     * @reason Intercept Iris program creation and link virtual Vulkan shader pipeline without raw OpenGL calls.
     */
    @Overwrite
    public static int create(String name, GlShader... shaders) {
        com.vulkairis.iris.StateUpdateNotifierManager.ensureInitialized();
        List<Integer> shaderIds = new ArrayList<>();
        if (shaders != null) {
            for (GlShader shader : shaders) {
                if (shader != null) {
                    shaderIds.add(shader.getHandle());
                }
            }
        }
        VULKAIRIS_LOGGER.info("[Vulkairis] Intercepted Iris program creation: '{}' with shaders {}", name, shaderIds);
        int progId = VulkanShaderRegistry.registerProgram(name, shaderIds);
        VulkairisController.getInstance().incrementPipelineCount();
        return progId;
    }
}
