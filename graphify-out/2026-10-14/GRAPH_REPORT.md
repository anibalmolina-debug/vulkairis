# Graph Report - vulkairis-template-1.21.1  (2026-10-14)

## Corpus Check
- 351 files · ~194,889 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 33 file(s) not represented in the graph (top: .jar 21, (none) 6, .properties 2)

## Summary
- 4302 nodes · 13184 edges · 194 communities (98 shown, 96 thin omitted)
- Extraction: 87% EXTRACTED · 13% INFERRED · 0% AMBIGUOUS · INFERRED: 1754 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `45a0e44e`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- org.spongepowered.asm.mixin.Overwrite
- GuiViewModel
- ManagedResource
- VulkanImageResource
- PipelineManager
- StandaloneVulkanTest
- assertions
- org.joml.Matrix4f
- SpirvReflector
- PackReadinessState
- .remapDescriptorBindings
- arraylist
- TextureResource
- GLSLParserMixin
- .of
- .forGraphics
- GlslToken
- VirtualGlFacade
- BridgeTestRenderer
- ProgramTranslator
- StandaloneVulkanTest.java
- VulkairisConfig
- org.slf4j.Logger
- GlStateManagerMixin
- VulkairisProgram
- .translateProgram
- .getActiveCapabilities
- RenderGraph
- PackChainPlan
- VulkanModProgramBridge
- ProgramUniformsBuilderMixin.java
- ShaderPackBrowserScreen
- VulkanModRenderStage
- map
- ShaderLifecycleState
- BackendState
- FrameState
- .tokenize
- org.spongepowered.asm.mixin.injection.Inject
- IncludeResolver
- CompatibilityTestService
- DepthCopyStrategyMixin.java
- .logStructured
- org.spongepowered.asm.mixin.Mixin
- VulkairisArchitectureReport
- RenderResource
- DiagnosticDumps
- VulkanRenderPassManager
- ShaderCompileCache
- ResourceBindings
- IrisBridge
- DiagnosticReportService
- ShaderTranslator
- ShaderReflection
- .getConfig
- VulkairisDataGenerator.java
- Porting Vitrail's Vulkan-shader approach to 1.21.1 (VulkanMod + Iris)
- VulkanCapabilities
- PackTestHistory
- SynchronizationState
- VulkairisPreLaunch.java
- TargetDescriptor
- PassNode
- UniformFrameStore
- org.junit.jupiter.api.Test
- TerrainPipelineDoor
- AGENTS.md
- UniformBridge
- SourceProvenance
- ColorSpacePolicy
- .analyzeAndTest
- ShaderTestPack
- ShaderPackService
- PresentationTarget
- PassExecutionRecord
- ResourceUsage
- VulkairisController
- ProgramUniformStore
- LegacyKeywordPass
- LegacyKeywordPassTest.java
- IrisPackFiles
- ShadowRenderer
- Parser
- VulkairisRenderState
- ShaderPackProgramCatalog
- AbstractCategoryModel
- UniformInfo
- .execute
- VulkanModTypeNormalizer
- Graphics
- .main
- .depth
- .buildFromPlan
- SodiumTerrainDoorMixin.java
- CompatibilityCategoryModel
- .of
- .compileShader
- .expandEntry
- GpuPhaseConfigTest
- VulkairisFrameCoordinator
- GeneralCategoryModel
- PackSmokeTest
- .createGraphicsPipeline
- ShaderPacksCategoryModel
- VulkanSyncPrimitives
- DescriptorSetConvention
- Override
- .textFor
- DrawCategory
- RenderStateTracker
- .register
- .getDevice
- .takeIfCurrent
- CompiledRenderGraph
- .touch
- ProgramExecutionPlan
- RenderTargetFormat
- .compileGlslToSpirvDetailed
- Status
- VulkanRenderer
- DevGuiViewModel
- FeatureTable
- ShaderWarmup
- BindingSpec
- DiagnosticManagerTest
- Standard
- TranslationCache
- .testRescanReconcilesAddRemoveReplaceDuplicateCorrupt
- Phase
- net.vulkanmod.vulkan.texture.VulkanImage
- .shouldDrawControlled
- AdvancedCategoryModel
- TerrainVertexContract
- FallbackRecord
- TransformLedgerTest
- Std140
- PassState
- GuiCategory
- ToggleState
- CompileHardeningTest
- ResourceKind
- ValueSlider
- SamplerBindingTracker
- .testSamplerParametersAccumulate
- UniformType
- How Vitrail runs OptiFine-format shaders on a Vulkan renderer
- TerrainUniforms
- FrameSyncManager
- VulkanShaderRegistry
- PhaseDTerrainDoorTest
- ShadowFrameLag
- .destroy
- GLDebugMixin
- VulkairisModMenu
- list
- .snapshot
- SamplerMappingTest
- VulkanDevice
- ZipPackFiles
- RegressionCaptures
- Vulkairis.java
- Status
- SpirvValidator
- ValidationSuite
- strip_mixin.java
- RenderPassContract
- SmoothedFloatMixin.java
- PipelineRegistry.java
- GbufferPassConfig
- ResizeCoordinator
- .decide
- ShaderProgramContract
- ExperimentalFlags
- ResolutionStatus
- TransformLedger.java
- ShadercVerification.java
- ExtendedShaderMixin.java
- Status
- .testProgramVariantKeyComposition

