package com.vulkairis.translation;

import com.vulkairis.vulkan.VulkanDevice;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registry mapping virtual Iris shader/program IDs to Vulkan VkShaderModule handles,
 * VulkairisShader objects, and VulkairisProgram pipelines.
 */
public class VulkanShaderRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vulkairis/ShaderRegistry");
    private static final AtomicInteger SHADER_ID_GEN = new AtomicInteger(1000);
    private static final AtomicInteger PROGRAM_ID_GEN = new AtomicInteger(5000);

    public record ShaderData(int virtualId, String name, int stage, long vkShaderModule, ByteBuffer spirv, String glslSource) {}
    public record ProgramData(int virtualId, String name, List<Integer> attachedShaderIds) {}

    private static final Map<Integer, VulkairisShader> SHADERS = new ConcurrentHashMap<>();
    private static final Map<Integer, VulkairisProgram> PROGRAMS = new ConcurrentHashMap<>();

    public static long createVkShaderModule(ByteBuffer spirv) {
        return ShaderModuleFactory.createShaderModule(spirv);
    }

    public static int registerShader(String name, int stage, ByteBuffer spirv, String source) {
        int virtualId = SHADER_ID_GEN.incrementAndGet();
        long vkModule = 0L;
        if (spirv != null && spirv.hasRemaining()) {
            try {
                vkModule = ShaderModuleFactory.createShaderModule(spirv);
            } catch (Exception e) {
                LOGGER.error("[Vulkairis] Error creating VkShaderModule for '{}': {}", name, e.getMessage(), e);
            }
        }

        VulkairisShader shader = new VulkairisShader(virtualId, name, stage, vkModule, spirv, source);
        SHADERS.put(virtualId, shader);
        com.vulkairis.resource.VulkairisResourceManager.getInstance().createResource(
                com.vulkairis.resource.ManagedResource.ResourceType.SHADER, virtualId, name, "Iris", vkModule);
        LOGGER.info("[Vulkairis] Registered shader handle {} -> '{}' (stage: {}, vkModule: 0x{})",
                virtualId, name, stage, Long.toHexString(vkModule));
        return virtualId;
    }

    public static int registerShader(VulkairisShader shader) {
        SHADERS.put(shader.getVirtualId(), shader);
        com.vulkairis.resource.VulkairisResourceManager.getInstance().createResource(
                com.vulkairis.resource.ManagedResource.ResourceType.SHADER, shader.getVirtualId(), shader.getName(), "Iris", shader.getVkShaderModule());
        return shader.getVirtualId();
    }

    public static int registerProgram(String name, List<Integer> attachedShaderIds) {
        int virtualProgramId = PROGRAM_ID_GEN.incrementAndGet();
        VulkairisProgram program = new VulkairisProgram(virtualProgramId, name, attachedShaderIds);
        PROGRAMS.put(virtualProgramId, program);
        com.vulkairis.resource.VulkairisResourceManager.getInstance().createResource(
                com.vulkairis.resource.ManagedResource.ResourceType.PROGRAM, virtualProgramId, name, "Iris", 0L);
        LOGGER.info("[Vulkairis] Registered program handle {} -> '{}' with shaders {}",
                virtualProgramId, name, attachedShaderIds);
        return virtualProgramId;
    }

    public static VulkairisShader getShader(int virtualId) {
        return SHADERS.get(virtualId);
    }

    public static ShaderData getShaderData(int virtualId) {
        VulkairisShader s = SHADERS.get(virtualId);
        if (s == null) return null;
        return new ShaderData(s.virtualId(), s.name(), s.stage(), s.vkShaderModule(), s.spirv(), s.glslSource());
    }

    public static VulkairisProgram getProgram(int virtualProgramId) {
        return PROGRAMS.get(virtualProgramId);
    }

    public static ProgramData getProgramData(int virtualProgramId) {
        VulkairisProgram p = PROGRAMS.get(virtualProgramId);
        if (p == null) return null;
        return new ProgramData(p.virtualProgramId(), p.name(), p.attachedShaderIds());
    }

    public static void deleteShader(int virtualId) {
        com.vulkairis.resource.VulkairisResourceManager.getInstance().releaseResource(virtualId);
        VulkairisShader shader = SHADERS.remove(virtualId);
        if (shader != null) {
            shader.destroy();
        }
    }

    public static void deleteProgram(int virtualProgramId) {
        com.vulkairis.resource.VulkairisResourceManager.getInstance().releaseResource(virtualProgramId);
        VulkairisProgram program = PROGRAMS.remove(virtualProgramId);
        if (program != null) {
            program.destroy();
        }
    }

    public static void cleanUp() {
        VkDevice device = VulkanDevice.getDevice();
        for (VulkairisProgram program : PROGRAMS.values()) {
            program.destroy(device);
        }
        for (VulkairisShader shader : SHADERS.values()) {
            shader.destroy(device);
        }
        SHADERS.clear();
        PROGRAMS.clear();
        LOGGER.info("[Vulkairis] Cleaned up VulkanShaderRegistry.");
    }
}
