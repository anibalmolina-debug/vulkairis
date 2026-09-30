package com.vulkairis.client.mixin.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vulkairis.resource.VulkairisResourceManager;
import com.vulkairis.vulkan.VulkanCapabilities;
import com.vulkairis.translation.VulkanShaderRegistry;
import net.vulkanmod.gl.VkGlFramebuffer;
import net.vulkanmod.gl.VkGlRenderbuffer;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.vulkan.VRenderSystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(GlStateManager.class)
public class GlStateManagerMixin {
    @Unique
    private static final Logger VULKAIRIS_LOGGER = LogManager.getLogger("Vulkairis/GlStateManagerMixin");

    @Unique
    private static final Map<String, Integer> UNIFORM_LOCATIONS = new ConcurrentHashMap<>();
    @Unique
    private static final AtomicInteger UNIFORM_ID_GEN = new AtomicInteger(100);

    static {
        com.vulkairis.iris.StateUpdateNotifierManager.ensureInitialized();
    }

    /**
     * @author Vulkairis
     * @reason Redirect glActiveTexture to VulkanMod's VkGlTexture when within the supported 0-11 unit range,
     * virtualizing units >= 12 to avoid ArrayIndexOutOfBoundsException or out-of-range error logs.
     */
    @Overwrite
    public static void glActiveTexture(int texture) {
        int unit = texture - 33984; // GL_TEXTURE0 is 33984
        if (unit >= 0 && unit < 12) {
            VkGlTexture.activeTexture(texture);
        } else {
            VULKAIRIS_LOGGER.debug("[Vulkairis] Virtualizing glActiveTexture({}) unit {} outside VulkanMod [0, 11] range", texture, unit);
        }
    }

