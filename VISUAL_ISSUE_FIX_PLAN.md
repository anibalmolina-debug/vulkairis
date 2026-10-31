# Plan: Fixing the Visual Issue (Image 1 → Image 2)

## Goal
Make Minecraft 1.21.1 render with active shader packs (ComplementaryReimagined / BSL / Bliss)
through Vulkairis + VulkanMod 0.6.7 + Iris 1.8.14, instead of falling back to vanilla graphics.

## Current State (after prior work)
- GLSL → SPIR-V compilation, pipeline linking, terrain hook, uniform transport, and auto-engagement
  are implemented and the test suite is green (71/71).
- The composite chain *executes* (passes are recorded) but the screen still shows vanilla because
  intermediate targets (`colortex*`, `ping`/`pong`) are never rendered into and the final image
  is never presented to the swapchain.

## Root Cause
`RenderGraphExecutor.execute(...)` records every pass into the **currently bound render pass**
(VulkanMod's swapchain main pass). It never:
- begins a new render pass for an offscreen target,
- binds that target's framebuffer/color attachment,
- ends the render pass and transitions the image layout.

Consequently `colortex*`/`ping`/`pong` stay uninitialized (zeros), the final pass's fragment
shader samples zero data, and even though a draw is recorded it produces no visible change.
World geometry has already been drawn into the swapchain by VulkanMod before `endRenderPass`,
so the screen shows vanilla terrain.

## Required Architecture (Vitrail-style, adapted to Vulkairis)
```
World pass (VulkanMod, swapchain framebuffer)
  └─ endRenderPass hook
       ├─ for each composite pass:
       │    beginRenderPass(pass framebuffer: colortexN / ping / pong)
       │    bind pipeline + descriptors (inputs = previous targets)
       │    draw fullscreen quad
       │    endRenderPass
       │    transition image layout (color attachment -> shader read)
       └─ final pass:
            (swapchain framebuffer already bound at endRenderPass)
            bind final pipeline (inputs = final_output/last composite)
            draw fullscreen quad  -> writes into swapchain image
```

## Components to Implement

### 1. Offscreen framebuffer acquisition
- Add a helper in `TerrainTargets`/`VulkanResourceBridge` to obtain (or create) a VulkanMod
  `Framebuffer` + `RenderPass` for each registered `TextureResource` target, keyed by target name.
- Cache them in a map on `PackRenderPipeline` (or a new `FramebufferRegistry`) so they survive
  across frames; recreate on resize.
- Forward-Z depth clear value `1.0` for shadow targets; color clear for color targets.

### 2. Per-pass render pass begin/end in `RenderGraphExecutor`
- Before recording a graphics pass whose output target is offscreen:
  - `Renderer.beginRenderPass(framebuffer, clearValues)` (or `VK10.vkCmdBeginRenderPass` with
    the target's render pass + framebuffer).
  - Apply barriers for that pass's inputs.
  - Bind pipeline + descriptors, set viewport/scissor to target dimensions.
  - `vkCmdDraw`.
  - `vkCmdEndRenderPass`.
  - Transition the output image: `COLOR_ATTACHMENT_OPTIMAL -> SHADER_READ_ONLY_OPTIMAL`.
- For the final pass, do NOT begin a new render pass — reuse the already-bound swapchain pass
  (VulkanMod's main pass is active at `endRenderPass`), so the draw writes to the swapchain image.

### 3. Pipeline render-pass compatibility
- `VulkanModProgramBridge.link()` currently builds pipelines with `renderPass = null`.
  VulkanMod's `GraphicsPipeline.createGraphicsPipeline()` uses the render pass from
  `PipelineState.getCurrentPipelineState(boundRenderPass)` at bind time, so pipelines are
  render-pass-agnostic at creation — verify this holds for offscreen framebuffers whose format
  matches (RGBA16F color + DEPTH32F depth). If not, create pipelines per (target format) bucket.
- Ensure `PipelineState` / `PipelineState.getCurrentPipelineState` returns a state whose
  render pass matches the offscreen framebuffer's render pass format (color format, depth).

### 4. Input sampling (descriptor sets)
- `PassBinding` already carries `descriptorSet` with sampler bindings for input targets.
- Ensure each offscreen pass's descriptor set points at the *previous* pass's output image view
  (ping/pong alternation already planned in `CompositeChainPlanner`).
- Add an image-layout transition for inputs: `SHADER_READ_ONLY_OPTIMAL` before sampling.

### 5. Final presentation
- After the final pass draws into the swapchain image, no extra blit is needed (the swapchain
  image is what VulkanMod presents).
- Confirm `Renderer.endFrame()` / present uses the image we wrote to (current image index).

### 6. Fallback & safety
- If any offscreen framebuffer cannot be created, skip the composite chain and log a
  `StageDiagnostics` warning (never crash the frame).
- Keep the vanilla world already drawn as the fallback image if the chain is skipped.

## Files to Touch
- `src/client/java/com/vulkairis/rendering/exec/RenderGraphExecutor.java` — per-pass begin/end render pass + transitions.
- `src/client/java/com/vulkairis/rendering/exec/PackRenderPipeline.java` — framebuffer registry, resize handling.
- `src/client/java/com/vulkairis/bridge/TerrainTargets.java` — framebuffer/render-pass creation helpers.
- `src/client/java/com/vulkairis/bridge/VulkanResourceBridge.java` — VkImageView/VkFramebuffer creation.
- `src/client/java/com/vulkairis/client/mixin/render/RendererMixin.java` — ensure `onExecutePasses` runs inside the swapchain pass (already does).

## Verification
- Automated: keep the 71-test suite green; add tests for framebuffer creation helpers (headless-safe).
- Manual: launch client, enter a world, toggle a shader pack, confirm:
  - Log shows `[Vulkairis/FrameCoordinator] Rebuilt pack render pipeline ... hasGraph=true`.
  - Log shows composite passes recorded without render-pass errors.
  - Screen shows shader features (shadows, water shading, atmosphere) instead of vanilla.

## Risk & Mitigation
- Render-pass format mismatch crashes the GPU → validate color/depth formats against VulkanMod's
  `RenderPass` creation before binding.
- Offscreen framebuffer leaks → tie lifetime to `PackRenderPipeline` and dispose on rebuild.
- Performance: extra render passes per frame → acceptable for a first working implementation;
  optimize (subpasses / merged passes) later.
