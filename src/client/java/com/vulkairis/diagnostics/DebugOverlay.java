package com.vulkairis.diagnostics;

import com.vulkairis.core.VulkairisController;
import com.vulkairis.vulkan.VulkanCapabilities;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic overlay renderer for in-game debugging (F3 / custom overlay).
 */
public class DebugOverlay {
    public static List<String> getDebugLines() {
        List<String> lines = new ArrayList<>();
        VulkairisController controller = VulkairisController.getInstance();
        VulkanCapabilities caps = VulkanCapabilities.getActiveCapabilities();

        lines.add("§6[Vulkairis Experimental Bridge]§r");
        lines.add(String.format("Backend State: %s%s§r",
                controller.getState() == com.vulkairis.core.BackendState.READY ? "§a" : "§e",
                controller.getState()));
        lines.add("GPU: " + caps.getDeviceName() + " (" + caps.getVendorName() + ")");
        lines.add("Vulkan API: " + caps.getApiVersionString());
        lines.add("Active Shaderpack: " + controller.getActiveShaderPackName());
        lines.add(String.format("Shaders Compiled: %d | Pipelines: %d",
                controller.getCompiledShadersCount(), controller.getCreatedPipelinesCount()));
        lines.add("Last Fallback Reason: " + DiagnosticManager.getLastFallbackReason());

        return lines;
    }

    public static void render(GuiGraphics graphics) {
        if (graphics == null) return;
        List<String> lines = getDebugLines();
        int y = 10;
        for (String line : lines) {
            graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, line, 10, y, 0xFFFFFF);
            y += 10;
        }
    }
}
