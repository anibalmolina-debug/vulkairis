package com.vulkairis.diagnostics;

import com.vulkairis.core.VulkairisController;
import com.vulkairis.iris.IrisBridge;
import com.vulkairis.vulkan.VulkanCapabilities;
import com.vulkairis.vulkan.test.StandaloneVulkanTest;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

/**
 * Registers /vulkairis client-side debug commands.
 */
public class VulkairisCommands {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("vulkairis")
                    .then(ClientCommandManager.literal("status").executes(ctx -> {
                        VulkairisController controller = VulkairisController.getInstance();
                        ctx.getSource().sendFeedback(Component.literal("§6=== Vulkairis Status ==="));
                        ctx.getSource().sendFeedback(Component.literal("State: §a" + controller.getState()));
                        ctx.getSource().sendFeedback(Component.literal("Shader Pack: " + controller.getActiveShaderPackName()));
                        ctx.getSource().sendFeedback(Component.literal("Frames Processed: " + controller.getFrameCount()));
                        ctx.getSource().sendFeedback(Component.literal("Compiled Shaders: " + controller.getCompiledShadersCount()));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("vulkan").executes(ctx -> {
                        VulkanCapabilities caps = VulkanCapabilities.getActiveCapabilities();
                        ctx.getSource().sendFeedback(Component.literal("§6=== Vulkan Capabilities ==="));
                        ctx.getSource().sendFeedback(Component.literal("GPU: §b" + caps.getDeviceName()));
                        ctx.getSource().sendFeedback(Component.literal("Vendor: " + caps.getVendorName()));
                        ctx.getSource().sendFeedback(Component.literal("Vulkan API: " + caps.getApiVersionString()));
                        ctx.getSource().sendFeedback(Component.literal("Device Type: " + caps.getDeviceType()));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("shader").executes(ctx -> {
                        ctx.getSource().sendFeedback(Component.literal("§6=== Vulkairis Shader Diagnostics ==="));
                        ctx.getSource().sendFeedback(Component.literal("Active Pack: " + IrisBridge.getCurrentPackName()));
                        ctx.getSource().sendFeedback(Component.literal("Last Fallback: " + DiagnosticManager.getLastFallbackReason()));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("reload").executes(ctx -> {
                        ctx.getSource().sendFeedback(Component.literal("§e[Vulkairis] Triggering shader pack reload..."));
                        IrisBridge.onShaderPackChanged();
                        ctx.getSource().sendFeedback(Component.literal("§a[Vulkairis] Reload complete."));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("test").executes(ctx -> {
                        ctx.getSource().sendFeedback(Component.literal("§e[Vulkairis] Executing Phase 3 Standalone Vulkan Test..."));
                        boolean ok = StandaloneVulkanTest.initTestPipeline();
                        if (ok) {
                            ctx.getSource().sendFeedback(Component.literal("§a[Vulkairis] Standalone Vulkan pipeline test passed!"));
                        } else {
                            ctx.getSource().sendFeedback(Component.literal("§c[Vulkairis] Standalone Vulkan pipeline test failed."));
                        }
                        return 1;
                    }))
            );
        });
    }
}
