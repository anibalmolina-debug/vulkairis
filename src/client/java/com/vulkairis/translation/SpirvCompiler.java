package com.vulkairis.translation;

import net.irisshaders.iris.gl.shader.ShaderType;
import org.lwjgl.util.shaderc.Shaderc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * In-process GLSL to SPIR-V compiler using LWJGL Shaderc bindings.
 * Automatically configures Vulkan environment, Y-axis inversion, and produces SPIR-V byte buffers.
 */
public class SpirvCompiler {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vulkairis/Compiler");

    public static VulkairisShader compileShader(String name, String glslSource, ShaderType shaderType) {
        int stage = (shaderType != null) ? shaderType.id : 0;
        int shadercKind = mapShaderTypeToShaderc(shaderType);
        return compileShaderInternal(name, glslSource, stage, shadercKind);
    }

    public static VulkairisShader compileShader(String name, String glslSource, int stage) {
        int shadercKind = mapStageIntToShaderc(stage);
        return compileShaderInternal(name, glslSource, stage, shadercKind);
    }

    public static int compileAndRegister(String name, String glslSource, ShaderType shaderType) {
        return compileShader(name, glslSource, shaderType).getVirtualId();
    }

    public static int compileAndRegister(String name, String glslSource, int stage) {
        return compileShader(name, glslSource, stage).getVirtualId();
    }

    public static ByteBuffer compileGlslToSpirv(String name, String glslSource, int shadercKind) {
        long compiler = Shaderc.shaderc_compiler_initialize();
        if (compiler == 0L) {
            throw new RuntimeException("Failed to initialize Shaderc compiler");
        }

        long options = Shaderc.shaderc_compile_options_initialize();
        if (options == 0L) {
            Shaderc.shaderc_compiler_release(compiler);
            throw new RuntimeException("Failed to initialize Shaderc compile options");
        }

        try {
            Shaderc.shaderc_compile_options_set_target_env(
                    options,
                    Shaderc.shaderc_target_env_vulkan,
                    Shaderc.shaderc_env_version_vulkan_1_1
            );
            Shaderc.shaderc_compile_options_set_invert_y(options, true);
            Shaderc.shaderc_compile_options_set_auto_map_locations(options, true);
            Shaderc.shaderc_compile_options_set_auto_bind_uniforms(options, true);
            Shaderc.shaderc_compile_options_set_auto_combined_image_sampler(options, true);

            long result = Shaderc.shaderc_compile_into_spv(
                    compiler,
                    glslSource,
                    shadercKind,
                    name != null ? name : "shader",
                    "main",
                    options
            );

            if (result == 0L) {
                throw new RuntimeException("Shaderc returned null result pointer for: " + name);
            }

            try {
                int status = Shaderc.shaderc_result_get_compilation_status(result);
                if (status != Shaderc.shaderc_compilation_status_success) {
                    String errorLog = Shaderc.shaderc_result_get_error_message(result);
                    LOGGER.error("[Vulkairis] GLSL -> SPIR-V compilation failed for '{}':\n{}", name, errorLog);
                    throw new RuntimeException("GLSL compilation failed for '" + name + "': " + errorLog);
                }

                ByteBuffer spirvBytes = Shaderc.shaderc_result_get_bytes(result);
                if (spirvBytes == null || !spirvBytes.hasRemaining()) {
                    throw new RuntimeException("Empty SPIR-V bytecode produced for: " + name);
                }

                ByteBuffer directCopy = ByteBuffer.allocateDirect(spirvBytes.remaining());
                directCopy.put(spirvBytes);
                directCopy.flip();

                SpirvValidator.validate(directCopy);
                return directCopy;
            } finally {
                Shaderc.shaderc_result_release(result);
            }
        } finally {
            Shaderc.shaderc_compile_options_release(options);
            Shaderc.shaderc_compiler_release(compiler);
        }
    }

    private static VulkairisShader compileShaderInternal(String name, String glslSource, int stage, int shadercKind) {
        LOGGER.info("[Vulkairis] Compiling shader '{}' (stage: {}, kind: {})...", name, stage, shadercKind);
        ByteBuffer spirv = null;
        try {
            boolean isVertex = (shadercKind == Shaderc.shaderc_glsl_vertex_shader);
            String preprocessed = ShaderTranslator.preprocessGlsl(glslSource, isVertex);
            spirv = compileGlslToSpirv(name, preprocessed, shadercKind);
        } catch (Throwable e) {
            LOGGER.warn("[Vulkairis] GLSL -> SPIR-V compilation failed for '{}': {}. Falling back to virtual handle.", name, e.getMessage());
        }
        int virtualId = VulkanShaderRegistry.registerShader(name, stage, spirv, glslSource);
        return VulkanShaderRegistry.getShader(virtualId);
    }

    public static int mapShaderTypeToShaderc(ShaderType type) {
        if (type == null) return Shaderc.shaderc_glsl_infer_from_source;
        return switch (type) {
            case VERTEX -> Shaderc.shaderc_glsl_vertex_shader;
            case FRAGMENT -> Shaderc.shaderc_glsl_fragment_shader;
            case GEOMETRY -> Shaderc.shaderc_glsl_geometry_shader;
            case COMPUTE -> Shaderc.shaderc_glsl_compute_shader;
            case TESSELATION_CONTROL -> Shaderc.shaderc_glsl_tess_control_shader;
            case TESSELATION_EVAL -> Shaderc.shaderc_glsl_tess_evaluation_shader;
        };
    }

    public static int mapStageIntToShaderc(int stage) {
        return switch (stage) {
            case 35633 -> Shaderc.shaderc_glsl_vertex_shader;
            case 35632 -> Shaderc.shaderc_glsl_fragment_shader;
            case 36313 -> Shaderc.shaderc_glsl_geometry_shader;
            case 37305 -> Shaderc.shaderc_glsl_compute_shader;
            default -> Shaderc.shaderc_glsl_infer_from_source;
        };
    }
}
