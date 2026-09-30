package com.vulkairis.vulkan.pipeline;

import com.vulkairis.vulkan.VulkanDevice;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Manages graphics pipeline compilation, driver-level VkPipelineCache persistence,
 * and in-memory pipeline caching to eliminate stutters.
 */
public class PipelineManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vulkairis/PipelineManager");
    private static final Path CACHE_DIR = FabricLoader.getInstance().getGameDir().resolve("vulkairis");
    private static final Path CACHE_FILE = CACHE_DIR.resolve("pipeline_cache.bin");


    private static long vkPipelineCache = 0L;
    private static final Map<PipelineKey, Long> MEMORY_PIPELINE_CACHE = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    public static synchronized long getOrCreatePipelineCache() {
        if (initialized && vkPipelineCache != 0L) return vkPipelineCache;

        VkDevice device = VulkanDevice.getDevice();
        if (!VulkanDevice.isLive(device)) {
            LOGGER.warn("[Vulkairis] Cannot initialize VkPipelineCache: VkDevice not available");
            return 0L;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer initialData = loadCacheDataFromDisk();

            VkPipelineCacheCreateInfo createInfo = VkPipelineCacheCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO);

            if (initialData != null && initialData.hasRemaining()) {
                createInfo.pInitialData(initialData);
                LOGGER.info("[Vulkairis] Populating VkPipelineCache with {} bytes of cached driver data",
                        initialData.remaining());
            }

            LongBuffer pCache = stack.mallocLong(1);
            int result = VK10.vkCreatePipelineCache(device, createInfo, null, pCache);
            if (result != VK10.VK_SUCCESS) {
                LOGGER.error("[Vulkairis] Failed to create VkPipelineCache, Vulkan error: {}", result);
                return 0L;
            }

            vkPipelineCache = pCache.get(0);
            initialized = true;
            LOGGER.info("[Vulkairis] Initialized driver VkPipelineCache 0x{}", Long.toHexString(vkPipelineCache));
            return vkPipelineCache;
        }
    }

    private static ByteBuffer loadCacheDataFromDisk() {
        try {
            if (!Files.exists(CACHE_FILE)) return null;

            byte[] bytes = Files.readAllBytes(CACHE_FILE);
            if (bytes.length < 16) return null;

            ByteBuffer directBuffer = ByteBuffer.allocateDirect(bytes.length);
            directBuffer.put(bytes);
            directBuffer.flip();
            return directBuffer;
        } catch (Exception e) {
            LOGGER.warn("[Vulkairis] Could not load pipeline cache from disk: {}", e.getMessage());
            return null;
        }
    }

    public static synchronized void savePipelineCacheToDisk() {
        if (vkPipelineCache == 0L) return;

        VkDevice device = VulkanDevice.getDevice();
        if (!VulkanDevice.isLive(device)) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.PointerBuffer pDataSize = stack.mallocPointer(1);
            int res1 = VK10.vkGetPipelineCacheData(device, vkPipelineCache, pDataSize, null);
            if (res1 != VK10.VK_SUCCESS) return;

            long dataSize = pDataSize.get(0);
            if (dataSize <= 0) return;

            ByteBuffer dataBuffer = ByteBuffer.allocateDirect((int) dataSize);
            int res2 = VK10.vkGetPipelineCacheData(device, vkPipelineCache, pDataSize, dataBuffer);
            if (res2 != VK10.VK_SUCCESS) return;

            if (!Files.exists(CACHE_DIR)) {
                Files.createDirectories(CACHE_DIR);
            }

            byte[] bytes = new byte[(int) dataSize];
            dataBuffer.get(bytes);
            Files.write(CACHE_FILE, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            LOGGER.info("[Vulkairis] Saved {} bytes of compiled GPU pipelines to {}", dataSize, CACHE_FILE);
        } catch (Throwable t) {
            LOGGER.warn("[Vulkairis] Pipeline cache save notice: {}", t.getMessage());
        }
    }

    public static long getOrCreatePipeline(PipelineKey key, LongSupplier pipelineCreator) {
        return MEMORY_PIPELINE_CACHE.computeIfAbsent(key, k -> {
            LOGGER.debug("[Vulkairis] Compiling new pipeline for key: {}", k);
            long handle = pipelineCreator.getAsLong();
            LOGGER.info("[Vulkairis] Cached GraphicsPipeline 0x{} in memory for format '{}'",
                    Long.toHexString(handle), k.vertexFormatName());
            return handle;
        });
    }

    public static void cleanUp() {
        try {
            savePipelineCacheToDisk();
        } catch (Throwable ignored) {
        }

        VkDevice device = VulkanDevice.getDevice();
        if (VulkanDevice.isLive(device) && vkPipelineCache != 0L) {
            try {
                VK10.vkDestroyPipelineCache(device, vkPipelineCache, null);
            } catch (Throwable ignored) {
            }
        }
        vkPipelineCache = 0L;

        MEMORY_PIPELINE_CACHE.clear();
        initialized = false;
        LOGGER.info("[Vulkairis] Cleaned up PipelineManager.");
    }
}
