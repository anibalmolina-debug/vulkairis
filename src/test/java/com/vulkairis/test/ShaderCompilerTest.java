package com.vulkairis.test;

import com.vulkairis.translation.ShaderTranslator;
import com.vulkairis.translation.SpirvCompiler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.shaderc.Shaderc;

import java.nio.ByteBuffer;

public class ShaderCompilerTest {

    @Test
    public void testInProcessShadercCompilation() {
        long compiler = Shaderc.shaderc_compiler_initialize();
        Assertions.assertNotEquals(0L, compiler, "Shaderc compiler should initialize");

        long options = Shaderc.shaderc_compile_options_initialize();
        Assertions.assertNotEquals(0L, options, "Shaderc options should initialize");

        Shaderc.shaderc_compile_options_set_target_env(
                options,
                Shaderc.shaderc_target_env_vulkan,
                Shaderc.shaderc_env_version_vulkan_1_1
        );
        Shaderc.shaderc_compile_options_set_invert_y(options, true);

        String vertSource = """
                #version 450 core
                layout (location = 0) in vec3 Position;
                layout (location = 1) in vec4 Color;
                layout (location = 0) out vec4 vertexColor;
                void main() {
                    gl_Position = vec4(Position, 1.0);
                    vertexColor = Color;
                }
                """;

        long result = Shaderc.shaderc_compile_into_spv(
                compiler,
                vertSource,
                Shaderc.shaderc_glsl_vertex_shader,
                "test_vert.vsh",
                "main",
                options
        );

        Assertions.assertNotEquals(0L, result, "Compilation result pointer should not be 0");

        int status = Shaderc.shaderc_result_get_compilation_status(result);
        if (status != Shaderc.shaderc_compilation_status_success) {
            String errorMsg = Shaderc.shaderc_result_get_error_message(result);
            Shaderc.shaderc_result_release(result);
            Assertions.fail("GLSL -> SPIR-V compilation failed: " + errorMsg);
        }

        ByteBuffer spv = Shaderc.shaderc_result_get_bytes(result);
        Assertions.assertNotNull(spv, "SPIR-V bytes should not be null");
        Assertions.assertTrue(spv.remaining() > 0, "SPIR-V bytecode size must be > 0");

        // Verify SPIR-V magic number (0x07230203) in little endian
        int magic = spv.asIntBuffer().get(0);
        Assertions.assertEquals(0x07230203, magic, "SPIR-V magic number must match 0x07230203");

        System.out.println("[Test] Successfully compiled GLSL to SPIR-V, size: " + spv.remaining() + " bytes, magic: 0x" + Integer.toHexString(magic));

        Shaderc.shaderc_result_release(result);
        Shaderc.shaderc_compile_options_release(options);
        Shaderc.shaderc_compiler_release(compiler);
    }

    @Test
    public void testCenterDepthCompilation() {
        String vertSource = """
                #version 150 core

                in vec3 iris_Position;
                uniform mat4 projection;

                void main() {
                    gl_Position = projection * vec4(iris_Position, 1.0);
                }
                """;

        String preprocessed = ShaderTranslator.preprocessGlsl(vertSource, true);
        System.out.println("Preprocessed shader:\n" + preprocessed);

        ByteBuffer spv = SpirvCompiler.compileGlslToSpirv("centerDepth.vsh", preprocessed, Shaderc.shaderc_glsl_vertex_shader);
        Assertions.assertNotNull(spv);
        Assertions.assertTrue(spv.remaining() > 0);
        System.out.println("[Test] centerDepth.vsh successfully compiled to SPIR-V: " + spv.remaining() + " bytes");
    }
}
