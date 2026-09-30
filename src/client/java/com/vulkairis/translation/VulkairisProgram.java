package com.vulkairis.translation;

import com.vulkairis.vulkan.VulkanDevice;
import com.vulkairis.vulkan.pipeline.PipelineKey;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.LongBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a linked Iris program in the Vulkairis bridge.
 * Encapsulates attached shaders (vertex, fragment, compute), pipeline layout,
 * descriptor layouts, and compiled graphics pipelines.
 */
public class VulkairisProgram {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vulkairis/Program");

    private final int virtualProgramId;
    private final String name;
    private final List<Integer> attachedShaderIds;
    private final List<VulkairisShader> attachedShaders = new ArrayList<>();
    private final Map<PipelineKey, Long> pipelines = new ConcurrentHashMap<>();

    private long pipelineLayout = 0L;
    private long descriptorSetLayout = 0L;

    public VulkairisProgram(int virtualProgramId, String name, List<Integer> attachedShaderIds) {
        this.virtualProgramId = virtualProgramId;
        this.name = (name != null && !name.isEmpty()) ? name : "program_" + virtualProgramId;
        this.attachedShaderIds = Collections.unmodifiableList(new ArrayList<>(attachedShaderIds));
        resolveAttachedShaders();
    }

    public void resolveAttachedShaders() {
        attachedShaders.clear();
        for (int id : attachedShaderIds) {
            VulkairisShader shader = VulkanShaderRegistry.getShader(id);
            if (shader != null) {
                attachedShaders.add(shader);
            }
        }
    }

    public int getVirtualProgramId() {
        return virtualProgramId;
    }

    public int virtualProgramId() {
        return virtualProgramId;
    }

    public String getName() {
        return name;
    }

    public String name() {
        return name;
    }

    public List<Integer> getAttachedShaderIds() {
        return attachedShaderIds;
    }

    public List<Integer> attachedShaderIds() {
        return attachedShaderIds;
    }

    public List<VulkairisShader> getAttachedShaders() {
        if (attachedShaders.size() != attachedShaderIds.size()) {
            resolveAttachedShaders();
        }
        return Collections.unmodifiableList(attachedShaders);
    }

    public VulkairisShader getVertexShader() {
        for (VulkairisShader shader : getAttachedShaders()) {
            if ((shader.getVkStageFlag() & VK10.VK_SHADER_STAGE_VERTEX_BIT) != 0) {
                return shader;
            }
        }
        return null;
    }

    public VulkairisShader getFragmentShader() {
        for (VulkairisShader shader : getAttachedShaders()) {
            if ((shader.getVkStageFlag() & VK10.VK_SHADER_STAGE_FRAGMENT_BIT) != 0) {
                return shader;
            }
        }
        return null;
    }

    public VulkairisShader getComputeShader() {
        for (VulkairisShader shader : getAttachedShaders()) {
            if ((shader.getVkStageFlag() & VK10.VK_SHADER_STAGE_COMPUTE_BIT) != 0) {
                return shader;
            }
        }
        return null;
    }

    public long getPipelineLayout() {
        return pipelineLayout;
    }

    public void setPipelineLayout(long pipelineLayout) {
        this.pipelineLayout = pipelineLayout;
    }

    public long getDescriptorSetLayout() {
        return descriptorSetLayout;
    }

    public void setDescriptorSetLayout(long descriptorSetLayout) {
        this.descriptorSetLayout = descriptorSetLayout;
    }

    public synchronized long getOrCreatePipelineLayout(VkDevice device, long descriptorSetLayout) {
        if (this.pipelineLayout != 0L) return this.pipelineLayout;
        if (!VulkanDevice.isLive(device)) return 0L;

        this.descriptorSetLayout = descriptorSetLayout;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineLayoutCreateInfo layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);

            if (descriptorSetLayout != 0L) {
                LongBuffer pSetLayouts = stack.longs(descriptorSetLayout);
                layoutInfo.pSetLayouts(pSetLayouts);
            }

            LongBuffer pLayout = stack.mallocLong(1);
            int res = VK10.vkCreatePipelineLayout(device, layoutInfo, null, pLayout);
            if (res != VK10.VK_SUCCESS) {
                throw new RuntimeException("vkCreatePipelineLayout failed with error: " + res);
            }

            this.pipelineLayout = pLayout.get(0);
            LOGGER.info("[Vulkairis] Created VkPipelineLayout 0x{} for program '{}'",
                    Long.toHexString(this.pipelineLayout), name);
            return this.pipelineLayout;
        }
    }

    public void cachePipeline(PipelineKey key, long pipeline) {
        pipelines.put(key, pipeline);
    }

    public Long getCachedPipeline(PipelineKey key) {
        return pipelines.get(key);
    }

    public synchronized void destroy(VkDevice device) {
        if (VulkanDevice.isLive(device)) {
            for (Map.Entry<PipelineKey, Long> entry : pipelines.entrySet()) {
                long pipe = entry.getValue();
                if (pipe != 0L) {
                    try {
                        VK10.vkDestroyPipeline(device, pipe, null);
                    } catch (Throwable ignored) {
                    }
                }
            }
            pipelines.clear();

            if (pipelineLayout != 0L) {
                try {
                    VK10.vkDestroyPipelineLayout(device, pipelineLayout, null);
                    LOGGER.info("[Vulkairis] Destroyed VkPipelineLayout 0x{} for program '{}'",
                            Long.toHexString(pipelineLayout), name);
                } catch (Throwable ignored) {
                }
                pipelineLayout = 0L;
            }
        }
    }

    public synchronized void destroy() {
        destroy(VulkanDevice.getDevice());
    }
}