## God Nodes (most connected - your core abstractions)
1. `IrisRenderSystemMixin` - 86 edges
2. `VulkairisConfig` - 71 edges
3. `VulkairisShader` - 71 edges
4. `ShaderReflection` - 68 edges
5. `ShaderPackProgramCatalog` - 67 edges
6. `VulkairisController` - 55 edges
7. `ShaderPacksCategoryModel` - 53 edges
8. `PassNode` - 52 edges
9. `ShaderPackProgram` - 49 edges
10. `ShaderTranslator` - 48 edges

## Surprising Connections (you probably didn't know these)
- `0. Constraints (non-negotiable)` --references--> `SodiumProgramsMixin`  [INFERRED]
  VULKAIRIS_VITRAIL_PORT_PLAN.md → src/client/java/com/vulkairis/client/mixin/iris/SodiumProgramsMixin.java
- `2. Phase A — Include resolver (unblocks all pack compiles)` --references--> `IrisBridge`  [INFERRED]
  VULKAIRIS_VITRAIL_PORT_PLAN.md → src/client/java/com/vulkairis/iris/IrisBridge.java
- `3. Phase B — Translation parity (program-together, no per-file bindings)` --references--> `ShaderTranslator`  [INFERRED]
  VULKAIRIS_VITRAIL_PORT_PLAN.md → src/client/java/com/vulkairis/translation/ShaderTranslator.java
- `9. Risks` --references--> `VulkanPipelineFactory`  [INFERRED]
  VULKAIRIS_VITRAIL_PORT_PLAN.md → src/client/java/com/vulkairis/vulkan/pipeline/VulkanPipelineFactory.java
- `7. Phase F — GUI close-out (plan Rev 2 remainder)` --references--> `AdvancedCategoryModel`  [INFERRED]
  VULKAIRIS_VITRAIL_PORT_PLAN.md → src/client/java/com/vulkairis/client/gui/AdvancedCategoryModel.java

## Import Cycles
- None detected.

## Communities (194 total, 96 thin omitted)

### Community 1 - "GuiViewModel"
Cohesion: 0.07
Nodes (11): GuiViewModel, NumericRange, Kind, ACTION, SELECT, SLIDER, STATUS, TOGGLE (+3 more)

### Community 2 - "ManagedResource"
Cohesion: 0.07
Nodes (14): ManagedResource, ResourceType, BUFFER, FRAMEBUFFER, OTHER, PROGRAM, SAMPLER, SHADER (+6 more)

