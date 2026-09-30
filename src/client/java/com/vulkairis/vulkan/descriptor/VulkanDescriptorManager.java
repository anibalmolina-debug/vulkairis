package com.vulkairis.vulkan.descriptor;

import com.vulkairis.vulkan.VulkanDevice;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.LongBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages rigid Vulkan descriptor set layouts, pools, allocations, and binding updates.
 */
public class VulkanDescriptorManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vulkairis/DescriptorManager");

    public record BindingInfo(int binding, int descriptorType, int stageFlags, int descriptorCount) {}
    public record LayoutKey(List<BindingInfo> bindings) {}

    private static final Map<LayoutKey, Long> LAYOUT_CACHE = new ConcurrentHashMap<>();
    private static final Map<Long, Long> POOL_MAP = new ConcurrentHashMap<>();
    private static long defaultDescriptorPool = 0L;
    private static VulkanImage fallbackWhiteTexture = null;

    public static long getOrCreateDescriptorSetLayout(List<BindingInfo> bindings) {
        LayoutKey key = new LayoutKey(Collections.unmodifiableList(new ArrayList<>(bindings)));
        return LAYOUT_CACHE.computeIfAbsent(key, VulkanDescriptorManager::createDescriptorSetLayout);
    }

    private static long createDescriptorSetLayout(LayoutKey key) {
        VkDevice device = VulkanDevice.getDevice();
        if (!VulkanDevice.isLive(device)) {
            LOGGER.warn("[Vulkairis] Cannot create VkDescriptorSetLayout: VkDevice not available");
            return 0L;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            List<BindingInfo> bindings = key.bindings();
            VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.calloc(bindings.size(), stack);

            for (int i = 0; i < bindings.size(); i++) {
                BindingInfo info = bindings.get(i);
                layoutBindings.get(i)
                        .binding(info.binding())
                        .descriptorType(info.descriptorType())
                        .descriptorCount(info.descriptorCount() > 0 ? info.descriptorCount() : 1)
                        .stageFlags(info.stageFlags());
            }

            VkDescriptorSetLayoutCreateInfo createInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pBindings(layoutBindings);

            LongBuffer pSetLayout = stack.mallocLong(1);
            int result = VK10.vkCreateDescriptorSetLayout(device, createInfo, null, pSetLayout);
            if (result != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create VkDescriptorSetLayout, Vulkan error: " + result);
            }

            long layoutHandle = pSetLayout.get(0);
            LOGGER.info("[Vulkairis] Created VkDescriptorSetLayout 0x{} with {} bindings",
                    Long.toHexString(layoutHandle), bindings.size());
            return layoutHandle;
        }
    }

    public static synchronized long getOrCreateDefaultPool() {
        if (defaultDescriptorPool != 0L) return defaultDescriptorPool;

        VkDevice device = VulkanDevice.getDevice();
        if (!VulkanDevice.isLive(device)) return 0L;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(4, stack);
            poolSizes.get(0).type(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER).descriptorCount(512);
            poolSizes.get(1).type(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(1024);
            poolSizes.get(2).type(VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).descriptorCount(256);
            poolSizes.get(3).type(VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE).descriptorCount(256);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .flags(VK10.VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
                    .maxSets(1024)
                    .pPoolSizes(poolSizes);

            LongBuffer pDescriptorPool = stack.mallocLong(1);
            int result = VK10.vkCreateDescriptorPool(device, poolInfo, null, pDescriptorPool);
            if (result != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create VkDescriptorPool, error: " + result);
            }

            defaultDescriptorPool = pDescriptorPool.get(0);
            LOGGER.info("[Vulkairis] Created default VkDescriptorPool 0x{}", Long.toHexString(defaultDescriptorPool));
            return defaultDescriptorPool;
        }
    }

    public static long allocateDescriptorSet(long descriptorSetLayout) {
        long pool = getOrCreateDefaultPool();
        if (pool == 0L || descriptorSetLayout == 0L) return 0L;

        VkDevice device = VulkanDevice.getDevice();
        if (!VulkanDevice.isLive(device)) return 0L;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pSetLayout = stack.longs(descriptorSetLayout);
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(pool)
                    .pSetLayouts(pSetLayout);

            LongBuffer pDescriptorSet = stack.mallocLong(1);
            int result = VK10.vkAllocateDescriptorSets(device, allocInfo, pDescriptorSet);
            if (result != VK10.VK_SUCCESS) {
                LOGGER.error("[Vulkairis] Failed to allocate descriptor set, error: {}", result);
                return 0L;
            }

            long setHandle = pDescriptorSet.get(0);
            POOL_MAP.put(setHandle, pool);
            return setHandle;
        }
    }

    public static void updateTextureBinding(long descriptorSet, int binding, int openGlTextureId) {
        VkDevice device = VulkanDevice.getDevice();
        if (!VulkanDevice.isLive(device) || descriptorSet == 0L) return;

        VulkanImage vulkanImage = null;
        if (openGlTextureId > 0) {
            VkGlTexture glTexture = VkGlTexture.getTexture(openGlTextureId);
            if (glTexture != null) {
                vulkanImage = glTexture.getVulkanImage();
            }
        }

        if (vulkanImage == null) {
            if (fallbackWhiteTexture == null) {
                fallbackWhiteTexture = VulkanImage.createWhiteTexture();
            }
            vulkanImage = fallbackWhiteTexture;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long imageView = vulkanImage.getImageView();
            long sampler = vulkanImage.getSampler();

            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack)
                    .imageLayout(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .imageView(imageView)
                    .sampler(sampler);

            VkWriteDescriptorSet.Buffer descriptorWrite = VkWriteDescriptorSet.calloc(1, stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSet)
                    .dstBinding(binding)
                    .dstArrayElement(0)
                    .descriptorType(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .pImageInfo(imageInfo);

            VK10.vkUpdateDescriptorSets(device, descriptorWrite, null);
        }
    }

    public static void updateUniformBufferBinding(long descriptorSet, int binding, long bufferHandle, long offset, long range) {
        VkDevice device = VulkanDevice.getDevice();
        if (!VulkanDevice.isLive(device) || descriptorSet == 0L || bufferHandle == 0L) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
                    .buffer(bufferHandle)
                    .offset(offset)
                    .range(range);

            VkWriteDescriptorSet.Buffer descriptorWrite = VkWriteDescriptorSet.calloc(1, stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSet)
                    .dstBinding(binding)
                    .dstArrayElement(0)
                    .descriptorType(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(1)
                    .pBufferInfo(bufferInfo);

            VK10.vkUpdateDescriptorSets(device, descriptorWrite, null);
        }
    }

    public static void bindDescriptorSets(VkCommandBuffer cmdBuffer, long pipelineLayout, long... descriptorSets) {
        if (cmdBuffer == null || pipelineLayout == 0L || descriptorSets == null || descriptorSets.length == 0) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pDescriptorSets = stack.mallocLong(descriptorSets.length);
            for (long set : descriptorSets) {
                pDescriptorSets.put(set);
            }
            pDescriptorSets.flip();

            VK10.vkCmdBindDescriptorSets(
                    cmdBuffer,
                    VK10.VK_PIPELINE_BIND_POINT_GRAPHICS,
                    pipelineLayout,
                    0,
                    pDescriptorSets,
                    null
            );
        }
    }

    public static void freeDescriptorSet(long descriptorSet) {
        Long pool = POOL_MAP.remove(descriptorSet);
        if (pool != null && pool != 0L) {
            VkDevice device = VulkanDevice.getDevice();
            if (VulkanDevice.isLive(device)) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    LongBuffer pSets = stack.longs(descriptorSet);
                    VK10.vkFreeDescriptorSets(device, pool, pSets);
                }
            }
        }
    }

    public static void cleanUp() {
        VkDevice device = VulkanDevice.getDevice();
        if (VulkanDevice.isLive(device)) {
            LAYOUT_CACHE.forEach((key, layout) -> {
                if (layout != 0L) {
                    try {
                        VK10.vkDestroyDescriptorSetLayout(device, layout, null);
                    } catch (Throwable ignored) {
                    }
                }
            });

            if (defaultDescriptorPool != 0L) {
                try {
                    VK10.vkDestroyDescriptorPool(device, defaultDescriptorPool, null);
                } catch (Throwable ignored) {
                }
            }

            if (fallbackWhiteTexture != null) {
                try {
                    fallbackWhiteTexture.free();
                } catch (Throwable ignored) {
                }
            }
        }

        LAYOUT_CACHE.clear();
        defaultDescriptorPool = 0L;
        fallbackWhiteTexture = null;
        LOGGER.info("[Vulkairis] Cleaned up descriptor layouts and pools.");
    }
}
