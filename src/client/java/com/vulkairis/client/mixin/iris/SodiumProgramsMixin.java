package com.vulkairis.client.mixin.iris;

import com.google.common.collect.ImmutableSet;
import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import net.caffeinemc.mods.sodium.client.gl.shader.GlShader;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.SodiumPrograms;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

@Mixin(value = SodiumPrograms.class, remap = false)
public class SodiumProgramsMixin {

    /**
     * @author Vulkairis
     * @reason Sodium terrain OpenGL shaders are not used under VulkanMod; stub to avoid NoSuchMethodError on GlShader constructor.
     */
    @Overwrite
    private Map<PatchShaderType, GlShader> createGlShaders(String name, Map<PatchShaderType, String> transformed) {
        return Collections.emptyMap();
    }

    /**
     * @author Vulkairis
     * @reason Sodium terrain OpenGL shaders are not used under VulkanMod; stub to avoid creating OpenGL GlProgram.
     */
    @Overwrite
    private GlProgram<ChunkShaderInterface> createShader(IrisRenderingPipeline pipeline,
                                                         SodiumPrograms.Pass pass,
                                                         ProgramSource source,
                                                         AlphaTest alphaTest,
                                                         CustomUniforms customUniforms,
                                                         Supplier<ImmutableSet<Integer>> flipState,
                                                         Map<PatchShaderType, GlShader> glShaders) {
        return null;
    }
}