### Community 3 - "VulkanImageResource"
Cohesion: 0.13
Nodes (3): VulkanFramebuffer, VulkanImageResource, VulkanImageView

### Community 5 - "StandaloneVulkanTest"
Cohesion: 0.11
Nodes (11): DebugRenderMode, DEPTH, FLAT, LIGHTMAP, NO_POST, NORMAL, OFF, SHADER_TEST (+3 more)

### Community 6 - "assertions"
Cohesion: 0.10
Nodes (5): ShaderCompileResult, SpirvCompiler, VulkairisShader, PipelineCompatibilityValidator, 4. Phase C — Compile path hardening (stack, cache, stages)

### Community 7 - "org.joml.Matrix4f"
Cohesion: 0.15
Nodes (3): OpenGLBridge, RenderMatrices, RenderMatricesTest

### Community 8 - "SpirvReflector"
Cohesion: 0.17
Nodes (4): State, SpirvReflector, State, Variable

### Community 9 - "PackReadinessState"
Cohesion: 0.11
Nodes (15): PackReadinessState, COMPILED, DEGRADED, DISCOVERED, EXECUTION_READY, INTERFACE_VALIDATED, NO_PACK, REJECTED (+7 more)

### Community 11 - "arraylist"
Cohesion: 0.08
Nodes (3): VulkairisGuiModels, VulkairisConfigManager, TranslationWorker

### Community 12 - "TextureResource"
Cohesion: 0.07
Nodes (9): RenderTargetRegistry, SamplerPolicy, LINEAR_CLAMP, LINEAR_REPEAT, NEAREST_CLAMP, NEAREST_REPEAT, SHADOW_COMPARE, TextureResource (+1 more)

### Community 14 - ".of"
Cohesion: 0.20
Nodes (7): CapabilityGate, GateVerdict, PackVerdict, ACCEPT, ACCEPT_PARTIAL, REJECT, CapabilityGateTest

### Community 15 - ".forGraphics"
Cohesion: 0.26
Nodes (3): DescriptorBinding, DescriptorSetLayoutBuilder, DescriptorSetLayoutTest

### Community 16 - "GlslToken"
Cohesion: 0.06
Nodes (14): LegacyGlsl, SplitLeaf, GlslToken, Kind, COMMENT_BLOCK, COMMENT_LINE, DIRECTIVE, IDENTIFIER (+6 more)

### Community 17 - "VirtualGlFacade"
Cohesion: 0.11
Nodes (3): VirtualGlFacade, LinkResult, VirtualGlStatusTest

### Community 19 - "ProgramTranslator"
Cohesion: 0.10
Nodes (8): LegacyResult, CacheEntry, ProgramTranslator, StageDraft, StageUnit, TranslatedProgram, TranslatedStage, ProgramTranslatorTest

### Community 20 - "StandaloneVulkanTest.java"
Cohesion: 0.08
Nodes (4): VulkanBackendAdapter, StageDiagnostics, ComputePipelineFactory, Key

### Community 21 - "VulkairisConfig"
Cohesion: 0.05
Nodes (10): CompatibilityMode, AUTOMATIC, STRICT, DiagnosticVerbosity, BASIC, DETAILED, DEVELOPER, VulkairisConfig (+2 more)

### Community 22 - "org.slf4j.Logger"
Cohesion: 0.08
Nodes (9): GlShaderMixin, ProgramCreatorMixin, VulkairisClient, CompatibilityManager, ShaderAnalyzer, ConfigManager, DiagnosticManager, RenderHookGuard (+1 more)

### Community 23 - "GlStateManagerMixin"
Cohesion: 0.07
Nodes (4): A. Where does Iris turn patched GLSL into a GL shader object?, B. Does VulkanMod already have a GL-call shim for shader compilation, or not?, Step 0 — Investigation Findings, GlStateManagerMixin

