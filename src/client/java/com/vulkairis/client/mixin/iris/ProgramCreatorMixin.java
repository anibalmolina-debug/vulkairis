package com.vulkairis.client.mixin.iris;

import com.vulkairis.bridge.VulkanModProgramBridge;
import com.vulkairis.config.ConfigManager;
import com.vulkairis.core.VulkairisController;
import com.vulkairis.diagnostics.DiagnosticManager;
import com.vulkairis.translation.GlObjectState;
import com.vulkairis.translation.UnifiedTranslationService;
import com.vulkairis.translation.VulkanProgramLinker;
import com.vulkairis.translation.VulkanShaderRegistry;
import com.vulkairis.translation.VulkairisProgram;
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
     * @reason Intercept Iris program creation and link a real Vulkan pipeline into VulkanMod's
     * registry so that {@code glUseProgram} on the virtual program id binds an actual pipeline
     * instead of doing nothing.
     *
     * <p>Phase 5 contract (pinned target: Iris 1.8.14 {@code ProgramCreator.create}, which this
     * method fully replaces): the program is first <em>validated</em> by
     * {@link VulkanProgramLinker} — stage set, interface types, descriptor agreement. A failed
     * link records {@code FAILED} with the precise problems in the program info log and returns
     * the id <em>without</em> attempting pipeline construction, so status queries report failure
     * instead of simulated success. Only a validated program reaches
     * {@code VulkanModProgramBridge.link}, and a bridge failure marks it failed as well.
     * Compatibility risk: if Iris changes {@code create}'s signature this overwrite fails loudly
     * at mixin apply time (required overwrite); fallback is the contained failed-link state.</p>
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
        UnifiedTranslationService.translateProgram(name, shaderIds);
        int progId = VulkanShaderRegistry.registerProgram(name, shaderIds);

        VulkairisProgram program = VulkanShaderRegistry.getProgram(progId);
        VulkanProgramLinker.LinkResult linkResult = (program != null)
                ? VulkanProgramLinker.validate(program)
                : new VulkanProgramLinker.LinkResult(false,
                        List.of("program object missing for '" + name + "'"), List.of());
        if (!linkResult.valid()) {
            VulkanShaderRegistry.setProgramLinkState(progId, GlObjectState.FAILED);
            VulkanShaderRegistry.setProgramInfoLog(progId, linkResult.log());
            VulkairisController.getInstance().setLastLinkError(name + ": " + linkResult.log());
            DiagnosticManager.logStructured(DiagnosticManager.Category.PIPELINE,
                    DiagnosticManager.Phase.LINK,
                    "Program link failed", linkResult.log(), null,
                    name, null, null, -1, null,
                    "Fix the named stage interface or disable the program; no pipeline was built");
            VULKAIRIS_LOGGER.warn("[Vulkairis] Program '{}' link FAILED: {}. Returning id without a pipeline.",
                    name, linkResult.log());
            return progId;
        }
        VulkanShaderRegistry.setProgramLinkState(progId, GlObjectState.LINKED);
        VulkanShaderRegistry.setProgramInfoLog(progId, "");
        for (String warning : linkResult.warnings()) {
            DiagnosticManager.logStructured(DiagnosticManager.Category.PIPELINE,
                    DiagnosticManager.Phase.LINK,
                    "Program link warning", warning, null,
                    name, null, null, -1, null,
                    "Descriptor numbers differ per stage; reconciled layout lands with Phase 10/11");
        }

        if (ConfigManager.getConfig().isIrisPipelineLink()) {
            boolean linked = (program != null) && VulkanModProgramBridge.link(program);
            if (!linked) {
                VulkanShaderRegistry.setProgramLinkState(progId, GlObjectState.FAILED);
                String reason = "Vulkan pipeline construction failed for '" + name + "'";
                VulkanShaderRegistry.setProgramInfoLog(progId, reason);
                VulkairisController.getInstance().setLastLinkError(name + ": " + reason);
                DiagnosticManager.logStructured(DiagnosticManager.Category.PIPELINE,
                        DiagnosticManager.Phase.PIPELINE,
                        "Vulkan pipeline construction failed", reason, null,
                        name, null, null, -1, null,
                        "The program stays unbound; check device availability and link diagnostics");
                VULKAIRIS_LOGGER.warn("[Vulkairis] {}", reason);
                return progId;
            }
            VulkairisController.getInstance().incrementPipelineCount();
        }
        return progId;
    }
}
