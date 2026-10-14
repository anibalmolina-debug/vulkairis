package com.vulkairis.client.mixin.iris;

import com.vulkairis.bridge.VulkanModProgramBridge;
import com.vulkairis.core.VulkairisController;
import com.vulkairis.diagnostics.StageDiagnostics;
import com.vulkairis.iris.IrisBridge;
import com.vulkairis.vulkan.pipeline.TerrainPipelineDoor;
import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.pipeline.programs.SodiumPrograms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Phase D terrain door around the frozen {@code SodiumPrograms} path.
 *
 * <p>Semantics (narrow wraps only, no {@code @Overwrite} here): the frozen
 * {@code SodiumProgramsMixin} signatures stay untouched. This mixin adds
 * {@code @Inject}-based behavior around them:</p>
 *
 * <ul>
 *   <li>{@code getProgram} HEAD (cancellable): resolves the handed pass through
 *       {@code TerrainPipelineDoor} and {@code VulkanModProgramBridge}. When a pack
 *       pipeline is linked, the decision is recorded (pack pass/target) and the GL path
 *       is left to return {@code null} (Vulkan owns drawing via the bridge). When no
 *       pipeline is linked and a pack is active, an exact fallback reason is recorded
 *       instead of silently drawing vanilla.</li>
 *   <li>{@code getFramebuffer} RETURN: swaps the handed pass label for the pack target
 *       label on the controller so diagnostics show the effective pack target.</li>
 * </ul>
 *
 * <p>Shadow draw stays one frame late by design (see {@code ShadowFrameLag}): nothing here
 * attempts same-frame shadow publication.</p>
 */
@Mixin(value = SodiumPrograms.class, remap = false)
public abstract class SodiumTerrainDoorMixin {

    @Unique
    private static volatile String vulkairis$lastDoorReason = "";

    /**
     * Head-of-compile terrain decision. Cancellable so a future pack-owned path can return
     * early; today it records the decision and lets the (null-returning) GL path proceed,
     * with the real draw owned by the Vulkan bridge.
     */
    @Inject(method = "getProgram", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void vulkairis$terrainCompileHead(TerrainRenderPass pass,
                                              CallbackInfoReturnable<GlProgram<ChunkShaderInterface>> cir) {
        try {
            String irisName = pass == null ? "TERRAIN" : pass.toString();
            com.vulkairis.rendering.backend.vulkanmod.v1211.VulkanModRenderStage stage = pass != null && pass.isTranslucent()
                    ? com.vulkairis.rendering.backend.vulkanmod.v1211.VulkanModRenderStage.TRANSLUCENT_TERRAIN
                    : com.vulkairis.rendering.backend.vulkanmod.v1211.VulkanModRenderStage.OPAQUE_TERRAIN;
            com.vulkairis.rendering.backend.vulkanmod.v1211.VulkanModLifecycleAdapter.getInstance()
                    .onStageObserved(stage, "SodiumPrograms.getProgram: " + irisName);

            TerrainPipelineDoor.TerrainPass doorPass =
                    TerrainPipelineDoor.forPassName(simplePassName(pass));
            String program = TerrainPipelineDoor.programNameFor(doorPass);
            int linked = VulkanModProgramBridge.terrainLinkedId(program);
            TerrainPipelineDoor.DoorDecision decision =
                    TerrainPipelineDoor.decideCompile(doorPass, linked);

            VulkairisController controller = VulkairisController.getInstance();
            controller.setActivePass(decision.passLabel());
            controller.setCurrentTarget(decision.targetLabel());
            vulkairis$lastDoorReason = decision.fallbackReason();

            if (decision.usePackPipeline()) {
                StageDiagnostics.pass("Terrain door",
                        "pack program '" + program + "' bound for pass " + irisName);
                // The Vulkan bridge owns the draw; the GL program stays null (frozen overwrite).
                return;
            }
            if (isPackActive()) {
                StageDiagnostics.warn("Terrain door", decision.fallbackReason());
                controller.setLastLinkError("terrain:" + program + ": " + decision.fallbackReason());
            }
        } catch (Throwable t) {
            StageDiagnostics.warn("Terrain door",
                    "door decision failed: " + (t.getMessage() != null ? t.getMessage() : t.toString()));
        }
    }

    /** Render-path label swap: report the effective pack target for the handed pass. */
    @Inject(method = "getFramebuffer", at = @At("RETURN"), remap = false, require = 0)
    private void vulkairis$terrainFramebufferReturn(TerrainRenderPass pass,
                                                    CallbackInfoReturnable<GlFramebuffer> cir) {
        try {
            TerrainPipelineDoor.TerrainPass doorPass =
                    TerrainPipelineDoor.forPassName(simplePassName(pass));
            VulkairisController.getInstance()
                    .setCurrentTarget(TerrainPipelineDoor.targetFor(doorPass));
        } catch (Throwable ignored) {
        }
    }

    @Unique
    private static String simplePassName(TerrainRenderPass pass) {
        if (pass == null) {
            return "TERRAIN";
        }
        // TerrainRenderPass has no stable name accessor across Sodium builds; map by behavior.
        try {
            if (pass.isTranslucent()) {
                return "TRANSLUCENT";
            }
        } catch (Throwable ignored) {
        }
        return "TERRAIN";
    }

    @Unique
    private static boolean isPackActive() {
        try {
            String pack = IrisBridge.getCurrentPackName();
            return pack != null && !pack.isBlank()
                    && !pack.equals("None") && !pack.startsWith("None (");
        } catch (Throwable t) {
            return false;
        }
    }
}
