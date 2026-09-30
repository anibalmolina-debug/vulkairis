package com.vulkairis.vulkan.pipeline;

/**
 * Unique cache key identifying a Vulkan Graphics Pipeline by its shader modules,
 * vertex format, blend mode, depth state, and culling state.
 */
public record PipelineKey(
        long vertShaderModule,
        long fragShaderModule,
        String vertexFormatName,
        int blendModeOrdinal,
        boolean depthTest,
        boolean depthMask,
        boolean cull
) {}
