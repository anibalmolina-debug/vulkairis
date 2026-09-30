package com.vulkairis.client.mixin.iris;

import com.vulkairis.compatibility.AnalysisResult;
import com.vulkairis.compatibility.ShaderAnalyzer;
import com.vulkairis.core.VulkairisController;
import com.vulkairis.diagnostics.DiagnosticManager;
import com.vulkairis.translation.SpirvCompiler;
import com.vulkairis.translation.VulkanShaderRegistry;
import net.irisshaders.iris.gl.GlResource;
import net.irisshaders.iris.gl.shader.GlShader;
import net.irisshaders.iris.gl.shader.ShaderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = GlShader.class, remap = false)
public abstract class GlShaderMixin extends GlResource {
    @Unique
    private static final Logger VULKAIRIS_LOGGER = LoggerFactory.getLogger("Vulkairis/GlShaderMixin");

    protected GlShaderMixin(int id) {
        super(id);
    }

    /**
     * @author Vulkairis
     * @reason Intercept Iris GLSL shader compilation, run dry-run analysis, and compile to SPIR-V.
     */
    @Overwrite
    private static int createShader(ShaderType type, String name, String source) {
        VULKAIRIS_LOGGER.info("[Vulkairis] Intercepted Iris shader compilation: '{}' (stage: {})", name, type);

        // Dry run analyzer check
        int stage = (type != null) ? type.id : 0;
        AnalysisResult analysis = ShaderAnalyzer.analyzeAndTest(name, source, stage);
        if (!analysis.isSuccess()) {
            DiagnosticManager.log(DiagnosticManager.Category.SHADER,
                    "Shader rejected by dry-run analyzer: " + name, analysis.reason());
            VulkairisController.getInstance().triggerFallback("Unsupported shader feature: " + name, analysis.reason());
        }

        int virtualId;
        try {
            virtualId = SpirvCompiler.compileAndRegister(name, source, type);
        } catch (Throwable t) {
            VULKAIRIS_LOGGER.warn("[Vulkairis] Exception compiling shader '{}': {}. Returning fallback virtual handle.", name, t.getMessage());
            virtualId = VulkanShaderRegistry.registerShader(name, stage, null, source);
        }
        VulkairisController.getInstance().incrementShaderCount();
        return virtualId;
    }

    /**
     * @author Vulkairis
     * @reason Clean up VkShaderModule when Iris destroys a GlShader.
     */
    @Overwrite
    protected void destroyInternal() {
        VulkanShaderRegistry.deleteShader(this.getGlId());
    }
}
