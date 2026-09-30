package com.vulkairis.translation;

import com.vulkairis.vulkan.VulkanDevice;
import net.irisshaders.iris.gl.shader.ShaderType;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;

import java.nio.ByteBuffer;

/**
 * First-class encapsulation of a compiled shader module in the Vulkairis bridge.
 * Retains Vulkan VkShaderModule handle, raw SPIR-V bytecode, GLSL source, and reflection metadata.
 */
public class VulkairisShader {
    private final int virtualId;
    private final String name;
    private final int stage;
    private long vkShaderModule;
    private final ByteBuffer spirv;
    private final String glslSource;
    private final String entryPoint;
    private final ShaderReflection reflection;

    public VulkairisShader(int virtualId, String name, int stage, long vkShaderModule,
                           ByteBuffer spirv, String glslSource, String entryPoint,
                           ShaderReflection reflection) {
        this.virtualId = virtualId;
        this.name = name != null ? name : "unnamed_shader";
        this.stage = stage;
        this.vkShaderModule = vkShaderModule;
        this.spirv = spirv;
        this.glslSource = glslSource;
        this.entryPoint = (entryPoint != null && !entryPoint.isEmpty()) ? entryPoint : "main";
        this.reflection = reflection != null ? reflection : new ShaderReflection();
    }

    public VulkairisShader(int virtualId, String name, int stage, long vkShaderModule,
                           ByteBuffer spirv, String glslSource) {
        this(virtualId, name, stage, vkShaderModule, spirv, glslSource, "main", new ShaderReflection());
    }

    public int getVirtualId() {
        return virtualId;
    }

    public int virtualId() {
        return virtualId;
    }

    public String getName() {
        return name;
    }

    public String name() {
        return name;
    }

    public int getStage() {
        return stage;
    }

    public int stage() {
        return stage;
    }

    public long getVkShaderModule() {
        return vkShaderModule;
    }

    public long vkShaderModule() {
        return vkShaderModule;
    }

    public ByteBuffer getSpirv() {
        return spirv;
    }

    public ByteBuffer spirv() {
        return spirv;
    }

    public String getGlslSource() {
        return glslSource;
    }

    public String glslSource() {
        return glslSource;
    }

    public String getEntryPoint() {
        return entryPoint;
    }

    public ShaderReflection getReflection() {
        return reflection;
    }

    public boolean isValid() {
        return vkShaderModule != 0L;
    }

    public boolean hasSpirv() {
        return spirv != null && spirv.hasRemaining();
    }

    public int getVkStageFlag() {
        // GL constants vs Vulkan stage flags
        return switch (stage) {
            case 35633 -> VK10.VK_SHADER_STAGE_VERTEX_BIT;   // GL_VERTEX_SHADER
            case 35632 -> VK10.VK_SHADER_STAGE_FRAGMENT_BIT; // GL_FRAGMENT_SHADER
            case 36313 -> VK10.VK_SHADER_STAGE_GEOMETRY_BIT; // GL_GEOMETRY_SHADER
            case 37305 -> VK10.VK_SHADER_STAGE_COMPUTE_BIT;  // GL_COMPUTE_SHADER
            case 1 -> VK10.VK_SHADER_STAGE_VERTEX_BIT;       // internal stage index
            case 2 -> VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
            default -> {
                if (name.contains("vert")) yield VK10.VK_SHADER_STAGE_VERTEX_BIT;
                if (name.contains("frag")) yield VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
                if (name.contains("comp")) yield VK10.VK_SHADER_STAGE_COMPUTE_BIT;
                yield VK10.VK_SHADER_STAGE_ALL_GRAPHICS;
            }
        };
    }

    public synchronized void destroy(VkDevice device) {
        if (vkShaderModule != 0L) {
            ShaderModuleFactory.destroyShaderModule(device, vkShaderModule);
            vkShaderModule = 0L;
        }
    }

    public synchronized void destroy() {
        destroy(VulkanDevice.getDevice());
    }

    @Override
    public String toString() {
        return "VulkairisShader{" +
                "id=" + virtualId +
                ", name='" + name + '\'' +
                ", stage=" + stage +
                ", vkModule=0x" + Long.toHexString(vkShaderModule) +
                ", spirvBytes=" + (spirv != null ? spirv.remaining() : 0) +
                '}';
    }
}