### Community 24 - "VulkairisProgram"
Cohesion: 0.13
Nodes (3): VulkairisProgram, VulkanProgramLinker, PipelineKey

### Community 25 - ".translateProgram"
Cohesion: 0.15
Nodes (4): BufferedSource, TranslationOutcome, UnifiedTranslationService, UnifiedTranslationServiceTest

### Community 28 - "RenderGraph"
Cohesion: 0.14
Nodes (4): Access, RenderGraph, PassNodeConfigTest, RenderGraphTest

### Community 29 - "PackChainPlan"
Cohesion: 0.11
Nodes (4): DrawStep, PackChainPlan, TargetSlot, PhaseEChainFallbackTest

### Community 33 - "VulkanModRenderStage"
Cohesion: 0.07
Nodes (21): RenderHookDescriptor, RenderLifecycleTable, StageObservation, VulkanModLifecycleAdapter, VulkanModRenderStage, CHUNK_SUBMISSION, COMPOSITE_POST, CUTOUT_TERRAIN (+13 more)

### Community 34 - "map"
Cohesion: 0.13
Nodes (3): StandardUniforms, FramebufferValidator, DescriptorContractValidator

### Community 35 - "ShaderLifecycleState"
Cohesion: 0.10
Nodes (12): ShaderLifecycleState, BOUND, COMPILED, DISCOVERED, DRAW_EXECUTED, MODULE_CREATED, OUTPUT_WRITTEN, PIPELINE_CREATED (+4 more)

### Community 36 - "BackendState"
Cohesion: 0.12
Nodes (12): BackendState, BRIDGE_ACTIVE, DEVICE_READY, DISABLED, FAILED, FALLBACK, FALLBACK_GL, INITIALIZING (+4 more)

### Community 38 - ".tokenize"
Cohesion: 0.14
Nodes (4): GlslLexer, GlslParseException, TokenStream, TokenStreamTest

### Community 39 - "org.spongepowered.asm.mixin.injection.Inject"
Cohesion: 0.06
Nodes (9): LoadingOverlayDebugMixin, HandPassDebugMixin, PBRTextureManagerMixin, ProgramImagesBuilderMixin, RenderBuffersMixin, ShaderPackScreenMixin, TransformPatcherMixin, RendererMixin (+1 more)

### Community 40 - "IncludeResolver"
Cohesion: 0.11
Nodes (9): PackFilesSnapshot, ExpansionResult, FollowResult, Found, IncludeResolver, MemoEntry, Miss, PackFiles (+1 more)

### Community 41 - "CompatibilityTestService"
Cohesion: 0.05
Nodes (21): CompatibilityFixture, CompatibilityStage, CompatibilityTestReport, CompatibilityTestService, ProgramPair, Progress, StageStatus, FAILED (+13 more)

### Community 44 - ".logStructured"
Cohesion: 0.12
Nodes (10): Category, COMPATIBILITY, FALLBACK, FRAMEBUFFER, PIPELINE, RESOURCE, SHADER, VULKAN (+2 more)

### Community 45 - "org.spongepowered.asm.mixin.Mixin"
Cohesion: 0.08
Nodes (11): MinecraftScreenDebugMixin, GlResourceMixin, PipelineManagerMixin, ProgramMixin, ShaderWorkaroundsMixin, StandardMacrosMixin, RenderTargetExtMixin, GlUtilMixin (+3 more)

### Community 46 - "VulkairisArchitectureReport"
Cohesion: 0.16
Nodes (10): Probe, Status, IMPLEMENTED, NOOP, NOT_WIRED, PARTIAL, PLACEHOLDER, Subsystem (+2 more)

### Community 47 - "RenderResource"
Cohesion: 0.11
Nodes (6): Builder, Kind, COMPUTE, GRAPHICS, TRANSFER, RenderResource

