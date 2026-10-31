# Graph Report - vulkairis-template-1.21.1  (2026-10-16)

## Corpus Check
- 363 files · ~222,125 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 33 file(s) not represented in the graph (top: .jar 21, (none) 6, .properties 2)

## Summary
- 4557 nodes · 14285 edges · 188 communities (107 shown, 81 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 1947 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `a9f30deb`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- org.spongepowered.asm.mixin.Overwrite
- GuiViewModel
- ManagedResource
- VulkanImageResource
- .getDevice
- StandaloneVulkanTest
- VulkairisShader
- org.joml.Matrix4f
- SpirvReflector
- PackReadinessState
- .remapDescriptorBindings
- PhaseFGuiCloseoutTest.java
- .main
- GLSLParserMixin
- .of
- PhaseDTerrainDoorTest
- LegacyGlsl
- VirtualGlFacade
- BridgeTestRenderer
- ProgramTranslator
- org.slf4j.Logger
- VulkairisConfig
- DiagnosticManager
- GlStateManagerMixin
- VulkanPipelineFactory
- .translateProgram
- SamplerType
- RenderGraph
- PackChainPlan
- VulkanModProgramBridge.java
- SmoothedFloatMixin.java
- ShaderPackBrowserScreen
- VulkanModRenderStage
- CompatibilityTestService
- .link
- BackendState
- FrameState
- .tokenize
- org.spongepowered.asm.mixin.injection.Inject
- IncludeResolver
- .compileGlslToSpirv
- ProgramTranslatorTest
- ProgramMixin.java
- .expandVaryingLeaves
- org.spongepowered.asm.mixin.Mixin
- VulkairisArchitectureReport
- PassNode
- DiagnosticDumps
- VulkanRenderPassManager
- ShaderCompileCache
- ResourceBindings
- FullscreenDepthRootsTest
- DiagnosticReportService
- assertions
- ShaderReflection
- .getConfig
- VulkairisDataGenerator.java
- .vulkairis$terrainCompileHead
- VulkanCapabilities
- PackTestHistory
- SynchronizationState
- VulkairisMixinExtension.java
- TargetDescriptor
- .loadConfig
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
- PackValidationTest
- VulkairisController
- ProgramUniformStore
- LegacyKeywordPass
- Kind
- ShadowRenderer
- Parser
- VulkairisRenderState
- ShaderPackProgramCatalog
- AbstractCategoryModel
- UniformBlock
- org.lwjgl.vulkan.VkCommandBuffer
- VulkanModTypeNormalizer
- ShaderPackProgram
- DeviceOwnershipTest
- ResourceUsage
- PackRenderPipeline
- SodiumProgramsMixin.java
- CompatibilityCategoryModel
- TextureResource
- .compileShader
- .injectLayoutLocations
- GpuPhaseConfigTest
- VulkairisFrameCoordinator
- GeneralCategoryModel
- Stability
- .createGraphicsPipeline
- ShaderPacksCategoryModel
- VulkanSyncPrimitives
- .expandEntry
- DistantHorizonsSupport
- .testSubstituteFinalLabeling
- DrawCategory
- RenderStateTracker
- .register
- VulkanDescriptorManager
- CompatibilityTestReport
- CompiledRenderGraph
- .touch
- ProgramExecutionPlan
- RenderTargetFormat
- Status
- .nextGeneration
- DevGuiViewModel
- FeatureTable
- GpuPhaseConfigTest.java
- ShaderWarmup
- BindingSpec
- DiagnosticManagerTest
- Standard
- PassState
- .testRescanReconcilesAddRemoveReplaceDuplicateCorrupt
- .logStructured
- .testRepairFillsNulls
- LegacyKeywordPassTest.java
- AdvancedCategoryModel
- TerrainVertexContract
- FallbackRecord
- RenderTargetExtMixin
- VulkanRenderer
- ValueSlider
- .validate
- GuiCategory
- ToggleState
- .clearForTests
- DepthCopyStrategyMixin.java
- net.minecraft.client.gui.GuiGraphics
- SamplerBindingTracker
- ShadowCulling
- UniformType
- How Vitrail runs OptiFine-format shaders on a Vulkan renderer
- TerrainUniforms
- FrameSyncManager
- GlObjectState
- .validate
- Vulkairis Implementation Plan — Vitrail-Guided, 1.21.1-Adapted
- VulkairisLifecycle
- DiagnosticVerbosity
- VulkairisModMenu
- list
- .snapshot
- VulkanDevice
- RegressionCaptures
- VulkairisRenderSystemMixin.java
- CompileHardeningTest
- .translate
- ValidationSuite
- strip_mixin.java
- RenderPassContract
- net.vulkanmod.vulkan.texture.VulkanImage
- PipelineRegistry
- ComputePipelineFactory
- Status
- PipelineKeyTest
- .testProgramVariantKeyComposition
- ResolutionStatus

## God Nodes (most connected - your core abstractions)
1. `ShaderReflection` - 88 edges
2. `IrisRenderSystemMixin` - 87 edges
3. `VulkairisShader` - 72 edges
4. `VulkairisConfig` - 71 edges
5. `ShaderPackProgramCatalog` - 71 edges
6. `LegacyGlsl` - 65 edges
7. `ShaderTranslator` - 57 edges
8. `ShaderPacksCategoryModel` - 55 edges
9. `VulkairisController` - 55 edges
10. `ShaderPackProgram` - 54 edges

## Surprising Connections (you probably didn't know these)
- `1. Baseline — what already exists (verified in `src/`)` --references--> `ShaderPacksCategoryModel`  [INFERRED]
  VULKAIRIS_VITRAIL_GUIDED_IMPLEMENTATION_PLAN.md → src/client/java/com/vulkairis/client/gui/ShaderPacksCategoryModel.java
- `2. Phase A — Include resolver (unblocks all pack compiles)` --references--> `IrisBridge`  [INFERRED]
  VULKAIRIS_VITRAIL_PORT_PLAN.md → src/client/java/com/vulkairis/iris/IrisBridge.java
- `3. Phase B — Translation parity (program-together, no per-file bindings)` --references--> `ShaderTranslator`  [INFERRED]
  VULKAIRIS_VITRAIL_PORT_PLAN.md → src/client/java/com/vulkairis/translation/ShaderTranslator.java
- `12. Risks` --references--> `VulkanPipelineFactory`  [INFERRED]
  VULKAIRIS_VITRAIL_GUIDED_IMPLEMENTATION_PLAN.md → src/client/java/com/vulkairis/vulkan/pipeline/VulkanPipelineFactory.java
- `9. Risks` --references--> `VulkanPipelineFactory`  [INFERRED]
  VULKAIRIS_VITRAIL_PORT_PLAN.md → src/client/java/com/vulkairis/vulkan/pipeline/VulkanPipelineFactory.java

## Import Cycles
- None detected.

## Communities (188 total, 81 thin omitted)

### Community 1 - "GuiViewModel"
Cohesion: 0.08
Nodes (11): GuiViewModel, NumericRange, Kind, ACTION, SELECT, SLIDER, STATUS, TOGGLE (+3 more)

### Community 2 - "ManagedResource"
Cohesion: 0.07
Nodes (15): ManagedResource, ResourceType, BUFFER, FRAMEBUFFER, OTHER, PROGRAM, SAMPLER, SHADER (+7 more)

### Community 3 - "VulkanImageResource"
Cohesion: 0.17
Nodes (3): VulkanFramebuffer, VulkanImageResource, VulkanImageView

### Community 4 - ".getDevice"
Cohesion: 0.12
Nodes (3): SamplerKey, VulkanSampler, ComputeGate

### Community 5 - "StandaloneVulkanTest"
Cohesion: 0.11
Nodes (11): DebugRenderMode, DEPTH, FLAT, LIGHTMAP, NO_POST, NORMAL, OFF, SHADER_TEST (+3 more)

### Community 6 - "VulkairisShader"
Cohesion: 0.04
Nodes (21): Baseline Prototype Architecture, ShaderProgramContract, ProgramCreatorMixin, ShaderLifecycleState, BOUND, COMPILED, DISCOVERED, DRAW_EXECUTED (+13 more)

### Community 7 - "org.joml.Matrix4f"
Cohesion: 0.14
Nodes (3): OpenGLBridge, RenderMatrices, RenderMatricesTest

### Community 8 - "SpirvReflector"
Cohesion: 0.21
Nodes (4): State, SpirvReflector, State, Variable

### Community 9 - "PackReadinessState"
Cohesion: 0.11
Nodes (19): PackReadinessState, COMPILED, DEGRADED, DISCOVERED, EXECUTION_READY, INTERFACE_VALIDATED, NO_PACK, REJECTED (+11 more)

### Community 10 - ".remapDescriptorBindings"
Cohesion: 0.12
Nodes (3): BridgeUnionBlockTest, DenseBindingRemapTest, SamplerBindingInjectionTest

### Community 12 - ".main"
Cohesion: 0.08
Nodes (6): DiagnosticHarnessTest, LegacyGlslTest, RenderStateTrackerTest, ShaderTestHarnessTest, StandardUniformsTest, VulkairisTestRunner

### Community 14 - ".of"
Cohesion: 0.16
Nodes (7): CapabilityGate, GateVerdict, PackVerdict, ACCEPT, ACCEPT_PARTIAL, REJECT, CapabilityGateTest

### Community 15 - "PhaseDTerrainDoorTest"
Cohesion: 0.10
Nodes (10): UniformInfo, DescriptorSetConvention, COMPUTE, FRAME, PASS, DescriptorBinding, SwapDecision, TerrainDescriptorSwap (+2 more)

### Community 17 - "VirtualGlFacade"
Cohesion: 0.15
Nodes (3): VirtualGlFacade, ProgramData, VirtualGlStatusTest

### Community 19 - "ProgramTranslator"
Cohesion: 0.14
Nodes (6): LegacyResult, CacheEntry, ProgramTranslator, StageDraft, TranslatedProgram, TranslatedStage

### Community 20 - "org.slf4j.Logger"
Cohesion: 0.09
Nodes (6): VulkanBackendAdapter, VulkairisClient, CompatibilityManager, StageDiagnostics, ShaderCompilerService, ShaderModuleFactory

### Community 22 - "DiagnosticManager"
Cohesion: 0.11
Nodes (4): ShaderAnalyzer, ConfigManager, DiagnosticManager, RenderHookGuard

### Community 23 - "GlStateManagerMixin"
Cohesion: 0.07
Nodes (4): A. Where does Iris turn patched GLSL into a GL shader object?, B. Does VulkanMod already have a GL-call shim for shader compilation, or not?, Step 0 — Investigation Findings, GlStateManagerMixin

### Community 24 - "VulkanPipelineFactory"
Cohesion: 0.18
Nodes (4): TransformLedger, ShadercProfile, PipelineKey, VulkanPipelineFactory

### Community 25 - ".translateProgram"
Cohesion: 0.18
Nodes (4): BufferedSource, TranslationOutcome, UnifiedTranslationService, UnifiedTranslationServiceTest

### Community 27 - "SamplerType"
Cohesion: 0.05
Nodes (25): NumericClass, DEPTH, FLOAT, INT, UINT, SamplerDescriptor, SamplerPlan, SamplerType (+17 more)

### Community 28 - "RenderGraph"
Cohesion: 0.13
Nodes (4): Access, RenderGraph, PassNodeConfigTest, RenderGraphTest

### Community 29 - "PackChainPlan"
Cohesion: 0.14
Nodes (3): DrawStep, PackChainPlan, TargetSlot

### Community 30 - "VulkanModProgramBridge.java"
Cohesion: 0.07
Nodes (5): StandardUniforms, DriverSources, VulkanModProgramBridge, DescriptorContractValidator, DescriptorSetLayoutBuilder

### Community 32 - "ShaderPackBrowserScreen"
Cohesion: 0.13
Nodes (3): PackEntry, PackList, ShaderPackBrowserScreen

### Community 33 - "VulkanModRenderStage"
Cohesion: 0.07
Nodes (21): RenderHookDescriptor, RenderLifecycleTable, StageObservation, VulkanModLifecycleAdapter, VulkanModRenderStage, CHUNK_SUBMISSION, COMPOSITE_POST, CUTOUT_TERRAIN (+13 more)

### Community 34 - "CompatibilityTestService"
Cohesion: 0.15
Nodes (9): CompatibilityFixture, CompatibilityStage, CompatibilityTestService, ProgramPair, StageStatus, FAILED, NOT_SUPPORTED, PARTIAL (+1 more)

### Community 36 - "BackendState"
Cohesion: 0.12
Nodes (12): BackendState, BRIDGE_ACTIVE, DEVICE_READY, DISABLED, FAILED, FALLBACK, FALLBACK_GL, INITIALIZING (+4 more)

### Community 38 - ".tokenize"
Cohesion: 0.10
Nodes (5): GlslLexer, GlslParseException, TokenStream, GlslLexerTest, TokenStreamTest

### Community 39 - "org.spongepowered.asm.mixin.injection.Inject"
Cohesion: 0.06
Nodes (10): HandPassDebugMixin, PBRTextureManagerMixin, PipelineManagerMixin, ProgramImagesBuilderMixin, ProgramUniformsBuilderMixin, RenderBuffersMixin, TransformPatcherMixin, RendererMixin (+2 more)

### Community 40 - "IncludeResolver"
Cohesion: 0.05
Nodes (15): PackFilesSnapshot, IrisPackFiles, FolderPackSource, PackReader, PackSource, ZipPackSource, ExpansionResult, FollowResult (+7 more)

### Community 43 - "ProgramMixin.java"
Cohesion: 0.13
Nodes (3): GlResourceMixin, GlShaderMixin, ProgramMixin

### Community 44 - ".expandVaryingLeaves"
Cohesion: 0.13
Nodes (3): SplitLeaf, StructMember, VaryingDecl

### Community 45 - "org.spongepowered.asm.mixin.Mixin"
Cohesion: 0.07
Nodes (8): LoadingOverlayDebugMixin, MinecraftScreenDebugMixin, ExtendedShaderMixin, GLDebugMixin, ShaderWorkaroundsMixin, StandardMacrosMixin, GlUtilMixin, VkGlTextureMixin

### Community 46 - "VulkairisArchitectureReport"
Cohesion: 0.17
Nodes (10): Probe, Status, IMPLEMENTED, NOOP, NOT_WIRED, PARTIAL, PLACEHOLDER, Subsystem (+2 more)

### Community 47 - "PassNode"
Cohesion: 0.05
Nodes (14): Builder, Kind, COMPUTE, GRAPHICS, TRANSFER, PassNode, ResourceAccess, ShaderStage (+6 more)

### Community 48 - "DiagnosticDumps"
Cohesion: 0.12
Nodes (15): 0.8.12+v21.0.4.0 — Phases 0–13 implementation baseline, Diagnostics, Known limitations, Supported render stages, Tested dependencies (pinned — do not mix versions), Vulkairis Changelog, Collecting a diagnostic report, Current Status (Phase 13) (+7 more)

### Community 49 - "VulkanRenderPassManager"
Cohesion: 0.29
Nodes (5): PassType, COLOR_DEPTH_SCENE, FINAL_COMPOSITE, SHADER_INTERMEDIATE, VulkanRenderPassManager

### Community 51 - "ResourceBindings"
Cohesion: 0.11
Nodes (4): ImageUnitBinding, ResourceBindings, SamplerObject, TextureUnitBinding

### Community 53 - "DiagnosticReportService"
Cohesion: 0.15
Nodes (5): DiagnosticReport, DiagnosticReportService, ExportException, ReportSection, DiagnosticReportTest

### Community 54 - "assertions"
Cohesion: 0.09
Nodes (8): ShaderCompileResult, BindingRemap, ShaderTranslator, SpirvCompiler, TranslationCache, ShadercVerification, 5. Phase C — Compile path hardening (stack, cache, warmup, stages), 4. Phase C — Compile path hardening (stack, cache, stages)

### Community 55 - "ShaderReflection"
Cohesion: 0.08
Nodes (13): PassDescriptorReport, VulkanDescriptorManager, ShaderReflection, StageInput, StageOutput, VertexAttribute, Descriptor, ReflectionData (+5 more)

### Community 56 - ".getConfig"
Cohesion: 0.13
Nodes (6): RenderingCategoryModel, ApplyStatus, RendererSettingsService, CategoryModelTest, ConfigSnapshot, VulkairisControllerHolder

### Community 59 - "VulkanCapabilities"
Cohesion: 0.09
Nodes (9): EnvironmentReport, DeviceType, CPU, DISCRETE_GPU, INTEGRATED_GPU, OTHER, VIRTUAL_GPU, VulkanCapabilities (+1 more)

### Community 62 - "PackTestHistory"
Cohesion: 0.20
Nodes (4): PackSmokeTest, SmokeResult, PackTestHistory, TestMark

### Community 66 - "SynchronizationState"
Cohesion: 0.20
Nodes (4): Barrier, DispatchRequest, SynchronizationState, RenderingStateTest

### Community 67 - "VulkairisMixinExtension.java"
Cohesion: 0.12
Nodes (3): VulkairisMixinExtension, VulkairisMixinPlugin, VulkairisPreLaunch

### Community 68 - "TargetDescriptor"
Cohesion: 0.13
Nodes (12): LoadOp, CLEAR, DONT_CARE, LOAD, Role, COLOR, DEPTH, StoreOp (+4 more)

### Community 70 - "UniformFrameStore"
Cohesion: 0.21
Nodes (5): UniformValue, UniformFrameStore, ProgramPack, UniformPackPlanner, UniformIsolationTest

### Community 71 - "org.junit.jupiter.api.Test"
Cohesion: 0.08
Nodes (5): ControlledDrawGateTest, IrisAttributeMappingTest, LegacyKeywordPassTest, PipelineRegressionTest, TransformLedgerTest

### Community 72 - "TerrainPipelineDoor"
Cohesion: 0.13
Nodes (10): DoorDecision, TerrainPass, SHADOW, SHADOW_CUTOUT, SHADOW_TRANS, TERRAIN, TERRAIN_CUTOUT, TRANSLUCENT (+2 more)

### Community 74 - "UniformBridge"
Cohesion: 0.16
Nodes (3): UniformBridge, UniformBlockPacker, UniformBridgeTest

### Community 75 - "SourceProvenance"
Cohesion: 0.25
Nodes (3): SourceProvenance, SourceUnit, SourceProvenanceTest

### Community 76 - "ColorSpacePolicy"
Cohesion: 0.19
Nodes (5): ColorSpacePolicy, Encoding, HDR_LINEAR, LINEAR, SRGB

### Community 77 - ".analyzeAndTest"
Cohesion: 0.16
Nodes (6): AnalysisResult, Status, ERROR, PASS, UNSUPPORTED, ShaderAnalyzerTest

### Community 78 - "ShaderTestPack"
Cohesion: 0.19
Nodes (4): GlslLexException, ShaderStageFixture, ShaderTestPack, ShaderTestPacks

### Community 80 - "ShaderPackService"
Cohesion: 0.21
Nodes (4): ScanResult, ScanWarning, ShaderPackService, ShaderPackServiceTest

### Community 82 - "PassExecutionRecord"
Cohesion: 0.12
Nodes (14): Feature, ShaderPackCompatibilityReport, Support, NOT_USED, PARTIAL, SUPPORTED, UNSUPPORTED, Entry (+6 more)

### Community 86 - "LegacyKeywordPass"
Cohesion: 0.15
Nodes (4): LegacyKeywordPass, PassContext, PassRegistry, TranslationPass

### Community 88 - "Kind"
Cohesion: 0.20
Nodes (10): Kind, COMMENT_BLOCK, COMMENT_LINE, DIRECTIVE, IDENTIFIER, NEWLINE, NUMBER, STRING (+2 more)

### Community 89 - "ShadowRenderer"
Cohesion: 0.11
Nodes (4): ShadowPassConfig, Result, ShadowProjection, ShadowRenderer

### Community 90 - "Parser"
Cohesion: 0.23
Nodes (4): CustomUniformExpression, ParseFailure, Parser, Result

### Community 92 - "ShaderPackProgramCatalog"
Cohesion: 0.13
Nodes (3): IrisBridge, ShaderPackProgramCatalog, 3. Phase A — Include + pack-intake fidelity (Iris-owned)

### Community 94 - "UniformBlock"
Cohesion: 0.35
Nodes (4): PackResult, UniformBlockPacker, UniformBlock, FramePassStateTest

### Community 97 - "ShaderPackProgram"
Cohesion: 0.08
Nodes (8): Compute, Graphics, ShaderPackProgram, ShaderProgramKind, COMPUTE, GRAPHICS, ShaderPackProgramCatalogTest, TranslationPipelineAndReportTest

### Community 99 - "ResourceUsage"
Cohesion: 0.08
Nodes (14): ResourceUsage, COLOR_ATTACHMENT, DEPTH_ATTACHMENT, PRESENT, SAMPLED, STORAGE, TRANSFER_DST, TRANSFER_SRC (+6 more)

### Community 100 - "PackRenderPipeline"
Cohesion: 0.14
Nodes (4): BindingResolver, PackRenderPipeline, PassBinding, ExecutionReport

### Community 101 - "SodiumProgramsMixin.java"
Cohesion: 0.12
Nodes (10): SodiumProgramsMixin, 0. Constraints (non-negotiable), 2. Phase A — Include resolver (unblocks all pack compiles), 3. Phase B — Translation parity (program-together, no per-file bindings), 5. Phase D — Sodium/VulkanMod door (targets, descriptors, shadow), 6. Phase E — Frame orchestration and fallback honesty, 7. Phase F — GUI close-out (plan Rev 2 remainder), 8. Build order and test gates (+2 more)

### Community 103 - "TextureResource"
Cohesion: 0.06
Nodes (7): TerrainTargets, ResizeCoordinator, RenderTargetRegistry, TargetViewer, TextureResource, RenderTargetResourceTest, TargetViewerTest

### Community 106 - "GpuPhaseConfigTest"
Cohesion: 0.30
Nodes (3): CompositeChainPlanner, CompositeStep, GpuPhaseConfigTest

### Community 107 - "VulkairisFrameCoordinator"
Cohesion: 0.13
Nodes (3): FrameTrace, VulkairisFrameCoordinator, CoordinatorPinTest

### Community 108 - "GeneralCategoryModel"
Cohesion: 0.20
Nodes (4): GeneralCategoryModel, Check, Report, StartupValidation

### Community 109 - "Stability"
Cohesion: 0.28
Nodes (6): CompatibilityMatrix, Stability, BROKEN, EXPERIMENTAL, STABLE, UNKNOWN

### Community 110 - ".createGraphicsPipeline"
Cohesion: 0.13
Nodes (3): GraphicsPipelineMixin, VertexInputDescription, PipelineParams

### Community 111 - "ShaderPacksCategoryModel"
Cohesion: 0.18
Nodes (4): DiscoveredPack, EntryText, ShaderPacksCategoryModel, PackBrowserModelTest

### Community 114 - "DistantHorizonsSupport"
Cohesion: 0.20
Nodes (3): DistantHorizonsSupport, ProgramTranslatorStage, DistantHorizonsDegradeTest

### Community 116 - "DrawCategory"
Cohesion: 0.23
Nodes (8): DrawCategory, ENTITIES, GUI, HAND, POST_PROCESS, TERRAIN, WATER, DrawRouter

### Community 119 - "VulkanDescriptorManager"
Cohesion: 0.25
Nodes (3): BindingInfo, LayoutKey, VulkanDescriptorManager

### Community 120 - "CompatibilityTestReport"
Cohesion: 0.20
Nodes (4): CompatibilityTestReport, Progress, Task, TranslationWorkerTest

### Community 121 - "CompiledRenderGraph"
Cohesion: 0.08
Nodes (5): CompiledRenderGraph, RenderPassScheduler, ResourceLifetime, ResourceTransition, VulkanResourceBridge

### Community 123 - "ProgramExecutionPlan"
Cohesion: 0.08
Nodes (13): PassSpec, ProgramExecutionPlan, ProgramResolution, TargetSpec, Gate, DEFERRED_COMPOSITE, FINAL_HUD, FULLSCREEN_COMPOSITION (+5 more)

### Community 124 - "RenderTargetFormat"
Cohesion: 0.06
Nodes (15): RenderGraphException, ResourceKind, BUFFER, COLOR_IMAGE, DEPTH_IMAGE, STORAGE_IMAGE, GbufferPassConfig, RenderTargetFormat (+7 more)

### Community 126 - "Status"
Cohesion: 0.19
Nodes (6): Status, FAIL, NEUTRAL, PASS, WARN, StatusBadge

### Community 127 - ".nextGeneration"
Cohesion: 0.14
Nodes (3): PbrTextureState, TextureUnitPolicy, SamplerMappingTest

### Community 129 - "FeatureTable"
Cohesion: 0.19
Nodes (8): FeatureRow, FeatureTable, Maturity, EXPERIMENTAL, IMPLEMENTED, TESTED, UNSUPPORTED, FeatureTableTest

### Community 130 - "GpuPhaseConfigTest.java"
Cohesion: 0.07
Nodes (8): RenderResource, SamplerPolicy, LINEAR_CLAMP, LINEAR_REPEAT, NEAREST_CLAMP, NEAREST_REPEAT, SHADOW_COMPARE, TextureSpec

### Community 131 - "ShaderWarmup"
Cohesion: 0.28
Nodes (4): Listener, Progress, ShaderWarmup, WarmupEntry

### Community 134 - "Standard"
Cohesion: 0.14
Nodes (11): Cadence, ON_RESIZE, PER_FRAME, PER_PROGRAM, Standard, CAMERA_POSITION, FRAME_INDEX, FRAME_TIME (+3 more)

### Community 137 - ".logStructured"
Cohesion: 0.07
Nodes (24): Category, COMPATIBILITY, FALLBACK, FRAMEBUFFER, PIPELINE, RESOURCE, SHADER, VULKAN (+16 more)

### Community 138 - ".testRepairFillsNulls"
Cohesion: 0.25
Nodes (3): CompatibilityMode, AUTOMATIC, STRICT

### Community 139 - "LegacyKeywordPassTest.java"
Cohesion: 0.24
Nodes (3): Builder, SourceMap, TranslationResult

### Community 140 - "AdvancedCategoryModel"
Cohesion: 0.17
Nodes (3): AdvancedCategoryModel, ExperimentalFlag, ExperimentalFlags

### Community 144 - "VulkanRenderer"
Cohesion: 0.16
Nodes (3): DrawCommand, RenderBackend, VulkanRenderer

### Community 147 - "GuiCategory"
Cohesion: 0.12
Nodes (10): GuiCategory, ADVANCED, COMPATIBILITY, DIAGNOSTICS, GENERAL, RENDERING, SHADER_PACKS, VulkairisConfigScreen (+2 more)

### Community 151 - "net.minecraft.client.gui.GuiGraphics"
Cohesion: 0.13
Nodes (3): ToggleApplier, ToggleButton, ShaderPackScreenMixin

### Community 152 - "SamplerBindingTracker"
Cohesion: 0.26
Nodes (6): BindOutcome, ASSIGNED, REBOUND, SAME, Entry, SamplerBindingTracker

### Community 154 - "UniformType"
Cohesion: 0.21
Nodes (9): UniformResourceManager, UniformType, FLOAT, INT, MAT4, VEC2, VEC3, VEC4 (+1 more)

### Community 155 - "How Vitrail runs OptiFine-format shaders on a Vulkan renderer"
Cohesion: 0.17
Nodes (11): 10. What this means for Vulkairis (no action taken), 1. The one structural fact that explains everything, 2. Pack intake: the game folder is the source of truth, 3. `#include`: the exact place our bridge breaks and Vitrail does not, 4. GLSL translation: hand-written, program-together, no bindings emitted, 5. Compile/reflect/cache: the game's shaderc, in-process, 6. Frame: three cuts, Sodium terrain door, shadow one frame late, 7. Uniforms: type-from-declaration, per-frame snapshot (+3 more)

### Community 158 - "GlObjectState"
Cohesion: 0.11
Nodes (7): GlObjectState, COMPILED, CREATED, DELETED, FAILED, INVALIDATED, LINKED

### Community 159 - ".validate"
Cohesion: 0.17
Nodes (4): FramebufferValidator, Verdict, FramebufferAttachment, FramebufferValidatorTest

### Community 160 - "Vulkairis Implementation Plan — Vitrail-Guided, 1.21.1-Adapted"
Cohesion: 0.13
Nodes (12): ShadowFrameLag, 0. Version delta — why this is adaptation, not a port, 10. Phase H — GUI / diagnostics close-out, 11. Build order and test gates, 12. Risks, 13. Review checklist (for approver), 1. Baseline — what already exists (verified in `src/`), 2. Gap map — Vitrail capability → Vulkairis work item (+4 more)

### Community 162 - "DiagnosticVerbosity"
Cohesion: 0.25
Nodes (5): DiagnosticVerbosity, BASIC, DETAILED, DEVELOPER, ConfigSnapshot

### Community 164 - "list"
Cohesion: 0.08
Nodes (3): IrisShaderMetadata, FrameTraceValidator, PassGates

### Community 169 - "RegressionCaptures"
Cohesion: 0.32
Nodes (5): CaptureSpec, EvidenceStatus, CAPTURED, PENDING, RegressionCaptures

### Community 173 - ".translate"
Cohesion: 0.57
Nodes (3): PassResult, ShaderTranslationPipeline, TranslationResult

### Community 181 - "Status"
Cohesion: 0.33
Nodes (6): Status, COMPILE_ERROR, REFLECTION_INCOMPLETE, STAGE_REJECTED, SUCCESS, VALIDATION_FAILED

### Community 188 - "ResolutionStatus"
Cohesion: 0.40
Nodes (5): ResolutionStatus, DISABLED, FALLBACK_APPLIED, RESOLVED, UNSUPPORTED

## Knowledge Gaps
- **298 isolated node(s):** `PER_FRAME`, `ON_RESIZE`, `PER_PROGRAM`, `MODEL_VIEW`, `PROJECTION` (+293 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 769 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **81 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `VulkanShaderRegistry` connect `VulkairisShader` to `DeviceOwnershipTest`, `.link`, `list`, `.cleanUp`, `ProgramMixin.java`, `VulkairisArchitectureReport`, `VirtualGlFacade`, `org.slf4j.Logger`, `ShaderReflection`, `DiagnosticManager`, `GlStateManagerMixin`, `assertions`, `.translateProgram`, `GlObjectState`, `VulkanModProgramBridge.java`?**
  _High betweenness centrality (0.029) - this node is a cross-community bridge._
- **Why does `LegacyGlsl` connect `LegacyGlsl` to `list`, `.preprocessGlsl`, `.main`, `.expandVaryingLeaves`, `.testUnionVaryingHelpers`, `ProgramTranslator`, `FullscreenDepthRootsTest`, `assertions`, `VulkanModProgramBridge.java`?**
  _High betweenness centrality (0.025) - this node is a cross-community bridge._
- **Why does `ResourceUsage` connect `ResourceUsage` to `list`, `RenderGraph`, `PassNode`, `CompiledRenderGraph`, `SynchronizationTest.java`, `RenderTargetFormat`?**
  _High betweenness centrality (0.023) - this node is a cross-community bridge._
- **What connects `PER_FRAME`, `ON_RESIZE`, `PER_PROGRAM` to the rest of the system?**
  _298 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `org.spongepowered.asm.mixin.Overwrite` be split into smaller, more focused modules?**
  _Cohesion score 0.05479059093516925 - nodes in this community are weakly interconnected._
- **Should `GuiViewModel` be split into smaller, more focused modules?**
  _Cohesion score 0.0824524312896406 - nodes in this community are weakly interconnected._
- **Should `ManagedResource` be split into smaller, more focused modules?**
  _Cohesion score 0.070578231292517 - nodes in this community are weakly interconnected._