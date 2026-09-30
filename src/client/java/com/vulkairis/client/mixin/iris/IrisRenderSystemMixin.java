package com.vulkairis.client.mixin.iris;

import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.sampler.SamplerLimits;
import net.vulkanmod.gl.VkGlBuffer;
import net.vulkanmod.gl.VkGlFramebuffer;
import net.vulkanmod.gl.VkGlTexture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

@Mixin(value = IrisRenderSystem.class, remap = false)
public class IrisRenderSystemMixin {
    @Unique
    private static final Logger VULKAIRIS_LOGGER = LogManager.getLogger("Vulkairis/IrisRenderSystemMixin");

    @Shadow
    private static IrisRenderSystem.DSAAccess dsaState;
    @Shadow
    private static boolean hasMultibind;
    @Shadow
    private static boolean supportsCompute;
    @Shadow
    private static boolean supportsTesselation;
    @Shadow
    private static int[] samplers;
    @Shadow
    private static int polygonMode;
    @Shadow
    private static int backupPolygonMode;
    @Shadow
    private static boolean cullingState;

    @Unique
    private static final IrisRenderSystem.DSAAccess VULKAN_DSA = new IrisRenderSystem.DSAAccess() {
        @Override
        public void generateMipmaps(int texture, int target) {}
        @Override
        public void texParameteri(int texture, int target, int pname, int param) {}
        @Override
        public void texParameterf(int texture, int target, int pname, float param) {}
        @Override
        public void texParameteriv(int texture, int target, int pname, int[] params) {}
        @Override
        public void readBuffer(int framebuffer, int mode) {}
        @Override
        public void drawBuffers(int framebuffer, int[] buffers) {}
        @Override
        public int getTexParameteri(int texture, int target, int pname) { return 0; }
        @Override
        public void copyTexSubImage2D(int texture, int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {}
        @Override
        public void bindTextureToUnit(int unit, int texture, int target) {}
        @Override
        public int bufferStorage(int target, float[] data, int usage) { return 1; }
        @Override
        public void blitFramebuffer(int src, int dst, int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {}
        @Override
        public void framebufferTexture2D(int framebuffer, int target, int attachment, int textarget, int texture, int level) {}
        @Override
        public int createFramebuffer() { return VkGlFramebuffer.genFramebufferId(); }
        @Override
        public int createTexture(int target) { return VkGlTexture.genTextureId(); }
        @Override
        public int createBuffers() { return VkGlBuffer.glGenBuffers(); }
    };

    @Overwrite
    public static void initRenderer() {
        dsaState = VULKAN_DSA;
        hasMultibind = false;
        supportsCompute = true;
        supportsTesselation = false;
        samplers = new int[SamplerLimits.get().getMaxTextureUnits()];
        com.vulkairis.iris.StateUpdateNotifierManager.ensureInitialized();
        VULKAIRIS_LOGGER.info("[Vulkairis] Initialized IrisRenderSystem under Vulkan backend with complete GL bypass");
    }

    @Overwrite
    public static boolean supportsSSBO() { return true; }

    @Overwrite
    public static boolean supportsImageLoadStore() { return true; }

    @Overwrite
    public static boolean supportsCompute() { return true; }

    @Overwrite
    public static boolean supportsTesselation() { return false; }

    @Overwrite
    public static boolean supportsBufferBlending() { return true; }

    @Overwrite
    public static int getMaxImageUnits() { return 16; }

    @Overwrite
    public static long getVRAM() { return 4294967296L; }

    @Overwrite
    public static void bindBufferBase(int target, Integer index, int buffer) {}

    @Overwrite
    public static void bindBuffer(int target, int buffer) {}

    @Overwrite
    public static void bufferData(int target, float[] data, int usage) {}

    @Overwrite
    public static int bufferStorage(int target, float[] data, int flags) { return 1; }

    @Overwrite
    public static void bufferStorage(int target, long size, int flags) {}

    @Overwrite
    public static int createBuffers() { return VkGlBuffer.glGenBuffers(); }

    @Overwrite
    public static void genBuffers(int[] buffers) {
        if (buffers != null) {
            for (int i = 0; i < buffers.length; i++) {
                buffers[i] = VkGlBuffer.glGenBuffers();
            }
        }
    }

    @Overwrite
    public static void deleteBuffers(int buffer) {
        VkGlBuffer.glDeleteBuffers(buffer);
    }

    @Overwrite
    public static void clearBufferSubData(int target, int internalFormat, long offset, long size, int format, int type, int[] data) {}

    @Overwrite
    public static void getProgramiv(int program, int pname, int[] params) {
        if (params != null && params.length > 0) {
            params[0] = 1;
        }
    }

    @Overwrite
    public static String getProgramInfoLog(int program) { return ""; }

    @Overwrite
    public static String getShaderInfoLog(int shader) { return ""; }

    @Overwrite
    public static int getUniformBlockIndex(int program, String name) { return 0; }

    @Overwrite
    public static void uniformBlockBinding(int program, int blockIndex, int bufferIndex) {}

    @Overwrite
    public static void memoryBarrier(int barriers) {}

    @Overwrite
    public static void dispatchCompute(int numGroupsX, int numGroupsY, int numGroupsZ) {}

    @Overwrite
    public static void dispatchCompute(Vector3i vector3i) {}

    @Overwrite
    public static void dispatchComputeIndirect(long indirect) {}

    @Overwrite
    public static void disableBufferBlend(int index) {}

    @Overwrite
    public static void enableBufferBlend(int index) {}

    @Overwrite
    public static void blendFuncSeparatei(int buf, int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {}

    @Overwrite
    public static int genSampler() { return 1; }

    @Overwrite
    public static void destroySampler(int sampler) {}

    @Overwrite
    public static void bindSamplerToUnit(int unit, int sampler) {}

    @Overwrite
    public static void unbindAllSamplers() {}

    @Overwrite
    public static void samplerParameteri(int sampler, int pname, int param) {}

    @Overwrite
    public static void samplerParameterf(int sampler, int pname, float param) {}

    @Overwrite
    public static void samplerParameteriv(int sampler, int pname, int[] params) {}

    @Overwrite
    public static void vertexAttrib4f(int index, float x, float y, float z, float w) {}

    @Overwrite
    public static void detachShader(int program, int shader) {}

    @Overwrite
    public static void setPolygonMode(int mode) { polygonMode = mode; }

    @Overwrite
    public static void overridePolygonMode() { backupPolygonMode = polygonMode; }

    @Overwrite
    public static void restorePolygonMode() { polygonMode = backupPolygonMode; }

    @Overwrite
    public static void uniform1f(int location, float v0) {}

    @Overwrite
    public static void uniform2f(int location, float v0, float v1) {}

    @Overwrite
    public static void uniform2i(int location, int v0, int v1) {}

    @Overwrite
    public static void uniform3f(int location, float v0, float v1, float v2) {}

    @Overwrite
    public static void uniform3i(int location, int v0, int v1, int v2) {}

    @Overwrite
    public static void uniform4f(int location, float v0, float v1, float v2, float v3) {}

    @Overwrite
    public static void uniform4i(int location, int v0, int v1, int v2, int v3) {}

    @Overwrite
    public static void uniformMatrix4fv(int location, boolean transpose, FloatBuffer matrices) {}

    @Overwrite
    public static void getIntegerv(int pname, int[] params) {
        if (params != null && params.length > 0) {
            params[0] = 0;
        }
    }

    @Overwrite
    public static void getFloatv(int pname, float[] params) {
        if (params != null && params.length > 0) {
            params[0] = 0.0f;
        }
    }

    @Overwrite
    public static void generateMipmaps(int texture, int target) {}

    @Overwrite
    public static void bindTextureForSetup(int target, int texture) {
        VkGlTexture.bindTexture(texture);
    }

    @Overwrite
    public static void bindTextureToUnit(int unit, int texture, int target) {
        if (unit >= 0 && unit < 12) {
            VkGlTexture.activeTexture(33984 + unit);
            VkGlTexture.bindTexture(texture);
        } else {
            VULKAIRIS_LOGGER.debug("[Vulkairis] Virtualizing bindTextureToUnit unit={} texture={}", unit, texture);
        }
    }

    @Overwrite
    public static void bindAttributeLocation(int program, int index, CharSequence name) {}

    @Overwrite
    public static void texImage1D(int texture, int target, int level, int internalformat, int width, int border, int format, int type, ByteBuffer pixels) {}

    @Overwrite
    public static void texImage2D(int texture, int target, int level, int internalformat, int width, int height, int border, int format, int type, ByteBuffer pixels) {}

    @Overwrite
    public static void texImage3D(int texture, int target, int level, int internalformat, int width, int height, int depth, int border, int format, int type, ByteBuffer pixels) {}

    @Overwrite
    public static void copyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border) {}

    @Overwrite
    public static void copyTexSubImage2D(int texture, int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {}

    @Overwrite
    public static void texParameteri(int texture, int target, int pname, int param) {}

    @Overwrite
    public static void texParameterf(int texture, int target, int pname, float param) {}

    @Overwrite
    public static void texParameteriv(int texture, int target, int pname, int[] params) {}

    @Overwrite
    public static void texParameterivDirect(int target, int pname, int[] params) {}

    @Overwrite
    public static void drawBuffers(int framebuffer, int[] buffers) {}

    @Overwrite
    public static void readBuffer(int framebuffer, int mode) {}

    @Overwrite
    public static String getActiveUniform(int program, int index, int maxLength, IntBuffer length, IntBuffer size) { return ""; }

    @Overwrite
    public static void readPixels(int x, int y, int width, int height, int format, int type, float[] pixels) {}

    @Overwrite
    public static void framebufferTexture2D(int framebuffer, int target, int attachment, int textarget, int texture, int level) {}

    @Overwrite
    public static int getTexParameteri(int texture, int target, int pname) { return 0; }

    @Overwrite
    public static void bindImageTexture(int unit, int texture, int level, boolean layered, int layer, int access, int format) {}

    @Overwrite
    public static int createFramebuffer() { return VkGlFramebuffer.genFramebufferId(); }

    @Overwrite
    public static int createTexture(int target) { return VkGlTexture.genTextureId(); }

    @Overwrite
    public static void backupAndDisableCullingState(boolean culling) { cullingState = culling; }

    @Overwrite
    public static void restoreCullingState() {}

    @Overwrite
    public static void setShadowProjection(Matrix4f projection) {}

    @Overwrite
    public static void restorePlayerProjection() {}

    @Overwrite
    public static void blitFramebuffer(int src, int dst, int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {}
}