### Community 48 - "DiagnosticDumps"
Cohesion: 0.12
Nodes (16): 0.8.12+v21.0.4.0 — Phases 0–13 implementation baseline, Diagnostics, Known limitations, Supported render stages, Tested dependencies (pinned — do not mix versions), Vulkairis Changelog, Baseline Prototype Architecture, Collecting a diagnostic report (+8 more)

### Community 49 - "VulkanRenderPassManager"
Cohesion: 0.29
Nodes (5): PassType, COLOR_DEPTH_SCENE, FINAL_COMPOSITE, SHADER_INTERMEDIATE, VulkanRenderPassManager

### Community 51 - "ResourceBindings"
Cohesion: 0.14
Nodes (4): FramebufferAttachment, ImageUnitBinding, ResourceBindings, TextureUnitBinding

### Community 53 - "DiagnosticReportService"
Cohesion: 0.15
Nodes (5): DiagnosticReport, DiagnosticReportService, ExportException, ReportSection, DiagnosticReportTest

### Community 55 - "ShaderReflection"
Cohesion: 0.21
Nodes (4): ShaderReflection, StageInput, StageOutput, VertexAttribute

### Community 56 - ".getConfig"
Cohesion: 0.12
Nodes (6): RenderingCategoryModel, ApplyStatus, RendererSettingsService, CategoryModelTest, ConfigSnapshot, VulkairisControllerHolder

### Community 58 - "Porting Vitrail's Vulkan-shader approach to 1.21.1 (VulkanMod + Iris)"
Cohesion: 0.22
Nodes (8): 0. Constraints (non-negotiable), 2. Phase A — Include resolver (unblocks all pack compiles), 3. Phase B — Translation parity (program-together, no per-file bindings), 6. Phase E — Frame orchestration and fallback honesty, 7. Phase F — GUI close-out (plan Rev 2 remainder), 8. Build order and test gates, 9. Risks, Porting Vitrail's Vulkan-shader approach to 1.21.1 (VulkanMod + Iris)

### Community 59 - "VulkanCapabilities"
Cohesion: 0.08
Nodes (13): CompatibilityMatrix, Stability, BROKEN, EXPERIMENTAL, STABLE, UNKNOWN, DeviceType, CPU (+5 more)

### Community 66 - "SynchronizationState"
Cohesion: 0.22
Nodes (4): Barrier, DispatchRequest, SynchronizationState, RenderingStateTest

### Community 67 - "VulkairisPreLaunch.java"
Cohesion: 0.11
Nodes (3): VulkairisMixinExtension, VulkairisMixinPlugin, VulkairisPreLaunch

### Community 68 - "TargetDescriptor"
Cohesion: 0.13
Nodes (12): LoadOp, CLEAR, DONT_CARE, LOAD, Role, COLOR, DEPTH, StoreOp (+4 more)

### Community 70 - "UniformFrameStore"
Cohesion: 0.21
Nodes (5): UniformValue, UniformFrameStore, ProgramPack, UniformPackPlanner, UniformIsolationTest

### Community 71 - "org.junit.jupiter.api.Test"
Cohesion: 0.09
Nodes (3): BoolScopeAndMultiDeclTest, IrisAttributeMappingTest, LayoutLocationInjectionTest

### Community 72 - "TerrainPipelineDoor"
Cohesion: 0.16
Nodes (9): DoorDecision, TerrainPass, SHADOW, SHADOW_CUTOUT, SHADOW_TRANS, TERRAIN, TERRAIN_CUTOUT, TRANSLUCENT (+1 more)

### Community 75 - "SourceProvenance"
Cohesion: 0.19
Nodes (3): SourceProvenance, SourceUnit, SourceProvenanceTest

### Community 76 - "ColorSpacePolicy"
Cohesion: 0.17
Nodes (5): ColorSpacePolicy, Encoding, HDR_LINEAR, LINEAR, SRGB

