# Graph Report - vulkairis-template-1.21.1  (2026-10-03)

## Corpus Check
- 128 files · ~66,730 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 11 file(s) not represented in the graph (top: (none) 6, .properties 2, .salive 1)

## Summary
- 1335 nodes · 3469 edges · 79 communities (29 shown, 50 thin omitted)
- Extraction: 92% EXTRACTED · 8% INFERRED · 0% AMBIGUOUS · INFERRED: 282 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `7fc175d0`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- org.spongepowered.asm.mixin.Overwrite
- ShaderReflection
- ManagedResource
- VulkanFramebuffer
- .getInstance
- StandaloneVulkanTest
- VulkairisPreLaunch.java
- org.joml.Matrix4f
- collections
- .getDevice
- .preprocessGlsl
- ProgramCreatorMixin.java
- StandaloneVulkanTest.java
- GLSLParserMixin
- org.slf4j.Logger
- .validate
- org.spongepowered.asm.mixin.injection.Inject
- VulkairisProgram
- VulkairisShader
- What You Must Do When Invoked
- VulkanDevice
- VulkairisConfig
- RenderStateTrackerTest.java
- GlStateManagerMixin
- ShaderLifecycleState
- VulkanDescriptorManager
- org.spongepowered.asm.mixin.Mixin
- Baseline Prototype Architecture
- SodiumProgramsMixin.java
- org.junit.jupiter.api.Test
- .link
- ProgramUniformsBuilderMixin.java
- VulkairisRenderSystemMixin.java
- .log
- SpirvValidator
- VulkairisController
- VulkanCapabilities
- VulkanModProgramBridge.java
- RendererMixin.java
- VulkanInstance
- .remapDescriptorBindings
- org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
- GlResourceMixin
- Category
- GlResourceMixin.java
- VulkairisCommands.java
- .testDefaultInitializedUniformsCompile
- GLDebugMixin
- PassType
- RenderTargetExtMixin
- IrisRenderSystemMixin.java
- RenderBackend
- graphify reference: extra exports and benchmark
- .compileShader
- .main
- ReflectionValidationTest.java
- VulkairisDataGenerator.java
- net.vulkanmod.vulkan.texture.VulkanImage
- .glLinkProgram
- .analyzeAndTest
- org.spongepowered.asm.mixin.injection.callback.CallbackInfo
- VulkairisLifecycle
- org.apache.logging.log4j.Logger
- graphify reference: add a URL and watch a folder
- graphify reference: commit hook and native CLAUDE.md integration
- graphify reference: incremental update and cluster-only
- .injectLayoutLocations
- graphify reference: GitHub clone and cross-repo merge
- AGENTS.md
- extraction-spec.md
- .ensureDescriptorBindings
- DebugOverlay.java
- VulkanInstance.java
- VulkanSampler

## God Nodes (most connected - your core abstractions)
1. `IrisRenderSystemMixin` - 81 edges
2. `VulkairisShader` - 52 edges
3. `ShaderReflection` - 41 edges
4. `GlStateManagerMixin` - 35 edges
5. `VulkairisProgram` - 35 edges
6. `StandaloneVulkanTest` - 35 edges
7. `DebugRenderMode` - 33 edges
8. `ShaderTranslator` - 30 edges
9. `ShaderLifecycleState` - 28 edges
10. `VulkanCapabilities` - 28 edges

## Surprising Connections (you probably didn't know these)
- `Baseline Prototype Architecture` --references--> `ProgramCreatorMixin`  [INFERRED]
  README.md → src/client/java/com/vulkairis/client/mixin/iris/ProgramCreatorMixin.java
- `Baseline Prototype Architecture` --references--> `BridgeTestRenderer`  [INFERRED]
  README.md → src/client/java/com/vulkairis/client/render/BridgeTestRenderer.java
- `Baseline Prototype Architecture` --references--> `VulkanShaderRegistry`  [INFERRED]
  README.md → src/client/java/com/vulkairis/translation/VulkanShaderRegistry.java
- `BridgeTestRenderer` --references--> `VulkairisShader`  [EXTRACTED]
  src/client/java/com/vulkairis/client/render/BridgeTestRenderer.java → src/client/java/com/vulkairis/translation/VulkairisShader.java
- `ConfigManager` --references--> `VulkairisConfig`  [EXTRACTED]
  src/client/java/com/vulkairis/config/ConfigManager.java → src/client/java/com/vulkairis/config/VulkairisConfig.java

## Import Cycles
- None detected.

## Communities (79 total, 50 thin omitted)

