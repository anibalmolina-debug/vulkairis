# How Vitrail runs OptiFine-format shaders on a Vulkan renderer

Reference: `LIBS/Vitrail/src` (clone of `github.com/avpbynf/Vitrail-Shaders`, HEAD `f42e548`) and extracted `LIBS/Vitrail/` jar (`vitrail 0.12.0-beta+mc26.3`). Read-only analysis — no Vitrail code was copied into this project. Screenshots in the request (Bliss / BSL / Complementary, Aug 2026) match a working pack pipeline, not mockups.

## 1. The one structural fact that explains everything

Vitrail targets **Minecraft 26.2/26.3, where the game itself ships a native Vulkan renderer** (`VulkanBackend`, `Renderpearl` API; 26.3 splits `com.mojang.blaze3d.*` into `com.mojang.renderpearl.*`, see `versions/26.3.remap`). Vulkairis targets 1.21.1 (OpenGL game) and bridges via Iris 1.8.14 + VulkanMod 0.6.7. Vitrail therefore deletes the entire bridge layer:

- No Iris dependency, no Iris patching (zero `@Mixin` targets in `net.irisshaders.*`).
- No VulkanMod, no LWJGL/shaderc coordinates (resolved from the game jar via ModDev), no `glsl-transformer`/`jcpp`/`antlr`, no Jar-in-Jar, no access wideners, no refmaps.
- Sodium `0.9.x` is a hard dependency (`fabric.mod.json`: `minecraft >=26.3 <26.4`, `sodium 0.9.x`), `compileOnly` in `common/build.gradle` (`transitive=false`), because **Sodium owns Vulkan command submission** (NeoForge toml: "renders through Sodium, which owns the Vulkan command submission").
- Toolchain: Java 25, `net.neoforged.moddev 2.0.147`, no Loom/Yarn, per-game sourcesets `src/mc26.2|mc26.3`, `mergedJar` of fabric+neoforge jars.

## 2. Pack intake: the game folder is the source of truth

- `pack/load/PackLoader.java`: `shaderpacks` dir listing.
- `pack/source/ShaderPackSource.java`: dir-or-`.zip` (zip mounted as `FileSystem`), `shaders/` root search (depth 3), fixed file order, 8 MiB read ceiling, anti-`..` confine, case-insensitive fallback.
- `pack/source/DimensionSet.java`, `ShaderProperties.java`/`PropertiesFile.java` (`shaders.properties` with `#if/ifdef/ifndef/else/elif/endif` live evaluation, profiles, `program.<x>.enabled`, `uniform/variable.*`, `blend.*`, `alphaTest.*`, `shadow*`), `pack/option/OptionIndex.java`/`SettingSet.java`/`OptionRewriter.java`/`EngineDefines.java`.
- `pack/model/ProgramStage.java` (entry by extension; `.glsl` is never an entry), `ProgramNames.java`/`ProgramFallbacks.java`, `pack/program/ProgramSet.java` (enumerate entry points only), `ProgramResolver.java` (per-dimension fallback tree), `TerrainPass.java`/`ChainFilter.java`.

## 3. `#include`: the exact place our bridge breaks and Vitrail does not

Vitrail: `pack/source/IncludeExpander.java` (`#include [<"]...[>"]`), `follow(from, spec)`: spec starting with `/` resolves inside `shaders/` root, otherwise relative to the includer; `ShaderPackSource` strips leading `/`, normalizes, confines to root, tries exact path then case-insensitive fallback; settings-conditional includes, `MAX_DEPTH=32`/`MAX_FILES=20000` bounds, memo per `(settings, entry)`, `#error` on missing/cyclic.

Our bridge (`src/.../translation/ShaderTranslator.java`) has no include resolver; `iris/SourceProvenance.java` assumes Iris owns includes. Any direct pack read hitting `/program/foo.glsl` therefore fails with `#error unexpected include directive` — the current `runclient-fix5.log` failure signature.

## 4. GLSL translation: hand-written, program-together, no bindings emitted