### Community 77 - ".analyzeAndTest"
Cohesion: 0.11
Nodes (7): AnalysisResult, Status, ERROR, PASS, UNSUPPORTED, ShaderAnalyzerTest, ShaderCompileResultTest

### Community 78 - "ShaderTestPack"
Cohesion: 0.13
Nodes (8): GlslLexException, ShaderTestHarnessTest, PackReport, ShaderPackTestHarness, StageResult, ShaderStageFixture, ShaderTestPack, ShaderTestPacks

### Community 80 - "ShaderPackService"
Cohesion: 0.21
Nodes (3): ScanWarning, ShaderPackService, ShaderPackServiceTest

### Community 82 - "PassExecutionRecord"
Cohesion: 0.10
Nodes (15): Feature, ShaderPackCompatibilityReport, Support, NOT_USED, PARTIAL, SUPPORTED, UNSUPPORTED, Entry (+7 more)

### Community 83 - "ResourceUsage"
Cohesion: 0.08
Nodes (17): RenderGraphException, ResourceTransition, ResourceUsage, COLOR_ATTACHMENT, DEPTH_ATTACHMENT, PRESENT, SAMPLED, STORAGE (+9 more)

### Community 86 - "LegacyKeywordPass"
Cohesion: 0.14
Nodes (4): LegacyKeywordPass, PassContext, PassRegistry, TranslationPass

### Community 87 - "LegacyKeywordPassTest.java"
Cohesion: 0.19
Nodes (3): Builder, SourceMap, TranslationResult

### Community 89 - "ShadowRenderer"
Cohesion: 0.11
Nodes (4): ShadowPassConfig, Result, ShadowProjection, ShadowRenderer

### Community 90 - "Parser"
Cohesion: 0.23
Nodes (4): CustomUniformExpression, ParseFailure, Parser, Result

### Community 92 - "ShaderPackProgramCatalog"
Cohesion: 0.12
Nodes (3): Compute, ShaderPackProgram, ShaderPackProgramCatalog

### Community 94 - "UniformInfo"
Cohesion: 0.24
Nodes (5): PackResult, UniformBlockPacker, UniformBlock, UniformInfo, FramePassStateTest

### Community 95 - ".execute"
Cohesion: 0.11
Nodes (3): ComputeDispatcher, PassBinding, RenderGraphExecutor

### Community 97 - "Graphics"
Cohesion: 0.12
Nodes (5): Graphics, ShaderProgramKind, COMPUTE, GRAPHICS, ShaderPackProgramCatalogTest

### Community 98 - ".main"
Cohesion: 0.07
Nodes (10): DeviceOwnershipTest, DiagnosticHarnessTest, GlslLexerTest, LegacyKeywordPassTest, PipelineKeyTest, PipelineRegressionTest, RenderStateTrackerTest, StandardUniformsTest (+2 more)

### Community 99 - ".depth"
Cohesion: 0.14
Nodes (5): BarrierGenerator, PipelineBarrier, ImageLayoutMapper, ResourceStateTracker, SynchronizationTest

### Community 103 - ".of"
Cohesion: 0.07
Nodes (4): TerrainTargets, TargetViewer, RenderTargetResourceTest, TargetViewerTest

### Community 106 - "GpuPhaseConfigTest"
Cohesion: 0.29
Nodes (3): CompositeChainPlanner, CompositeStep, GpuPhaseConfigTest

### Community 107 - "VulkairisFrameCoordinator"
Cohesion: 0.12
Nodes (5): FrameTrace, PackRenderPipeline, ExecutionReport, VulkairisFrameCoordinator, CoordinatorPinTest

### Community 108 - "GeneralCategoryModel"
Cohesion: 0.20
Nodes (4): GeneralCategoryModel, Check, Report, StartupValidation

### Community 110 - ".createGraphicsPipeline"
Cohesion: 0.13
Nodes (5): GraphicsPipelineMixin, DrawCommand, VertexInputDescription, PipelineParams, VulkanPipelineFactory

