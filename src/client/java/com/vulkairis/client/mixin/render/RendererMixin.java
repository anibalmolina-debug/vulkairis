package com.vulkairis.client.mixin.render;

import com.vulkairis.rendering.VulkanRenderer;
import com.vulkairis.vulkan.test.StandaloneVulkanTest;
import net.vulkanmod.vulkan.Renderer;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Renderer.class, remap = false)
public class RendererMixin {

    @Inject(method = "beginFrame()V", at = @At("HEAD"), remap = false)
    private void onBeginFrame(CallbackInfo ci) {
        VulkanRenderer.getInstance().beginFrame();
    }

    @Inject(method = "endRenderPass(Lorg/lwjgl/vulkan/VkCommandBuffer;)V", at = @At("HEAD"), remap = false)
    private void onEndRenderPass(VkCommandBuffer cmdBuffer, CallbackInfo ci) {
        StandaloneVulkanTest.executeTestDraw(cmdBuffer);
    }

    @Inject(method = "endFrame()V", at = @At("TAIL"), remap = false)
    private void onEndFrame(CallbackInfo ci) {
        VulkanRenderer.getInstance().endFrame();
    }
}