### Community 1 - "ShaderReflection"
Cohesion: 0.08
Nodes (15): For /graphify explain, For /graphify path, graphify reference: query, path, explain, Step 0 — Constrained query expansion (REQUIRED before traversal), Step 1 — Traversal, SamplerBinding, ShaderReflection, StageInput (+7 more)

### Community 2 - "ManagedResource"
Cohesion: 0.07
Nodes (15): ManagedResource, ResourceType, BUFFER, FRAMEBUFFER, OTHER, PROGRAM, SAMPLER, SHADER (+7 more)

### Community 3 - "VulkanFramebuffer"
Cohesion: 0.12
Nodes (3): VulkanFramebuffer, VulkanImageResource, VulkanImageView

### Community 5 - "StandaloneVulkanTest"
Cohesion: 0.11
Nodes (11): DebugRenderMode, DEPTH, FLAT, LIGHTMAP, NO_POST, NORMAL, OFF, SHADER_TEST (+3 more)

### Community 6 - "VulkairisPreLaunch.java"
Cohesion: 0.09
Nodes (5): graphify reference: transcribe video and audio, Step 2.5 - Transcribe video / audio files (only if video files detected), VulkairisMixinExtension, VulkairisMixinPlugin, VulkairisPreLaunch

### Community 7 - "org.joml.Matrix4f"
Cohesion: 0.08
Nodes (5): OpenGLBridge, UniformBridge, RenderMatrices, RenderStateTracker, RenderMatricesTest

### Community 12 - "StandaloneVulkanTest.java"
Cohesion: 0.21
Nodes (3): StageDiagnostics, PipelineManager, VulkanPipelineFactory

### Community 14 - "org.slf4j.Logger"
Cohesion: 0.11
Nodes (7): GlShaderMixin, ProgramMixin, VulkairisClient, CompatibilityManager, ShaderAnalyzer, DiagnosticManager, ShaderModuleFactory

### Community 15 - ".validate"
Cohesion: 0.23
Nodes (3): BindingSpec, DescriptorContractValidator, DescriptorContractTest

### Community 17 - "VulkairisProgram"
Cohesion: 0.12
Nodes (3): VulkairisProgram, ProgramData, VulkanShaderRegistry

### Community 19 - "What You Must Do When Invoked"
Cohesion: 0.08
Nodes (24): For /graphify add and --watch, For /graphify query, For the commit hook and native CLAUDE.md integration, For --update and --cluster-only, /graphify, Honesty Rules, Interpreter guard for subcommands, Part A - Structural extraction for code files (+16 more)

### Community 21 - "VulkairisConfig"
Cohesion: 0.09
Nodes (4): CompatibilityMode, AUTOMATIC, STRICT, VulkairisConfig

### Community 24 - "ShaderLifecycleState"
Cohesion: 0.13
Nodes (11): ShaderLifecycleState, BOUND, COMPILED, DISCOVERED, DRAW_EXECUTED, MODULE_CREATED, OUTPUT_WRITTEN, PIPELINE_CREATED (+3 more)

### Community 25 - "VulkanDescriptorManager"
Cohesion: 0.23
Nodes (4): BridgeTestRenderer, BindingInfo, LayoutKey, VulkanDescriptorManager

### Community 26 - "org.spongepowered.asm.mixin.Mixin"
Cohesion: 0.23
Nodes (3): ExtendedShaderMixin, PBRTextureManagerMixin, ShaderPackScreenMixin

### Community 27 - "Baseline Prototype Architecture"
Cohesion: 0.50
Nodes (3): Baseline Prototype Architecture, Vulkairis, Vulkairis — Iris ↔ VulkanMod Bridge

### Community 36 - "VulkairisController"
Cohesion: 0.09
Nodes (13): BackendState, BRIDGE_ACTIVE, DEVICE_READY, DISABLED, FAILED, FALLBACK, FALLBACK_GL, INITIALIZING (+5 more)

### Community 37 - "VulkanCapabilities"
Cohesion: 0.09
Nodes (8): EnvironmentReport, DeviceType, CPU, DISCRETE_GPU, INTEGRATED_GPU, OTHER, VIRTUAL_GPU, VulkanCapabilities

### Community 44 - "Category"
Cohesion: 0.17
Nodes (9): Category, COMPATIBILITY, FALLBACK, FRAMEBUFFER, PIPELINE, RESOURCE, SHADER, VULKAN (+1 more)