### Community 113 - "DescriptorSetConvention"
Cohesion: 0.22
Nodes (4): DescriptorSetConvention, COMPUTE, FRAME, PASS

### Community 114 - "Override"
Cohesion: 0.19
Nodes (4): FolderPackSource, PackReader, PackSource, ZipPackSource

### Community 115 - ".textFor"
Cohesion: 0.22
Nodes (4): DiscoveredPack, EntryText, PackEntry, PackList

### Community 116 - "DrawCategory"
Cohesion: 0.17
Nodes (9): DrawCategory, ENTITIES, GUI, HAND, POST_PROCESS, TERRAIN, WATER, DrawRouter (+1 more)

### Community 119 - ".getDevice"
Cohesion: 0.16
Nodes (6): SamplerKey, VulkanSampler, ComputeGate, BindingInfo, LayoutKey, VulkanDescriptorManager

### Community 120 - ".takeIfCurrent"
Cohesion: 0.24
Nodes (3): SmokeResult, Task, TranslationWorkerTest

### Community 121 - "CompiledRenderGraph"
Cohesion: 0.14
Nodes (3): CompiledRenderGraph, RenderPassScheduler, ResourceLifetime

### Community 123 - "ProgramExecutionPlan"
Cohesion: 0.08
Nodes (13): PassSpec, ProgramExecutionPlan, ProgramResolution, TargetSpec, Gate, DEFERRED_COMPOSITE, FINAL_HUD, FULLSCREEN_COMPOSITION (+5 more)

### Community 124 - "RenderTargetFormat"
Cohesion: 0.12
Nodes (8): RenderTargetFormat, DEPTH24_STENCIL8, DEPTH32F, R11G11B10F, R8, RGBA16F, RGBA32F, RGBA8

### Community 126 - "Status"
Cohesion: 0.09
Nodes (8): Status, FAIL, NEUTRAL, PASS, WARN, StatusBadge, ToggleApplier, ToggleButton

### Community 129 - "FeatureTable"
Cohesion: 0.19
Nodes (8): FeatureRow, FeatureTable, Maturity, EXPERIMENTAL, IMPLEMENTED, TESTED, UNSUPPORTED, FeatureTableTest

### Community 131 - "ShaderWarmup"
Cohesion: 0.28
Nodes (4): Listener, Progress, ShaderWarmup, WarmupEntry

### Community 134 - "Standard"
Cohesion: 0.14
Nodes (11): Cadence, ON_RESIZE, PER_FRAME, PER_PROGRAM, Standard, CAMERA_POSITION, FRAME_INDEX, FRAME_TIME (+3 more)

### Community 137 - "Phase"
Cohesion: 0.13
Nodes (15): Phase, BIND, COMPILE, CONFIG, DRAW, INTERCEPT, LIFECYCLE, LINK (+7 more)

### Community 141 - "TerrainVertexContract"
Cohesion: 0.20
Nodes (3): TerrainAttribute, TerrainVertexContract, 5. Phase D — Sodium/VulkanMod door (targets, descriptors, shadow)

### Community 147 - "GuiCategory"
Cohesion: 0.14
Nodes (9): GuiCategory, ADVANCED, COMPATIBILITY, DIAGNOSTICS, GENERAL, RENDERING, SHADER_PACKS, VulkairisConfigScreen (+1 more)

### Community 150 - "ResourceKind"
Cohesion: 0.20
Nodes (5): ResourceKind, BUFFER, COLOR_IMAGE, DEPTH_IMAGE, STORAGE_IMAGE

### Community 152 - "SamplerBindingTracker"
Cohesion: 0.26
Nodes (6): BindOutcome, ASSIGNED, REBOUND, SAME, Entry, SamplerBindingTracker

### Community 154 - "UniformType"
Cohesion: 0.19
Nodes (9): UniformResourceManager, UniformType, FLOAT, INT, MAT4, VEC2, VEC3, VEC4 (+1 more)

