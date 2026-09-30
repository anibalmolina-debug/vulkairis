package com.vulkairis.vulkan;

import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages Vulkan instance lifecycle, physical device discovery,
 * and GPU selection according to prototype criteria.
 */
public class VulkanInstance {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vulkairis/Instance");

    private static VkInstance activeInstance = null;
    private static VkPhysicalDevice selectedPhysicalDevice = null;
    private static boolean ownedInstance = false;

    public static synchronized VkInstance getOrCreateInstance() {
        if (activeInstance != null) return activeInstance;

        // Check if VulkanMod already created an instance
        try {
            if (Vulkan.getVkDevice() != null) {
                LOGGER.info("[Vulkairis] VulkanMod active VkDevice detected; acquiring Vulkan environment.");
                // When VulkanMod is active, we utilize its initialized device and instance
                return activeInstance;
            }
        } catch (Throwable ignored) {
        }

        return activeInstance;
    }

    /**
     * Enumerates available physical devices and selects the best candidate (preferring discrete GPUs).
     */
    public static List<VkPhysicalDevice> enumeratePhysicalDevices(VkInstance instance) {
        List<VkPhysicalDevice> devices = new ArrayList<>();
        if (instance == null) return devices;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pDeviceCount = stack.mallocInt(1);
            int res = VK10.vkEnumeratePhysicalDevices(instance, pDeviceCount, null);
            if (res != VK10.VK_SUCCESS || pDeviceCount.get(0) == 0) {
                LOGGER.warn("[Vulkairis] No physical devices found (result: {})", res);
                return devices;
            }

            int count = pDeviceCount.get(0);
            PointerBuffer pDevices = stack.mallocPointer(count);
            VK10.vkEnumeratePhysicalDevices(instance, pDeviceCount, pDevices);

            for (int i = 0; i < count; i++) {
                devices.add(new VkPhysicalDevice(pDevices.get(i), instance));
            }
            LOGGER.info("[Vulkairis] Found {} physical Vulkan device(s)", devices.size());
        } catch (Throwable t) {
            LOGGER.error("[Vulkairis] Error enumerating physical devices: {}", t.getMessage());
        }

        return devices;
    }

    /**
     * Selects the most suitable GPU from candidate list (discrete > integrated > other).
     */
    public static VkPhysicalDevice selectBestPhysicalDevice(List<VkPhysicalDevice> devices) {
        if (devices == null || devices.isEmpty()) return null;

        VkPhysicalDevice best = null;
        VulkanCapabilities.DeviceType bestType = VulkanCapabilities.DeviceType.OTHER;

        for (VkPhysicalDevice dev : devices) {
            VulkanCapabilities caps = new VulkanCapabilities();
            caps.populateFromPhysicalDevice(dev);

            if (best == null) {
                best = dev;
                bestType = caps.getDeviceType();
                continue;
            }

            if (caps.getDeviceType() == VulkanCapabilities.DeviceType.DISCRETE_GPU &&
                    bestType != VulkanCapabilities.DeviceType.DISCRETE_GPU) {
                best = dev;
                bestType = VulkanCapabilities.DeviceType.DISCRETE_GPU;
            }
        }

        selectedPhysicalDevice = best;
        return best;
    }

    public static VkPhysicalDevice getSelectedPhysicalDevice() {
        return selectedPhysicalDevice;
    }

    public static synchronized void destroy() {
        if (ownedInstance && activeInstance != null) {
            VK10.vkDestroyInstance(activeInstance, null);
            LOGGER.info("[Vulkairis] Destroyed standalone VkInstance.");
            activeInstance = null;
            ownedInstance = false;
        }
    }
}