- `glsl/GlslTranslator.java` (token-stream rewrite via `GlslLexer.java`/`TokenStream.java`): deliberately emits **no `layout(binding=)` and no `layout(location=)` except fragment outputs** — the game compiler assigns from SPIR-V reflection; per-file numbering caused collisions.
- `glsl/LegacyGlsl.java`: `gl_*`→`of_*`, `texture2DLod`→`textureLod`, `shadow2D/shadow2DLod/shadow2DProj` wrapped (not renamed), depth/levelled lookups, `gl_TextureMatrix`/`ftransform`/`gl_FragData` mappings, `CORE_MATRICES`, `DynamicTransforms`.
- `glsl/ProgramTranslator.java` (per-program, not per-stage): union `OfGlobals` block + union varyings, `Emitter.java` header (`#version 460 core`, samplers, attributes, varyings, outputs, main wrap), `VertexPrologue`/`GeometryFold`/`VolumeFlattening`/`VaryingSplit`/`PackBuiltins` (MoltenVK helpers)/`VendorExtensions`, `TranslationCache` (SHA-256 of expanded text + vertex format + alpha/coverage + program name + full defines table, 256 MiB ceiling).

## 5. Compile/reflect/cache: the game's shaderc, in-process

- `mixin/GlslCompilerMixin.java` → Mojang `GlslCompiler.compileToSpv` → LWJGL `shaderc_compile_into_spv` (`auto_map_locations`, `gl_VertexID`→`gl_VertexIndex` macros), then `LocalZeroes`/`RawLocals`/`PackNames` patches + `SamplerReach.narrow` before `SPIRVModule`.
- `cache/ModuleCache.java` (per-game): SPIR-V words cached, key = text+stage+defines+mod/game/loader/LWJGL versions, 0.5 GiB/edition, `-Dvitrail.moduleCache=false`.
- Reflection via game `SPIRVModule`/`IntermediaryShaderModule.createFromSpirv` (+ SPIRV-Cross); compute road via `render/PackCompute.java` (`shaderc` kinds, `auto_bind_uniforms`/`auto_map_locations`, opt 2, Vulkan 1.2).
- Pipeline build: `render/GraphicsApi.compile` → `PipelineBuilder` → `CompiledRenderPipeline`, with warmup (`AheadCompiler`, `FamilyWarmup`, `DumpedProgram`) so shaderc never lands on first draw.

## 6. Frame: three cuts, Sodium terrain door, shadow one frame late

- Orchestrator `render/PackChain.java`: `beginFrame/openTargets/drawBegins/drawPrepares/drawBeforeTranslucents/draw/closeFrame`, `beforeLevel/pumpWarmup/warming`, `dispatchShadowCompute/clearCustomImages`.
- 26.3 has a single `LevelPass` on the main target; `render/LevelPass.java` suspends it to create gaps, `render/GeometryHold.java` coalesces consecutive geometry writing the same images into one `vkCmdBeginRendering`.
- Stage order via `platform/EngineStages.java`: `frameGraphSetup` (PBR load, begins, shadow capture, prepares) → mid-level (`afterOpaqueBlocks`: entity opaque features; `afterOpaqueFeatures`: suspend, distant depth, hand solid, before-translucents, feature layer; `afterTranslucentFeatures`: close, read copies) → `afterLevel` (hand translucent, `PackChain.draw()` composites+final or `ChainPresent` into `mainRenderTarget`, then shadow draw for next frame).
- Terrain door: `mixin/sodium/MixinShaderChunkRenderer.java` (`compileProgram` HEAD, cancellable → `TerrainDraw.pipeline()` on the Vulkan road; skips on `DrawBackend.OPENGL`/OIT), `mixin/sodium/MixinDefaultChunkRenderer.java` (swaps handed pass for `GeometryHold.open(TerrainDraw.descriptor())`), `sodium/TerrainMesh.java` (Sodium encoder as black box, appends `BLOCK_ID/MID_TEX_COORD/MID_BLOCK/TANGENT_FRAME/TINT_AND_AO` after 20 B), `sodium/ShadowTerrain.java` (end-of-frame second walk under light frustum; one frame late by design).
- Families: `TerrainDraw/TerrainProgram`, `SkyDraw`, `EntityDraw`, `CloudDraw`, `WeatherDraw`, `ParticleDraw`, `DistantDraw` (+ `dh/DhLods.java` proxy for Distant Horizons, `dh/DhDepth.java` reflection-only), `HandDraw`, `BlockEntityGeometry`; shadow via `ShadowGeometry/ShadowCullPlan/BoxShadowCull/ShadowAmortisation`, forward-Z (`LESS_THAN_OR_EQUAL`, clear 1.0) vs scene reversed-Z.
- The game keeps the swapchain; Vitrail never presents — it writes `final`/`ChainPresent` into `gameRenderer.mainRenderTarget()`.