    /**
     * @author Vulkairis
     * @reason Redirect glBlendFuncSeparate to VulkanMod's VRenderSystem to prevent raw GL14 access violation.
     */
    @Overwrite
    public static void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        VRenderSystem.blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }

    /**
     * @author Vulkairis
     * @reason Intercept _getString to safely report Vulkan info without invoking raw GL11.glGetString.
     */
    @Overwrite
    public static String _getString(int name) {
        String deviceName = "Vulkan Device";
        try {
            deviceName = VulkanCapabilities.getActiveCapabilities().getDeviceName();
        } catch (Throwable ignored) {}

        return switch (name) {
            case 7936 -> "VulkanMod"; // GL_VENDOR
            case 7937 -> deviceName;  // GL_RENDERER
            case 7938 -> "4.6.0 VulkanMod"; // GL_VERSION
            case 35724 -> "4.60 VulkanMod"; // GL_SHADING_LANGUAGE_VERSION
            case 7939 -> ""; // GL_EXTENSIONS
            default -> "Vulkan";
        };
    }

    /**
     * @author Vulkairis
     * @reason Intercept _getInteger to return safe device capabilities without calling raw GL11.glGetInteger.
     */
    @Overwrite
    public static int _getInteger(int name) {
        return switch (name) {
            case 33309 -> 0;     // GL_NUM_EXTENSIONS
            case 34047 -> 32;    // GL_MAX_TEXTURE_IMAGE_UNITS
            case 34930 -> 32;    // GL_MAX_TEXTURE_COORDS
            case 34921 -> 16;    // GL_MAX_VERTEX_ATTRIBS
            case 3379 -> 16384;  // GL_MAX_TEXTURE_SIZE
            case 34045 -> 16384; // GL_MAX_CUBE_MAP_TEXTURE_SIZE
            case 34852 -> 16;    // GL_MAX_DRAW_BUFFERS
            case 36063 -> 16;    // GL_MAX_COLOR_ATTACHMENTS
            case 34024 -> 16384; // GL_MAX_RENDERBUFFER_SIZE
            case 3386 -> 16384;  // GL_MAX_VIEWPORT_DIMS
            case 35660 -> 128;   // GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS
            case 35661 -> 32;    // GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS
            case 35374 -> 36;    // GL_MAX_UNIFORM_BUFFER_BINDINGS
            case 35375 -> 65536; // GL_MAX_UNIFORM_BLOCK_SIZE
            case 37085 -> 16;    // GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS
            case 37086 -> 134217728; // GL_MAX_SHADER_STORAGE_BLOCK_SIZE
            case 37087 -> 16;    // GL_MAX_COMPUTE_SHADER_STORAGE_BLOCKS
            case 37088 -> 64;    // GL_MAX_COMBINED_SHADER_STORAGE_BLOCKS
            case 37099 -> 1024;  // GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS
            case 36664 -> 16;    // GL_MAX_IMAGE_UNITS
            case 32873 -> 0;     // GL_TEXTURE_BINDING_2D
            default -> 0;
        };
    }

    /**
     * @author Vulkairis
     * @reason Return GL_NO_ERROR (0) without calling raw GL11.glGetError.
     */
    @Overwrite
    public static int _getError() {
        return 0;
    }

    /**
     * @author Vulkairis
     * @reason Intercept _genTextures to allocate virtual texture IDs via VulkanMod.
     */
    @Overwrite
    public static void _genTextures(int[] textures) {
        if (textures != null) {
            for (int i = 0; i < textures.length; i++) {
                textures[i] = VkGlTexture.genTextureId();
            }
        }
    }

    /**
     * @author Vulkairis
     * @reason Intercept _deleteTextures to clean up virtual texture IDs via VulkanMod.
     */
    @Overwrite
    public static void _deleteTextures(int[] textures) {
        if (textures != null) {
            for (int id : textures) {
                VkGlTexture.glDeleteTextures(id);
            }
        }
    }

    /**
     * @author Vulkairis
     * @reason Intercept _glDeleteFramebuffers to clean up virtual framebuffer via VulkanMod.
     */
    @Overwrite
    public static void _glDeleteFramebuffers(int framebuffer) {
        VkGlFramebuffer.deleteFramebuffer(framebuffer);
    }

    /**
     * @author Vulkairis
     * @reason Intercept _glDeleteRenderbuffers to clean up virtual renderbuffer via VulkanMod.
     */
    @Overwrite
    public static void _glDeleteRenderbuffers(int renderbuffer) {
        VkGlRenderbuffer.deleteRenderbuffer(renderbuffer);
    }

    /**
     * @author Vulkairis
     * @reason Intercept _glBlitFrameBuffer to execute via VulkanMod.
     */
    @Overwrite
    public static void _glBlitFrameBuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        VkGlFramebuffer.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    /**
     * @author Vulkairis
     * @reason Intercept glCheckFramebufferStatus to always return GL_FRAMEBUFFER_COMPLETE (36053).
     */
    @Overwrite
    public static int glCheckFramebufferStatus(int target) {
        return 36053; // GL_FRAMEBUFFER_COMPLETE
    }

    /**
     * @author Vulkairis
     * @reason Intercept getBoundFramebuffer to retrieve VulkanMod bound framebuffer ID.
     */
    @Overwrite
    public static int getBoundFramebuffer() {
        VkGlFramebuffer bound = VkGlFramebuffer.getBoundFramebuffer();
        return bound != null ? bound.id : 0;
    }

    /**
     * @author Vulkairis
     * @reason Check if the shader program actually declares the uniform; return -1 if not declared to avoid exhausting sampler/image units.
     */
    @Overwrite
    public static int _glGetUniformLocation(int program, CharSequence name) {
        if (name == null) return -1;
        String uniformName = name.toString();

        com.vulkairis.translation.VulkairisProgram prog = VulkanShaderRegistry.getProgram(program);
        if (prog != null && prog.attachedShaderIds() != null && !prog.attachedShaderIds().isEmpty()) {
            String baseName = uniformName;
            int bracket = uniformName.indexOf('[');
            if (bracket > 0) {
                baseName = uniformName.substring(0, bracket);
            }

            boolean declared = false;
            for (int sId : prog.attachedShaderIds()) {
                com.vulkairis.translation.VulkairisShader shader = VulkanShaderRegistry.getShader(sId);
                if (shader != null && shader.glslSource() != null) {
                    if (containsWord(shader.glslSource(), baseName)) {
                        declared = true;
                        break;
                    }
                }
            }

            if (!declared) {
                return -1;
            }
        }

        String key = program + ":" + uniformName;
        return UNIFORM_LOCATIONS.computeIfAbsent(key, k -> UNIFORM_ID_GEN.incrementAndGet());
    }

    @Unique
    private static boolean containsWord(String source, String word) {
        if (source == null || word == null || word.isEmpty()) return false;
        int wordLen = word.length();
        int srcLen = source.length();
        int index = 0;

        while ((index = source.indexOf(word, index)) != -1) {
            boolean startBoundary = (index == 0) || !Character.isJavaIdentifierPart(source.charAt(index - 1));
            int after = index + wordLen;
            boolean endBoundary = (after >= srcLen) || !Character.isJavaIdentifierPart(source.charAt(after));

            if (startBoundary && endBoundary) {
                return true;
            }
            index += wordLen;
        }
        return false;
    }

    /**
     * @author Vulkairis
     * @reason Prevent raw OpenGL glGetAttribLocation call; allocate virtual attribute locations.
     */
    @Overwrite
    public static int _glGetAttribLocation(int program, CharSequence name) {
        if (name == null) return -1;
        String n = name.toString().toLowerCase();
        if (n.contains("position")) return 0;
        if (n.contains("color")) return 1;
        if (n.contains("uv") || n.contains("texcoord")) return 2;
        if (n.contains("normal")) return 3;
        return UNIFORM_LOCATIONS.computeIfAbsent(program + ":attrib:" + name, k -> UNIFORM_ID_GEN.incrementAndGet());
    }

    /**
     * @author Vulkairis
     * @reason Safe no-op under Vulkan; layout locations are defined statically in SPIR-V.
     */
    @Overwrite
    public static void _glBindAttribLocation(int program, int index, CharSequence name) {}

    /**
     * @author Vulkairis
     * @reason Return safe program status values without calling raw OpenGL glGetProgrami.
     */
    @Overwrite
    public static int glGetProgrami(int program, int pname) {
        return switch (pname) {
            case 35714 -> 1;  // GL_LINK_STATUS -> GL_TRUE
            case 35716 -> 0;  // GL_INFO_LOG_LENGTH -> 0
            case 35717 -> 2;  // GL_ATTACHED_SHADERS -> 2
            case 35718 -> 0;  // GL_ACTIVE_UNIFORMS -> 0
            case 35721 -> 8;  // GL_ACTIVE_ATTRIBUTES -> 8
            default -> 1;
        };
    }

    /**
     * @author Vulkairis
     * @reason Safe no-op under Vulkan; pipelines are linked through VulkanShaderRegistry.
     */
    @Overwrite
    public static void glLinkProgram(int program) {}

    /**
     * @author Vulkairis
     * @reason Return empty log without raw OpenGL calls.
     */
    @Overwrite
    public static String glGetProgramInfoLog(int program, int maxLength) {
        return "";
    }

    /**
     * @author Vulkairis
     * @reason Redirect glDeleteProgram to VulkanShaderRegistry and VulkairisResourceManager instead of raw GL20.glDeleteProgram.
     */
    @Overwrite
    public static void glDeleteProgram(int program) {
        VULKAIRIS_LOGGER.debug("[Vulkairis] GlStateManager.glDeleteProgram({}) redirected to Vulkan registry", program);
        VulkanShaderRegistry.deleteProgram(program);
        VulkairisResourceManager.getInstance().releaseResource(program);
    }

    /**
     * @author Vulkairis
     * @reason Redirect glDeleteShader to VulkanShaderRegistry and VulkairisResourceManager instead of raw GL20.glDeleteShader.
     */
    @Overwrite
    public static void glDeleteShader(int shader) {
        VULKAIRIS_LOGGER.debug("[Vulkairis] GlStateManager.glDeleteShader({}) redirected to Vulkan registry", shader);
        VulkanShaderRegistry.deleteShader(shader);
        VulkairisResourceManager.getInstance().releaseResource(shader);
    }

    @Overwrite
    public static void _glUniform1i(int location, int value) {}

    @Overwrite
    public static void _glUniform1(int location, IntBuffer value) {}

    @Overwrite
    public static void _glUniform1(int location, FloatBuffer value) {}

    @Overwrite
    public static void _glUniform2(int location, IntBuffer value) {}

    @Overwrite
    public static void _glUniform2(int location, FloatBuffer value) {}

    @Overwrite
    public static void _glUniform3(int location, IntBuffer value) {}

    @Overwrite
    public static void _glUniform3(int location, FloatBuffer value) {}

    @Overwrite
    public static void _glUniform4(int location, IntBuffer value) {}

    @Overwrite
    public static void _glUniform4(int location, FloatBuffer value) {}

    @Overwrite
    public static void _glUniformMatrix2(int location, boolean transpose, FloatBuffer value) {}

    @Overwrite
    public static void _glUniformMatrix3(int location, boolean transpose, FloatBuffer value) {}

    @Overwrite
    public static void _glUniformMatrix4(int location, boolean transpose, FloatBuffer value) {}

    @Overwrite
    public static void _vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {}

    @Overwrite
    public static void _vertexAttribIPointer(int index, int size, int type, int stride, long pointer) {}

    @Overwrite
    public static void _enableVertexAttribArray(int index) {}
}