### Community 49 - "PassType"
Cohesion: 0.32
Nodes (5): PassType, COLOR_DEPTH_SCENE, FINAL_COMPOSITE, SHADER_INTERMEDIATE, VulkanRenderPassManager

### Community 53 - "graphify reference: extra exports and benchmark"
Cohesion: 0.22
Nodes (8): graphify reference: extra exports and benchmark, Step 6b - Wiki (only if --wiki flag), Step 7 - Neo4j export (only if --neo4j or --neo4j-push flag), Step 7a - FalkorDB export (only if --falkordb or --falkordb-push flag), Step 7b - SVG export (only if --svg flag), Step 7c - GraphML export (only if --graphml flag), Step 7d - MCP server (only if --mcp flag), Step 8 - Token reduction benchmark (only if total_words > 5000)

### Community 54 - ".compileShader"
Cohesion: 0.22
Nodes (3): PipelineCompatibilityValidator, PipelineRegressionTest, ReflectionValidationTest

### Community 55 - ".main"
Cohesion: 0.15
Nodes (4): RenderStateTrackerTest, ShaderAnalyzerTest, ShaderLifecycleTest, VulkairisTestRunner

### Community 59 - ".glLinkProgram"
Cohesion: 0.50
Nodes (3): A. Where does Iris turn patched GLSL into a GL shader object?, B. Does VulkanMod already have a GL-call shim for shader compilation, or not?, Step 0 — Investigation Findings

### Community 62 - ".analyzeAndTest"
Cohesion: 0.24
Nodes (5): AnalysisResult, Status, ERROR, PASS, UNSUPPORTED

### Community 67 - "org.apache.logging.log4j.Logger"
Cohesion: 0.13
Nodes (4): PipelineManagerMixin, ShaderWorkaroundsMixin, StandardMacrosMixin, GlUtilMixin

### Community 68 - "graphify reference: add a URL and watch a folder"
Cohesion: 0.50
Nodes (3): For /graphify add, For --watch, graphify reference: add a URL and watch a folder

### Community 69 - "graphify reference: commit hook and native CLAUDE.md integration"
Cohesion: 0.50
Nodes (3): For git commit hook, For native CLAUDE.md integration, graphify reference: commit hook and native CLAUDE.md integration

### Community 70 - "graphify reference: incremental update and cluster-only"
Cohesion: 0.50
Nodes (3): For --cluster-only, For --update (incremental re-extraction), graphify reference: incremental update and cluster-only

## Knowledge Gaps
- **102 isolated node(s):** `PASS`, `UNSUPPORTED`, `ERROR`, `AUTOMATIC`, `STRICT` (+97 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 289 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **50 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `VulkairisShader` connect `VulkairisShader` to `.createGraphicsPipeline`, `ShaderReflection`, `StandaloneVulkanTest`, `VulkanModProgramBridge.java`, `.getDevice`, `StandaloneVulkanTest.java`, `org.slf4j.Logger`, `VulkairisProgram`, `.main`, `.compileShader`, `ShaderLifecycleState`, `ReflectionValidationTest.java`, `VulkanDescriptorManager`, `.link`?**
  _High betweenness centrality (0.071) - this node is a cross-community bridge._
- **Why does `VulkairisController` connect `VulkairisController` to `.log`, `.getInstance`, `VulkanCapabilities`, `ProgramCreatorMixin.java`, `DebugOverlay.java`, `StandaloneVulkanTest.java`, `org.slf4j.Logger`, `VulkairisCommands.java`?**
  _High betweenness centrality (0.058) - this node is a cross-community bridge._
- **Why does `StandaloneVulkanTest` connect `StandaloneVulkanTest` to `.createGraphicsPipeline`, `RendererMixin.java`, `.getDevice`, `StandaloneVulkanTest.java`, `org.slf4j.Logger`, `VulkairisCommands.java`, `VulkairisShader`, `org.junit.jupiter.api.Test`?**
  _High betweenness centrality (0.049) - this node is a cross-community bridge._
- **What connects `PASS`, `UNSUPPORTED`, `ERROR` to the rest of the system?**
  _102 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `org.spongepowered.asm.mixin.Overwrite` be split into smaller, more focused modules?**
  _Cohesion score 0.05157894736842105 - nodes in this community are weakly interconnected._
- **Should `ShaderReflection` be split into smaller, more focused modules?**
  _Cohesion score 0.08020050125313283 - nodes in this community are weakly interconnected._
- **Should `ManagedResource` be split into smaller, more focused modules?**
  _Cohesion score 0.06666666666666667 - nodes in this community are weakly interconnected._