## 7. Uniforms: type-from-declaration, per-frame snapshot

`render/PackUniforms.java` → `uniform/UniformBlock.java` (type from declaration, so Mellow `bool hideGUI` and Bliss `int hideGUI` both work) + `uniform/UniformCatalog.java` (engine table) layered with `uniform/expr/CustomUniforms.java` (`uniform./variable.` stareval graph, vendored kroppeb); `render/PackValues.java` reads once per pack, `advance()` once per frame, `WorldState` snapshot shared across passes, per-draw `write(std140, world)` into `OfGlobals`.

## 8. Mixins: broad but shallow, no Iris targets

- `vitrail.mixins.json` (~60 client): vanilla feature renderers (entity/block-entity/item/model/text/particle/weather/cloud), `GameRenderer`×4, `GlslCompilerMixin`, `LevelRenderer`×3, `RenderPipeline(Builder)`, `VulkanBackend/Device/CommandEncoder×3/QueueSubmit/RenderPass/RenderPipeline/GpuTextureView`, Sodium narrow wraps (`BlockRenderer`, `ChunkMeshFormats`, `ChunkVertexEncoder.Vertex`, `DefaultChunkRenderer`, `DefaultFluidRenderer`, `RenderRegion`, `ShaderChunkRenderer`, `SodiumWorldRenderer`×2, `VKDrawContext`) with an explicit policy comment that rewriting Sodium culling/draw commands is out of the question; pure `@Accessor/@Invoker` set for game/Sodium internals. `minVersion 0.8.5`, `JAVA_21`, no refmap.
- `vitrail-game.mixins.json` (13): main-pass wrap (`LevelRendererMainPassMixin` → `LevelPass`), `PipelineBuilder`/`RenderSystem` (pipeline-map purge/serve), `SPIRVModule`, sky/hand/transparency.
- `vitrail-fabric.mixins.json` (6): `AfterLevelMixin`, `FrameGraphMixin`, `LevelStagesMixin` (Fabric equivalents of NeoForge frame-graph events), `AtlasStitchedMixin`, `ClientLifecycleMixin`, `ClientSetupMixin` (real setup after boot reload; entrypoint `VitrailFabric` only inits platform + keybinds).
- `vitrail-neoforge.mixins.json`: empty — NeoForge uses events.

## 9. Iris coexistence (not integration)

`IrisBeside` detects `net/irisshaders/iris/Iris.class` + snapshots `preferredGraphicsBackend` from `options.txt`: Vulkan → Vitrail draws; OpenGL + Iris → Iris draws, Vitrail yields (info log, no red chat); otherwise Vitrail warns to pick Vulkan. `HostReport`, `ScreenText` (Iris's pack-screen wording reused word-for-word), and `sodium:config_api_user ConfigEntry` (engine page omitted off-Vulkan; Sodium `filtering_mode` overlay skipped when Iris draws, since Sodium refuses two overlays) implement the truce. `fabric.mod.json` has no Iris entry at all.

## 10. What this means for Vulkairis (no action taken)

The blocking deltas are architectural, not tuning: (a) missing absolute-`/program` include resolver, (b) legacy-builtin coverage emitted against a profile shaderc rejects, (c) per-file binding/location numbering vs Vitrail's emit-none-and-let-reflect-assign, (d) `@Overwrite` on Iris `SodiumPrograms` (fixed signatures, still version-fragile) vs Vitrail's no-Iris narrow Sodium wraps, (e) `MemoryStack` SPIR-V path vs game-owned compile + module/translation caches + warmup. The screenshots show the end state of (a)–(e) done natively; porting any of it is a design decision, explicitly out of scope for this report.
