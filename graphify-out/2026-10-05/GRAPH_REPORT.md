# Graph Report - vulkairis-template-1.21.1  (2026-10-05)

## Corpus Check
- 195 files · ~97,697 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 11 file(s) not represented in the graph (top: (none) 6, .properties 2, .salive 1)

## Summary
- 2194 nodes · 5949 edges · 131 communities (47 shown, 84 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 639 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `7fc175d0`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- org.spongepowered.asm.mixin.Overwrite
- ShaderReflection
- ManagedResource
- VulkanImageResource
- StandaloneVulkanTest.java
- StandaloneVulkanTest
- org.joml.Matrix4f
- SpirvReflector
- .getDevice
- ShaderTranslator
- collections
- GLSLParserMixin
- .getConfig
- .validate
- .generate
- VulkairisProgram
- VulkairisShader
- What You Must Do When Invoked
- loggerfactory
- DebugRenderMode
- VulkairisController
- GlStateManagerMixin
- ShaderLifecycleState
- VulkanDescriptorManager
- .register
- RenderGraph
- org.junit.jupiter.api.Test
- .link
- ProgramUniformsBuilderMixin.java
- PipelineManager
- VulkairisRenderSystemMixin.java
- CompiledRenderGraph
- SpirvValidator
- BackendState
- FrameState
- org.spongepowered.asm.mixin.injection.callback.CallbackInfo
- VulkanInstance
- .remapDescriptorBindings
- DepthCopyStrategyMixin.java
- GlResourceMixin
- DiagnosticManager
- PassNode
- VulkairisArchitectureReport
- .glLinkProgram
- PassType
- RenderTargetExtMixin
- ResourceBindings
- .getInstance
- graphify reference: extra exports and benchmark
- ResourceStateTracker
- RenderStateTracker
- org.spongepowered.asm.mixin.injection.Inject
- VulkairisDataGenerator.java
- net.vulkanmod.vulkan.texture.VulkanImage
- VulkanCapabilities
- ShadowRenderer
- RenderingStateTest
- org.apache.logging.log4j.Logger
- graphify reference: add a URL and watch a folder
- graphify reference: commit hook and native CLAUDE.md integration
- graphify reference: incremental update and cluster-only
- .main
- graphify reference: GitHub clone and cross-repo merge
- AGENTS.md
- extraction-spec.md
- .ensureDescriptorBindings
- ResourceUsage
- .analyzeAndTest
- ShaderTestHarnessTest.java
- RenderGraphTest.java
- ShaderPackCompatibilityReport
- .execute
- ProgramUniformStore
- RenderStateTrackerTest
- org.slf4j.Logger
- RenderTargetFormat
- .of
- TextureResource
- VulkairisRenderState
- ShaderPackProgramCatalog
- SamplerPolicy
- UniformInfo
- PackRenderPipeline
- VulkanModTypeNormalizer
- Graphics
- .translate
- RendererMixin
- RenderBackend
- SodiumProgramsMixin.java
- GLDebugMixin
- org.lwjgl.vulkan.VkDevice
- .compileShader
- .standard
- PresentationTarget
- RenderTargetRegistry
- ResourceAccess
- org.spongepowered.asm.mixin.Mixin
- VulkanFramebuffer
- BridgeTestRenderer
- SynchronizationState
- .testFramebufferAttachmentAndCompleteness
- PassState
- VulkairisMixinPlugin.java
- UniformBridge
- .createGraphicsPipeline
- StandardMacrosMixin.java
- .runStage
- at
- DeviceType
- graphify reference: query, path, explain
- ShaderPackScreenMixin.java
- VulkanSampler
- TranslationPipelineAndReportTest

## God Nodes (most connected - your core abstractions)
1. `IrisRenderSystemMixin` - 85 edges
2. `VulkairisShader` - 52 edges
3. `ShaderReflection` - 50 edges
4. `RenderGraph` - 41 edges
5. `ShaderTranslator` - 40 edges
6. `ShaderPackProgramCatalog` - 38 edges
7. `PassNode` - 38 edges
8. `TextureResource` - 37 edges
9. `ResourceBindings` - 37 edges
10. `VulkairisProgram` - 36 edges

## Surprising Connections (you probably didn't know these)
- `Baseline Prototype Architecture` --references--> `BridgeTestRenderer`  [INFERRED]
  README.md → src/client/java/com/vulkairis/client/render/BridgeTestRenderer.java
- `Baseline Prototype Architecture` --references--> `ProgramCreatorMixin`  [INFERRED]
  README.md → src/client/java/com/vulkairis/client/mixin/iris/ProgramCreatorMixin.java
- `Baseline Prototype Architecture` --references--> `VulkanShaderRegistry`  [INFERRED]
  README.md → src/client/java/com/vulkairis/translation/VulkanShaderRegistry.java
- `UniformBridge` --references--> `RenderMatrices`  [EXTRACTED]
  src/client/java/com/vulkairis/bridge/UniformBridge.java → src/client/java/com/vulkairis/core/RenderMatrices.java
- `BridgeTestRenderer` --references--> `VulkairisShader`  [EXTRACTED]
  src/client/java/com/vulkairis/client/render/BridgeTestRenderer.java → src/client/java/com/vulkairis/translation/VulkairisShader.java

## Import Cycles
- None detected.

## Communities (131 total, 84 thin omitted)

### Community 1 - "ShaderReflection"
Cohesion: 0.13
Nodes (6): SamplerBinding, ShaderReflection, StageInput, StageOutput, VertexAttribute, PipelineCompatibilityValidator

### Community 2 - "ManagedResource"
Cohesion: 0.08
Nodes (15): ManagedResource, ResourceType, BUFFER, FRAMEBUFFER, OTHER, PROGRAM, SAMPLER, SHADER (+7 more)

### Community 8 - "SpirvReflector"
Cohesion: 0.27
Nodes (3): SpirvReflector, State, Variable

### Community 15 - ".validate"
Cohesion: 0.10
Nodes (10): BindingSpec, DescriptorContractValidator, DescriptorSetConvention, COMPUTE, FRAME, PASS, DescriptorBinding, DescriptorSetLayoutBuilder (+2 more)

### Community 19 - "What You Must Do When Invoked"
Cohesion: 0.08
Nodes (24): For /graphify add and --watch, For /graphify query, For the commit hook and native CLAUDE.md integration, For --update and --cluster-only, /graphify, Honesty Rules, Interpreter guard for subcommands, Part A - Structural extraction for code files (+16 more)

### Community 21 - "DebugRenderMode"
Cohesion: 0.06
Nodes (14): CompatibilityMode, AUTOMATIC, STRICT, VulkairisConfig, DebugRenderMode, DEPTH, FLAT, LIGHTMAP (+6 more)

### Community 22 - "VulkairisController"
Cohesion: 0.08
Nodes (3): ConfigManager, VulkairisController, IrisBridge

### Community 24 - "ShaderLifecycleState"
Cohesion: 0.12
Nodes (11): ShaderLifecycleState, BOUND, COMPILED, DISCOVERED, DRAW_EXECUTED, MODULE_CREATED, OUTPUT_WRITTEN, PIPELINE_CREATED (+3 more)

### Community 25 - "VulkanDescriptorManager"
Cohesion: 0.30
Nodes (3): BindingInfo, LayoutKey, VulkanDescriptorManager

### Community 32 - "PipelineManager"
Cohesion: 0.17
Nodes (4): PipelineKey, PipelineManager, VertexInputDescription, VulkanPipelineFactory

### Community 34 - "CompiledRenderGraph"
Cohesion: 0.11
Nodes (3): CompiledRenderGraph, ResourceLifetime, ResourceTransition

### Community 36 - "BackendState"
Cohesion: 0.14
Nodes (12): BackendState, BRIDGE_ACTIVE, DEVICE_READY, DISABLED, FAILED, FALLBACK, FALLBACK_GL, INITIALIZING (+4 more)

### Community 39 - "org.spongepowered.asm.mixin.injection.callback.CallbackInfo"
Cohesion: 0.22
Nodes (3): LoadingOverlayDebugMixin, MinecraftScreenDebugMixin, HandPassDebugMixin

### Community 44 - "DiagnosticManager"
Cohesion: 0.12
Nodes (11): VulkairisLifecycle, Category, COMPATIBILITY, FALLBACK, FRAMEBUFFER, PIPELINE, RESOURCE, SHADER (+3 more)

### Community 45 - "PassNode"
Cohesion: 0.12
Nodes (7): Builder, Kind, COMPUTE, GRAPHICS, TRANSFER, PassNode, RenderResource

### Community 46 - "VulkairisArchitectureReport"
Cohesion: 0.16
Nodes (10): Probe, Status, IMPLEMENTED, NOOP, NOT_WIRED, PARTIAL, PLACEHOLDER, Subsystem (+2 more)

### Community 48 - ".glLinkProgram"
Cohesion: 0.29
Nodes (5): A. Where does Iris turn patched GLSL into a GL shader object?, B. Does VulkanMod already have a GL-call shim for shader compilation, or not?, Step 0 — Investigation Findings, Vulkairis, Vulkairis — Iris ↔ VulkanMod Bridge

### Community 49 - "PassType"
Cohesion: 0.32
Nodes (5): PassType, COLOR_DEPTH_SCENE, FINAL_COMPOSITE, SHADER_INTERMEDIATE, VulkanRenderPassManager

### Community 51 - "ResourceBindings"
Cohesion: 0.11
Nodes (4): ImageUnitBinding, ResourceBindings, SamplerObject, TextureUnitBinding

### Community 53 - "graphify reference: extra exports and benchmark"
Cohesion: 0.22
Nodes (8): graphify reference: extra exports and benchmark, Step 6b - Wiki (only if --wiki flag), Step 7 - Neo4j export (only if --neo4j or --neo4j-push flag), Step 7a - FalkorDB export (only if --falkordb or --falkordb-push flag), Step 7b - SVG export (only if --svg flag), Step 7c - GraphML export (only if --graphml flag), Step 7d - MCP server (only if --mcp flag), Step 8 - Token reduction benchmark (only if total_words > 5000)

### Community 54 - "ResourceStateTracker"
Cohesion: 0.22
Nodes (3): BarrierGenerator, PipelineBarrier, ResourceStateTracker

### Community 62 - "ShadowRenderer"
Cohesion: 0.14
Nodes (4): ShadowPassConfig, Result, ShadowProjection, ShadowRenderer

### Community 67 - "org.apache.logging.log4j.Logger"
Cohesion: 0.10
Nodes (7): graphify reference: transcribe video and audio, Step 2.5 - Transcribe video / audio files (only if video files detected), VulkairisMixinExtension, PipelineManagerMixin, ShaderWorkaroundsMixin, GlUtilMixin, VkGlTextureMixin

### Community 68 - "graphify reference: add a URL and watch a folder"
Cohesion: 0.50
Nodes (3): For /graphify add, For --watch, graphify reference: add a URL and watch a folder

### Community 69 - "graphify reference: commit hook and native CLAUDE.md integration"
Cohesion: 0.50
Nodes (3): For git commit hook, For native CLAUDE.md integration, graphify reference: commit hook and native CLAUDE.md integration

### Community 70 - "graphify reference: incremental update and cluster-only"
Cohesion: 0.50
Nodes (3): For --cluster-only, For --update (incremental re-extraction), graphify reference: incremental update and cluster-only

### Community 71 - ".main"
Cohesion: 0.15
Nodes (3): BoolScopeAndMultiDeclTest, LayoutLocationInjectionTest, VulkairisTestRunner

### Community 76 - "ResourceUsage"
Cohesion: 0.15
Nodes (9): Access, ResourceUsage, COLOR_ATTACHMENT, DEPTH_ATTACHMENT, PRESENT, SAMPLED, STORAGE, TRANSFER_DST (+1 more)

### Community 77 - ".analyzeAndTest"
Cohesion: 0.15
Nodes (6): AnalysisResult, Status, ERROR, PASS, UNSUPPORTED, ShaderAnalyzerTest

### Community 78 - "ShaderTestHarnessTest.java"
Cohesion: 0.31
Nodes (3): ShaderStageFixture, ShaderTestPack, ShaderTestPacks

### Community 80 - "RenderGraphTest.java"
Cohesion: 0.14
Nodes (8): RenderGraphException, ShaderStage, COMPUTE, FRAGMENT, GEOMETRY, TESSELLATION_CONTROL, TESSELLATION_EVALUATION, VERTEX

### Community 81 - "ShaderPackCompatibilityReport"
Cohesion: 0.23
Nodes (7): Feature, ShaderPackCompatibilityReport, Support, NOT_USED, PARTIAL, SUPPORTED, UNSUPPORTED

### Community 82 - ".execute"
Cohesion: 0.21
Nodes (3): ComputeDispatcher, ExecutionReport, RenderGraphExecutor

### Community 87 - "org.slf4j.Logger"
Cohesion: 0.15
Nodes (8): Baseline Prototype Architecture, GlShaderMixin, ProgramCreatorMixin, ProgramMixin, VulkairisClient, CompatibilityManager, ShaderAnalyzer, VulkanShaderRegistry

### Community 88 - "RenderTargetFormat"
Cohesion: 0.05
Nodes (19): GbufferPassConfig, ResourceKind, BUFFER, COLOR_IMAGE, DEPTH_IMAGE, STORAGE_IMAGE, ColorSpacePolicy, Encoding (+11 more)

### Community 92 - "ShaderPackProgramCatalog"
Cohesion: 0.12
Nodes (3): Compute, ShaderPackProgram, ShaderPackProgramCatalog

### Community 93 - "SamplerPolicy"
Cohesion: 0.17
Nodes (6): SamplerPolicy, LINEAR_CLAMP, LINEAR_REPEAT, NEAREST_CLAMP, NEAREST_REPEAT, SHADOW_COMPARE

### Community 94 - "UniformInfo"
Cohesion: 0.25
Nodes (5): PackResult, UniformBlockPacker, UniformBlock, UniformInfo, FramePassStateTest

### Community 95 - "PackRenderPipeline"
Cohesion: 0.16
Nodes (4): BindingResolver, PackRenderPipeline, PassBinding, VulkairisFrameCoordinator

### Community 97 - "Graphics"
Cohesion: 0.12
Nodes (5): Graphics, ShaderProgramKind, COMPUTE, GRAPHICS, ShaderPackProgramCatalogTest

### Community 98 - ".translate"
Cohesion: 0.24
Nodes (4): PassResult, ShaderTranslationPipeline, TranslationResult, ShaderCompilerTest

### Community 103 - "org.lwjgl.vulkan.VkDevice"
Cohesion: 0.22
Nodes (3): ComputePipelineFactory, Key, ShaderModuleFactory

### Community 104 - ".compileShader"
Cohesion: 0.13
Nodes (3): PipelineRegressionTest, ReflectionValidationTest, ShaderLifecycleTest

### Community 109 - "org.spongepowered.asm.mixin.Mixin"
Cohesion: 0.18
Nodes (3): ExtendedShaderMixin, PBRTextureManagerMixin, SmoothedFloatMixin

### Community 121 - ".runStage"
Cohesion: 0.24
Nodes (4): ShaderTestHarnessTest, PackReport, ShaderPackTestHarness, StageResult

### Community 123 - "DeviceType"
Cohesion: 0.33
Nodes (6): DeviceType, CPU, DISCRETE_GPU, INTEGRATED_GPU, OTHER, VIRTUAL_GPU

### Community 124 - "graphify reference: query, path, explain"
Cohesion: 0.33
Nodes (5): For /graphify explain, For /graphify path, graphify reference: query, path, explain, Step 0 — Constrained query expansion (REQUIRED before traversal), Step 1 — Traversal

## Knowledge Gaps
- **150 isolated node(s):** `PASS`, `UNSUPPORTED`, `ERROR`, `SUPPORTED`, `PARTIAL` (+145 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 474 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **84 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `VulkairisRenderState` connect `VulkairisRenderState` to `org.spongepowered.asm.mixin.Overwrite`, `list`, `org.joml.Matrix4f`, `SynchronizationState`, `RenderGraphExecutor.java`, `IrisRenderSystemMixin.java`, `ProgramUniformStore`, `ResourceBindings`, `SynchronizationTest.java`, `ShadowRenderer`?**
  _High betweenness centrality (0.062) - this node is a cross-community bridge._
- **Why does `RenderGraph` connect `RenderGraph` to `list`, `.standard`, `collections`, `PassNode`, `ResourceUsage`, `ResourceAccess`, `RenderGraphTest.java`, `.generate`, `org.slf4j.Logger`, `SynchronizationTest.java`?**
  _High betweenness centrality (0.056) - this node is a cross-community bridge._
- **Why does `ResourceBindings` connect `ResourceBindings` to `collections`, `SynchronizationState`, `.testFramebufferAttachmentAndCompleteness`, `SynchronizationTest.java`, `VulkairisRenderState`?**
  _High betweenness centrality (0.048) - this node is a cross-community bridge._
- **What connects `PASS`, `UNSUPPORTED`, `ERROR` to the rest of the system?**
  _150 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `org.spongepowered.asm.mixin.Overwrite` be split into smaller, more focused modules?**
  _Cohesion score 0.059466848940533154 - nodes in this community are weakly interconnected._
- **Should `ShaderReflection` be split into smaller, more focused modules?**
  _Cohesion score 0.12962962962962962 - nodes in this community are weakly interconnected._
- **Should `ManagedResource` be split into smaller, more focused modules?**
  _Cohesion score 0.07729468599033816 - nodes in this community are weakly interconnected._