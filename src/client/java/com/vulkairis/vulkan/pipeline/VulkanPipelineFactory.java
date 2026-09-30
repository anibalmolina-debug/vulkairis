package com.vulkairis.vulkan.pipeline;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.vulkairis.translation.VulkairisProgram;
import com.vulkairis.translation.VulkairisShader;
import com.vulkairis.vulkan.VulkanDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

/**
 * Factory for creating native Vulkan VkPipeline objects directly from
 * compiled VkShaderModule handles. NEVER accepts GLSL source.
 */
public class VulkanPipelineFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vulkairis/PipelineFactory");

    public record PipelineParams(
            long vertShaderModule,
            long fragShaderModule,
            long pipelineLayout,
            long renderPass,
            VertexFormat vertexFormat,
            boolean depthTest,
            boolean depthWrite,
            boolean cull,
            boolean blend,
            long pipelineCache
    ) {}

    /**
     * Creates a native VkPipeline directly from shader modules and render parameters.
     */
    public static long createGraphicsPipeline(VkDevice device, PipelineParams params) {
        if (!VulkanDevice.isLive(device)) {
            throw new IllegalStateException("Cannot create VkPipeline: VkDevice is not live");
        }
        if (params.vertShaderModule() == 0L || params.fragShaderModule() == 0L) {
            throw new IllegalArgumentException(String.format(
                    "Invalid shader module handles: vert=0x%s, frag=0x%s",
                    Long.toHexString(params.vertShaderModule()),
                    Long.toHexString(params.fragShaderModule())
            ));
        }
        if (params.pipelineLayout() == 0L) {
            throw new IllegalArgumentException("Cannot create VkPipeline: pipelineLayout is 0");
        }
        if (params.renderPass() == 0L) {
            throw new IllegalArgumentException("Cannot create VkPipeline: renderPass is 0");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 1. Shader stages
            VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
            ByteBuffer mainEntry = stack.UTF8("main");

            stages.get(0)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK10.VK_SHADER_STAGE_VERTEX_BIT)
                    .module(params.vertShaderModule())
                    .pName(mainEntry);

            stages.get(1)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK10.VK_SHADER_STAGE_FRAGMENT_BIT)
                    .module(params.fragShaderModule())
                    .pName(mainEntry);

            // 2. Vertex input state
            VkPipelineVertexInputStateCreateInfo vertexInputInfo =
                    VertexInputDescription.createVertexInputState(stack, params.vertexFormat(), 0);

            // 3. Input assembly state
            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                    .topology(VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                    .primitiveRestartEnable(false);

            // 4. Viewport & scissor state (dynamic)
            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .viewportCount(1)
                    .scissorCount(1);

            // 5. Rasterization state
            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .depthClampEnable(false)
                    .rasterizerDiscardEnable(false)
                    .polygonMode(VK10.VK_POLYGON_MODE_FILL)
                    .lineWidth(1.0f)
                    .cullMode(params.cull() ? VK10.VK_CULL_MODE_BACK_BIT : VK10.VK_CULL_MODE_NONE)
                    .frontFace(VK10.VK_FRONT_FACE_COUNTER_CLOCKWISE)
                    .depthBiasEnable(false);

            // 6. Multisample state
            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                    .sampleShadingEnable(false)
                    .rasterizationSamples(VK10.VK_SAMPLE_COUNT_1_BIT);

            // 7. Depth stencil state
            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                    .depthTestEnable(params.depthTest())
                    .depthWriteEnable(params.depthWrite())
                    .depthCompareOp(VK10.VK_COMPARE_OP_LESS_OR_EQUAL)
                    .depthBoundsTestEnable(false)
                    .stencilTestEnable(false);

            // 8. Color blend state
            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment =
                    VkPipelineColorBlendAttachmentState.calloc(1, stack);
            colorBlendAttachment.colorWriteMask(
                    VK10.VK_COLOR_COMPONENT_R_BIT | VK10.VK_COLOR_COMPONENT_G_BIT |
                            VK10.VK_COLOR_COMPONENT_B_BIT | VK10.VK_COLOR_COMPONENT_A_BIT);
            if (params.blend()) {
                colorBlendAttachment.blendEnable(true);
                colorBlendAttachment.srcColorBlendFactor(VK10.VK_BLEND_FACTOR_SRC_ALPHA);
                colorBlendAttachment.dstColorBlendFactor(VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
                colorBlendAttachment.colorBlendOp(VK10.VK_BLEND_OP_ADD);
                colorBlendAttachment.srcAlphaBlendFactor(VK10.VK_BLEND_FACTOR_ONE);
                colorBlendAttachment.dstAlphaBlendFactor(VK10.VK_BLEND_FACTOR_ZERO);
                colorBlendAttachment.alphaBlendOp(VK10.VK_BLEND_OP_ADD);
            } else {
                colorBlendAttachment.blendEnable(false);
            }

            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .logicOpEnable(false)
                    .pAttachments(colorBlendAttachment);

            // 9. Dynamic states: Viewport and Scissor
            IntBuffer dynamicStates = stack.ints(VK10.VK_DYNAMIC_STATE_VIEWPORT, VK10.VK_DYNAMIC_STATE_SCISSOR);
            VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                    .pDynamicStates(dynamicStates);

            // 10. Pipeline create info
            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .pStages(stages)
                    .pVertexInputState(vertexInputInfo)
                    .pInputAssemblyState(inputAssembly)
                    .pViewportState(viewportState)
                    .pRasterizationState(rasterizer)
                    .pMultisampleState(multisampling)
                    .pDepthStencilState(depthStencil)
                    .pColorBlendState(colorBlending)
                    .pDynamicState(dynamicState)
                    .layout(params.pipelineLayout())
                    .renderPass(params.renderPass())
                    .subpass(0);

            LongBuffer pPipeline = stack.mallocLong(1);
            long cacheHandle = params.pipelineCache();
            int res = VK10.vkCreateGraphicsPipelines(device, cacheHandle, pipelineInfo, null, pPipeline);
            if (res != VK10.VK_SUCCESS) {
                throw new RuntimeException("vkCreateGraphicsPipelines failed with error code: " + res);
            }

            long pipeline = pPipeline.get(0);
            LOGGER.info("[Vulkairis] Created native VkPipeline 0x{} from VkShaderModules [vert: 0x{}, frag: 0x{}]",
                    Long.toHexString(pipeline),
                    Long.toHexString(params.vertShaderModule()),
                    Long.toHexString(params.fragShaderModule()));
            return pipeline;
        }
    }

    /**
     * Convenience method using the active logical device and pipeline cache.
     */
    public static long createGraphicsPipeline(PipelineParams params) {
        return createGraphicsPipeline(VulkanDevice.getDevice(), params);
    }

    /**
     * Convenience method creating a pipeline for a VulkairisProgram.
     */
    public static long createGraphicsPipeline(VulkairisProgram program, long renderPass,
                                             VertexFormat vertexFormat, boolean depthTest,
                                             boolean depthWrite, boolean cull, boolean blend) {
        VulkairisShader vert = program.getVertexShader();
        VulkairisShader frag = program.getFragmentShader();
        if (vert == null || frag == null) {
            throw new IllegalArgumentException("Program '" + program.getName() + "' is missing vertex or fragment shader");
        }

        VkDevice device = VulkanDevice.getDevice();
        long cache = PipelineManager.getOrCreatePipelineCache();

        PipelineParams params = new PipelineParams(
                vert.getVkShaderModule(),
                frag.getVkShaderModule(),
                program.getPipelineLayout(),
                renderPass,
                vertexFormat,
                depthTest,
                depthWrite,
                cull,
                blend,
                cache
        );

        long pipeline = createGraphicsPipeline(device, params);
        PipelineKey key = new PipelineKey(
                vert.getVkShaderModule(),
                frag.getVkShaderModule(),
                vertexFormat != null ? vertexFormat.toString() : "none",
                blend ? 1 : 0,
                depthTest,
                depthWrite,
                cull
        );
        program.cachePipeline(key, pipeline);
        return pipeline;
    }

    public static void destroyPipeline(VkDevice device, long pipeline) {
        if (pipeline != 0L && VulkanDevice.isLive(device)) {
            try {
                VK10.vkDestroyPipeline(device, pipeline, null);
                LOGGER.info("[Vulkairis] Destroyed VkPipeline 0x{}", Long.toHexString(pipeline));
            } catch (Throwable t) {
                LOGGER.warn("[Vulkairis] Error destroying VkPipeline 0x{}: {}", Long.toHexString(pipeline), t.getMessage());
            }
        }
    }
}
