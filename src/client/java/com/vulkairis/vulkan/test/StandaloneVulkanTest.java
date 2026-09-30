package com.vulkairis.vulkan.test;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.vulkairis.translation.SpirvCompiler;
import com.vulkairis.translation.VulkairisShader;
import com.vulkairis.vulkan.VulkanDevice;
import com.vulkairis.vulkan.descriptor.VulkanDescriptorManager;
import com.vulkairis.vulkan.pipeline.PipelineManager;
import com.vulkairis.vulkan.pipeline.VulkanPipelineFactory;
import net.irisshaders.iris.gl.shader.ShaderType;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standalone Vulkan Test Renderer (Phase 3).
 * Verifies basic colored triangle/quad directly on Vulkan using real VkShaderModule handles
 * without GLSL re-feed or raw GL escape paths.
 */
public class StandaloneVulkanTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vulkairis/StandaloneTest");

    private static final String BASIC_VERT = """
            #version 450 core
            layout (location = 0) in vec3 Position;
            layout (location = 1) in vec4 Color;
            layout (location = 0) out vec4 vertexColor;
            void main() {
                gl_Position = vec4(Position, 1.0);
                vertexColor = Color;
            }
            """;

    private static final String BASIC_FRAG = """
            #version 450 core
            layout (location = 0) in vec4 vertexColor;
            layout (location = 0) out vec4 fragColor;
            void main() {
                fragColor = vertexColor;
            }
            """;

    private static VulkairisShader vertShader = null;
    private static VulkairisShader fragShader = null;
    private static long testDescriptorSetLayout = 0L;
    private static long testDescriptorSet = 0L;
    private static long testPipelineLayout = 0L;
    private static final Map<Long, Long> PIPELINES_BY_RENDER_PASS = new ConcurrentHashMap<>();
    private static boolean testPassed = false;
    private static boolean initialized = false;

    public static synchronized boolean initTestPipeline() {
        if (initialized) return true;

        VkDevice device = VulkanDevice.getDevice();
        if (!VulkanDevice.isLive(device)) {
            LOGGER.warn("[Vulkairis/Test] Vulkan device not live yet.");
            return false;
        }

        try {
            LOGGER.info("[Vulkairis/Test] Running Phase 3 Standalone Vulkan Test initialization...");

            // 1. Initialize Pipeline Cache
            PipelineManager.getOrCreatePipelineCache();

            // 2. Compile shaders with in-process Shaderc to SPIR-V -> native VkShaderModule
            vertShader = SpirvCompiler.compileShader("test_vert", BASIC_VERT, ShaderType.VERTEX);
            fragShader = SpirvCompiler.compileShader("test_frag", BASIC_FRAG, ShaderType.FRAGMENT);

            if (vertShader == null || fragShader == null ||
                    vertShader.getVkShaderModule() == 0L || fragShader.getVkShaderModule() == 0L) {
                LOGGER.error("[Vulkairis/Test] Failed to create native VkShaderModule handles for test shaders");
                return false;
            }

            // 3. Create descriptor set layout and allocate descriptor set
            testDescriptorSetLayout = VulkanDescriptorManager.getOrCreateDescriptorSetLayout(List.of(
                    new VulkanDescriptorManager.BindingInfo(0, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
                            VK10.VK_SHADER_STAGE_FRAGMENT_BIT, 1)
            ));

            testDescriptorSet = VulkanDescriptorManager.allocateDescriptorSet(testDescriptorSetLayout);
            if (testDescriptorSet != 0L) {
                VulkanDescriptorManager.updateTextureBinding(testDescriptorSet, 0, 0);
            }

            // 4. Create pipeline layout
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkPipelineLayoutCreateInfo layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                        .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
                if (testDescriptorSetLayout != 0L) {
                    layoutInfo.pSetLayouts(stack.longs(testDescriptorSetLayout));
                }

                LongBuffer pLayout = stack.mallocLong(1);
                int res = VK10.vkCreatePipelineLayout(device, layoutInfo, null, pLayout);
                if (res != VK10.VK_SUCCESS) {
                    throw new RuntimeException("Failed to create test VkPipelineLayout: " + res);
                }
                testPipelineLayout = pLayout.get(0);
                LOGGER.info("[Vulkairis/Test] Created test VkPipelineLayout 0x{}", Long.toHexString(testPipelineLayout));
            }

            initialized = true;
            return true;
        } catch (Throwable t) {
            LOGGER.error("[Vulkairis/Test] Standalone Vulkan test initialization failed: {}", t.getMessage(), t);
            return false;
        }
    }

    private static long getOrCreatePipelineForPass(long renderPassId) {
        return PIPELINES_BY_RENDER_PASS.computeIfAbsent(renderPassId, passId -> {
            VkDevice device = VulkanDevice.getDevice();
            VulkanPipelineFactory.PipelineParams params = new VulkanPipelineFactory.PipelineParams(
                    vertShader.getVkShaderModule(),
                    fragShader.getVkShaderModule(),
                    testPipelineLayout,
                    passId,
                    DefaultVertexFormat.POSITION_COLOR,
                    false, // depthTest
                    false, // depthWrite
                    false, // cull
                    true,  // blend
                    PipelineManager.getOrCreatePipelineCache()
            );
            long pipeline = VulkanPipelineFactory.createGraphicsPipeline(device, params);
            LOGGER.info("[Vulkairis/Test] Created native VkPipeline 0x{} for VkRenderPass 0x{}",
                    Long.toHexString(pipeline), Long.toHexString(passId));
            return pipeline;
        });
    }

    private static int drawCount = 0;
    private static final int MAX_TEST_DRAWS = 600;

    public static void executeTestDraw() {
        executeTestDraw(Renderer.getCommandBuffer());
    }

    public static void executeTestDraw(VkCommandBuffer cmdBuffer) {
        if (!initialized && !initTestPipeline()) return;
        if (!initialized) return;
        if (drawCount >= MAX_TEST_DRAWS) return;

        try {
            Renderer renderer = Renderer.getInstance();
            if (cmdBuffer == null) {
                cmdBuffer = Renderer.getCommandBuffer();
            }
            if (renderer == null || cmdBuffer == null) return;

            RenderPass boundPass = renderer.getBoundRenderPass();
            if (boundPass == null || boundPass.getId() == 0L) return;

            long pipeline = getOrCreatePipelineForPass(boundPass.getId());
            if (pipeline == 0L) return;

            // 1. Bind native VkPipeline directly
            VK10.vkCmdBindPipeline(cmdBuffer, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);

            // 2. Set dynamic viewport & scissor
            Framebuffer fb = renderer.getBoundFramebuffer();
            if (fb != null) {
                int w = fb.getWidth();
                int h = fb.getHeight();
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    VkViewport.Buffer vp = VkViewport.calloc(1, stack)
                            .x(0.0f).y(0.0f).width((float) w).height((float) h)
                            .minDepth(0.0f).maxDepth(1.0f);
                    VK10.vkCmdSetViewport(cmdBuffer, 0, vp);

                    VkRect2D.Buffer sc = VkRect2D.calloc(1, stack);
                    sc.offset().set(0, 0);
                    sc.extent().set(w, h);
                    VK10.vkCmdSetScissor(cmdBuffer, 0, sc);
                }
            }

            // 3. Bind descriptor set
            if (testDescriptorSet != 0L && testPipelineLayout != 0L) {
                VulkanDescriptorManager.bindDescriptorSets(cmdBuffer, testPipelineLayout, testDescriptorSet);
            }

            // 4. Draw colored quad
            ByteBuffer buffer = BufferUtils.createByteBuffer(4 * 16);
            buffer.putFloat(-0.5f).putFloat(-0.5f).putFloat(0.0f).put((byte) 0).put((byte) 255).put((byte) 255).put((byte) 255);
            buffer.putFloat(-0.5f).putFloat(0.5f).putFloat(0.0f).put((byte) 255).put((byte) 0).put((byte) 255).put((byte) 255);
            buffer.putFloat(0.5f).putFloat(0.5f).putFloat(0.0f).put((byte) 255).put((byte) 255).put((byte) 0).put((byte) 255);
            buffer.putFloat(0.5f).putFloat(-0.5f).putFloat(0.0f).put((byte) 0).put((byte) 255).put((byte) 0).put((byte) 255);
            buffer.flip();

            Renderer.getDrawer().draw(buffer, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR, 4);

            drawCount++;
            if (!testPassed) {
                testPassed = true;
                LOGGER.info("[Vulkairis/Test] [SUCCESS] Phase 3 standalone Vulkan draw call verified from VkShaderModule handles (vert: 0x{}, frag: 0x{}) without GLSL re-feed!",
                        Long.toHexString(vertShader.getVkShaderModule()),
                        Long.toHexString(fragShader.getVkShaderModule()));
            }
        } catch (NullPointerException ignored) {
            // Safe retry when active render pass is not bound yet
        } catch (Throwable t) {
            LOGGER.error("[Vulkairis/Test] Error executing test draw: {}", t.getMessage(), t);
        }
    }

    public static void resetDrawCount() {
        drawCount = 0;
    }

    public static boolean isTestPassed() {
        return testPassed;
    }

    public static synchronized void cleanUp() {
        VkDevice device = VulkanDevice.getDevice();
        for (long pipe : PIPELINES_BY_RENDER_PASS.values()) {
            VulkanPipelineFactory.destroyPipeline(device, pipe);
        }
        PIPELINES_BY_RENDER_PASS.clear();

        if (testPipelineLayout != 0L && VulkanDevice.isLive(device)) {
            try {
                VK10.vkDestroyPipelineLayout(device, testPipelineLayout, null);
            } catch (Throwable ignored) {
            }
            testPipelineLayout = 0L;
        }

        if (vertShader != null) {
            vertShader.destroy(device);
            vertShader = null;
        }
        if (fragShader != null) {
            fragShader.destroy(device);
            fragShader = null;
        }

        initialized = false;
        testPassed = false;
        drawCount = 0;
    }
}