### Community 155 - "How Vitrail runs OptiFine-format shaders on a Vulkan renderer"
Cohesion: 0.17
Nodes (11): 10. What this means for Vulkairis (no action taken), 1. The one structural fact that explains everything, 2. Pack intake: the game folder is the source of truth, 3. `#include`: the exact place our bridge breaks and Vitrail does not, 4. GLSL translation: hand-written, program-together, no bindings emitted, 5. Compile/reflect/cache: the game's shaderc, in-process, 6. Frame: three cuts, Sodium terrain door, shadow one frame late, 7. Uniforms: type-from-declaration, per-frame snapshot (+3 more)

### Community 158 - "VulkanShaderRegistry"
Cohesion: 0.12
Nodes (10): GlObjectState, COMPILED, CREATED, DELETED, FAILED, INVALIDATED, LINKED, ProgramData (+2 more)

### Community 159 - "PhaseDTerrainDoorTest"
Cohesion: 0.11
Nodes (4): Verdict, ShadowCulling, FramebufferValidatorTest, PhaseDTerrainDoorTest

### Community 164 - "list"
Cohesion: 0.09
Nodes (3): IrisShaderMetadata, FrameTraceValidator, PassGates

### Community 169 - "RegressionCaptures"
Cohesion: 0.32
Nodes (5): CaptureSpec, EvidenceStatus, CAPTURED, PENDING, RegressionCaptures

### Community 172 - "Status"
Cohesion: 0.33
Nodes (6): Status, COMPILE_ERROR, REFLECTION_INCOMPLETE, STAGE_REJECTED, SUCCESS, VALIDATION_FAILED

### Community 188 - "ResolutionStatus"
Cohesion: 0.40
Nodes (5): ResolutionStatus, DISABLED, FALLBACK_APPLIED, RESOLVED, UNSUPPORTED

### Community 192 - "Status"
Cohesion: 0.50
Nodes (4): Status, CLOSED, OPEN, PARTIAL

## Knowledge Gaps
- **280 isolated node(s):** `PER_FRAME`, `ON_RESIZE`, `PER_PROGRAM`, `MODEL_VIEW`, `PROJECTION` (+275 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 736 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **96 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `VulkairisResourceManager` connect `org.spongepowered.asm.mixin.Mixin` to `ManagedResource`, `.getInstance`, `org.slf4j.Logger`, `assertions`?**
  _High betweenness centrality (0.047) - this node is a cross-community bridge._
- **Why does `ManagedResource` connect `ManagedResource` to `org.spongepowered.asm.mixin.Mixin`?**
  _High betweenness centrality (0.035) - this node is a cross-community bridge._
- **Why does `VulkairisShader` connect `assertions` to `.destroy`, `ShaderLifecycleState`, `StandaloneVulkanTest`, `.compileShader`, `CompatibilityTestService`, `.createGraphicsPipeline`, `VirtualGlFacade`, `BridgeTestRenderer`, `StandaloneVulkanTest.java`, `ShaderReflection`, `ShaderTranslator`, `org.slf4j.Logger`, `VulkairisProgram`, `.translateProgram`, `VulkanShaderRegistry`, `.compileGlslToSpirvDetailed`, `VulkanModProgramBridge`?**
  _High betweenness centrality (0.028) - this node is a cross-community bridge._
- **What connects `PER_FRAME`, `ON_RESIZE`, `PER_PROGRAM` to the rest of the system?**
  _280 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `org.spongepowered.asm.mixin.Overwrite` be split into smaller, more focused modules?**
  _Cohesion score 0.05432098765432099 - nodes in this community are weakly interconnected._
- **Should `GuiViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.07446808510638298 - nodes in this community are weakly interconnected._
- **Should `ManagedResource` be split into smaller, more focused modules?**
  _Cohesion score 0.0666049953746531 - nodes in this community are weakly interconnected._