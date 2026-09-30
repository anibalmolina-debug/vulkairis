# Vulkairis — Iris ↔ VulkanMod Bridge

Vulkairis is a baseline prototype designed to bridge Iris Shaders and VulkanMod, allowing Iris's GLSL shaders to be compiled to SPIR-V and executed on VulkanMod's renderer.

**Status**: Baseline Prototype / Technical Specification
**Target Stack**: Minecraft 1.21.1, Fabric, Java 21, Iris `1.8.14-beta.1+mc1.21.1`, VulkanMod `0.6.7+1.21.1`.

---

## Step 0 — Investigation Findings

### A. Where does Iris turn patched GLSL into a GL shader object?
- **AST Preprocessing & Transformation**:
  Iris runs incoming shader pack GLSL files through `io.github.douira.glsl_transformer` (ANTLR4-based) via `net.irisshaders.iris.pipeline.transform.TransformPatcher`.
- **Shader Compilation Call Site**:
  Inside `net.irisshaders.iris.gl.program.ProgramBuilder.buildShader(ShaderType type, String name, String source)`, Iris instantiates `new net.irisshaders.iris.gl.shader.GlShader(type, name, source)`.
  The constructor executes private static `createShader(ShaderType, String, String)`:
  - Calls `GlStateManager.glCreateShader(type.id)`
  - Calls `net.irisshaders.iris.gl.shader.ShaderWorkarounds.safeShaderSource(shader, source)` (which delegates directly to LWJGL `GL20C.nglShaderSource`)
  - Calls `GlStateManager.glCompileShader(shader)`
  - Reads the compilation log via `IrisRenderSystem.getShaderInfoLog(shader)`
  - Checks `GlStateManager.glGetShaderi(shader, GL_COMPILE_STATUS)` (throws `ShaderCompileException` if not 1)
- **Program Linking Call Site**:
  `net.irisshaders.iris.gl.shader.ProgramCreator.create(String name, GlShader... shaders)` calls `GlStateManager.glCreateProgram()`, binds attributes, calls `glAttachShader`, `glLinkProgram`, and checks `glGetProgrami(program, GL_LINK_STATUS)`.
- **Interception Point**:
  We hook into `net.irisshaders.iris.gl.shader.GlShader.createShader` and `net.irisshaders.iris.gl.shader.ProgramCreator.create` via Mixin. This intercepts the fully transformed GLSL source strings directly after Iris's AST patching and before any raw OpenGL/GL20C calls execute.

### B. Does VulkanMod already have a GL-call shim for shader compilation, or not?
- VulkanMod includes a compatibility layer (`net.vulkanmod.gl.VkGlShader` and `net.vulkanmod.gl.VkGlProgram`).
- However, `VkGlShader.glCompileShader(int)` is an empty NO-OP (`return;`), and `VkGlShader.glGetShaderi(int, int)` **unconditionally returns `0`** (which causes Iris's compile status check to fail immediately).
- Furthermore, VulkanMod's compatibility mixins (`GlStateManagerM`, `GL11M`, `GL15M`, `GL30M`) do **not** redirect `glCreateShader`, `glCompileShader`, `glGetShaderi`, `glShaderSource`, `glAttachShader`, `glLinkProgram`, or `glGetProgrami`.
- In vanilla Minecraft/LWJGL, these methods call native LWJGL `GL20C` functions directly. Because VulkanMod replaces the OpenGL renderer with a Vulkan swapchain without an active OpenGL context, calling raw GL functions results in null function pointer crashes or silent failures.
- **Decision**:
  We **hook upstream in Iris**, intercepting the shader sources before they reach raw OpenGL calls, compiling them in-process to SPIR-V using Shaderc, and creating `VkShaderModule` handles registered in our bridge.

---

## Baseline Prototype Architecture

1. **Interception**:
   - `GlShaderMixin` redirects `GlShader.createShader(type, name, source)` to `SpirvCompiler.compileAndRegister(name, source, stage)`.
   - `ProgramCreatorMixin` intercepts `ProgramCreator.create(name, shaders)` to track and link vertex/fragment modules.
2. **GLSL → SPIR-V Translation (in-process Shaderc)**:
   - Uses LWJGL `org.lwjgl.util.shaderc.Shaderc`.
   - Inverts Y axis automatically via `shaderc_compile_options_set_invert_y(options, true)`.
   - Target environment: Vulkan 1.2 / 1.1.
3. **Vulkan Shader & Pipeline Registry**:
   - `VulkanShaderRegistry` creates `VkShaderModule` on VulkanMod's active `VkDevice` (`net.vulkanmod.vulkan.Vulkan.getVkDevice()`).
   - Maps virtual GL shader/program IDs to Vulkan handles.
4. **Coordinate System Correction**:
   - Depth range correction: OpenGL $[-1, 1]$ clip space to Vulkan $[0, 1]$ clip space via matrix multiplication:
     $$\begin{pmatrix} 1 & 0 & 0 & 0 \\ 0 & 1 & 0 & 0 \\ 0 & 0 & 0.5 & 0.5 \\ 0 & 0 & 0 & 1 \end{pmatrix}$$
5. **Test Draw Call**:
   - `BridgeTestRenderer` issues a single test quad draw call inside VulkanMod's active frame render pass to verify end-to-end execution without Vulkan validation layer errors.
