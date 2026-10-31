# Vulkairis Implementation Plan — Vitrail-Guided, 1.21.1-Adapted

Status: **PLAN ONLY — no behavior changes in this file.**
Review gate: do not start coding until this plan is approved.

Source of truth for behavior: this repo on **MC 1.21.1 + Fabric Loader + Loom**.
Guideline reference only (do not copy code from it): `LIBS/Vitrail/src`
(Vitrail `0.12.0-beta+mc26.3`, native-Vulkan game). Prior analysis preserved in
`VITRAIL_VULKAN_REPORT.md` and `VULKAIRIS_VITRAIL_PORT_PLAN.md` — this file is the
buildable successor that accounts for what already exists in `src/`.

Target visuals (user screenshot grid): Bliss / BSL / Complementary working on
Distant Horizon, Landscape, Underwater, Ocean Ruins, Short render distance,
Lush Caves, End, Glowstone portal. That is the Vitrail bar.

---

## 0. Version delta — why this is adaptation, not a port

| Concern | Vitrail (26.2/26.3 guideline) | Vulkairis (1.21.1 reality) |
|---|---|---|
| Renderer owner | Game ships `VulkanBackend` / `Renderpearl`; Sodium `0.9.x` owns command submission | OpenGL game; VulkanMod `0.6.7` owns Vulkan submission; Sodium `0.8.12` era via Iris |
| Pack owner | Vitrail owns `PackLoader` / `ShaderPackSource` / `ProgramSet` / `TargetPlan` / `PackChain` | **Iris `1.8.14-beta.1` owns pack loading, settings, pipeline orchestration.** Vulkairis must not build a second loader |
| GLSL intake | Reads pack files directly, expands `#include`, translates program-together | Intercepts **Iris-transformed** GLSL (`iris/SourceProvenance.java`), routes to VulkanMod via `bridge/VulkanModProgramBridge.java` |
| Binding assignment | Emits no `binding/location` (except frag outputs); game compiler assigns from SPIR-V reflection | VulkanMod Shaderc **requires** explicit `layout(binding)` + vertex/varying `location`. Vulkairis must emit dense `0..N-1` bindings + locations derived from reflection + `DefaultVertexFormat` |
| Compile | Game `GlslCompiler.compileToSpv` in-process + disk `ModuleCache` + `AheadCompiler` warmup | LWJGL `shaderc` in-process (`translation/SpirvCompiler.java`) + `ShaderCompileCache` + `ShaderWarmup` |
| Terrain door | Narrow Sodium `0.9.x` wraps → pack pipeline; mesh `24→40B` extend; shadow second walk, 1 frame late | Narrow Iris `SodiumPrograms` + VulkanMod door; mesh contract `TerrainVertexContract`; shadow via `ShadowRenderer` / `ShadowFrameLag` |
| Mixins | ~60 client + 13 game + 6 fabric, zero Iris targets | Iris + VulkanMod targets unavoidable; prefer `@Inject(cancellable)` / `@WrapOperation` over `@Overwrite` |
| Toolchain | Java 25, ModDev, per-game sourcesets, `mergedJar` | Java 21, Loom, `GRADLE_USER_HOME=C:\gradle-ascii`, JDK 21 for all runs (non-ASCII profile breaks dev launcher) |

Non-negotiable constraints:

1. Stay on 1.21.1 + Loom. No native `VulkanBackend`. VulkanMod remains Vulkan owner, Iris remains pack owner.
2. No second pack loader, no second zip mount. Pack root stays Iris-owned (`iris/IrisBridge.java`).
3. Narrow wraps over `@Overwrite`. Frozen `@Overwrite` signatures stay frozen; all new behavior uses injection.
4. No render-thread blocking for scan/compile/report/IO; publish immutable results (`translation/TranslationWorker.java` pattern).
5. Every GUI control keeps contract: real effect + persisted value + default + validation + tooltip. No decorative controls.
6. Do not copy Vitrail source. Re-implement behaviors clean-room against OptiFine/Iris pack semantics + VulkanMod contracts.
7. Privacy/perf: reports redacted + bounded; never present swapchain; never assume GL state.

---

## 1. Baseline — what already exists (verified in `src/`)

Count: `src/client/java/com/vulkairis` 259 files; `src/main` 2 files.

Implemented (do not rebuild, extend only):

* Translation: `translation/ShaderTranslator.java` (per-stage), `translation/ProgramTranslator.java` (program-together union + binding strip), `translation/IncludeResolver.java` (`#include` expand + `#line` + `#error`), `translation/LegacyGlsl.java`, `translation/SpirvCompiler.java` (VERTEX/FRAGMENT only, malloc handoff, semaphore, `MAX_SOURCE_CHARS=512KiB`), `translation/SpirvReflector.java`, `translation/ShaderCompileCache.java`, `translation/ShaderWarmup.java`, `translation/UnifiedTranslationService.java`, `translation/lexer/*`, `translation/passes/*`.
* Bridge: `bridge/VulkanModProgramBridge.java` (links virtual Iris program → real `GraphicsPipeline`, dense binding remap, vertex-format table, `VkGlProgram` registration). **Documented gap: uniform/UBO values not yet transported.**
* Iris: `iris/IrisBridge.java`, `iris/ShaderPackProgramCatalog.java`, `iris/ShaderPackProgram.java`, `iris/SourceProvenance.java`, `iris/IrisPackFiles.java`.
* Frame: `rendering/exec/PackChainPlan.java` (begins→shadow→prepares→redirects→composites/final or `SUBSTITUTE`), `rendering/exec/VulkairisFrameCoordinator.java`, `rendering/PackRenderPipeline.java`, `rendergraph/*`, `rendering/RenderLifecycleTable.java`.
* Uniforms/state: `bridge/UniformBridge.java`, `bridge/StandardUniforms.java`, `bridge/TerrainUniforms.java`, `bridge/CustomUniformExpression.java`, `rendering/state/Std140.java`, `rendering/state/UniformFrameStore.java`, `rendering/state/UniformPackPlanner.java`, `plan/UniformPlan.java`, `plan/SamplerPlan.java`.
* Terrain door skeleton: `vulkan/pipeline/TerrainPipelineDoor.java` (pure decision), `client/mixin/iris/SodiumTerrainDoorMixin.java` (`@Inject` door).
* GUI (29 files): `client/gui/*` + `client/gui/services/*` (`ShaderPackBrowserScreen`, `ShaderPacksCategoryModel`, `VulkairisConfigScreen`, `ShaderPackService`, `DiagnosticReportService`, `CompatibilityTestService`).
* Diagnostics: `diagnostics/*`, `core/VulkairisController.java` fallback, `rendering/state/*`.

Stubs / thin / explicitly limited:

* `client/mixin/iris/SodiumProgramsMixin.java` — `@Overwrite` stub returning `emptyMap` / `null` to avoid `NoSuchMethodError`. No draw.
* `translation/SpirvCompiler.java` — tess/geom/compute rejected with reason by design.
* `resource/VulkanResourceBridge.java` (`blitFramebuffer`/`generateMipmaps` log-only), `client/iris/VulkanDepthCopyStrategy.java` (throws), `bridge/OpenGLBridge.java` + `bridge/TextureBridge.java` (delegators).
* `VulkanModProgramBridge` vertex format chosen from small `DefaultVertexFormat` table; uncovered inputs fail link by design.

---

## 2. Gap map — Vitrail capability → Vulkairis work item

Behavioral descriptions only. No Vitrail code reproduced.

| # | Vitrail capability (guideline) | Vulkairis status | Work item in this plan |
|---|---|---|---|
| G1 | Absolute-`/` include resolves to `shaders/` root, relative otherwise; confine to root; exact-hit then case-insensitive fallback; bounds; memo per settings+entry; `#error` on missing/cyclic | `IncludeResolver` exists; integration on `/program/*.glsl` fixtures unverified since `fix5` log | §3 Phase A: fixture proof + wiring audit |
| G2 | Program-together union globals/varyings; emit-no-bindings, reflection assigns | `ProgramTranslator` union exists; VulkanMod forces dense bindings + locations instead | §4 Phase B: keep union, formalize VulkanMod dense-binding adaptation as the 1.21.1 equivalent |
| G3 | Legacy coverage: version/precision strip, `texture2D*`→`texture*`, `texture2DLod`→`textureLod`, `shadow2D*` wrap, `gl_FragData`→outputs, `gl_TextureMatrix`/`ftransform`/`gl_Color`/`gl_Normal`, depth-volume conversion, mat/struct/array varying split, fullscreen/entity roots | Partial; `fix5` still failed on `texture2DLod`/`shadow2D`/`gl_FragData`/`gl_TextureMatrix` | §4 Phase B: coverage table + golden fixtures |
| G4 | In-process shaderc + translation/SPIR-V caches + warmup; bounded native handoff | Hardened handoff done; cache is success-only in-memory; warmup exists but pre-draw coverage unverified | §5 Phase C: persistent bounded cache + warmup-before-draw |
| G5 | Sodium terrain door: compile→pack pipeline; render→pack targets; mesh extend `+BLOCK_ID/MID_TEX_COORD/MID_BLOCK/TANGENT_FRAME/TINT_AND_AO`; shadow second walk, 1 frame late | Decision logic only; `@Overwrite` stub draws nothing; mesh/targets/shadow unverified in-game | §6 Phase D: make door actually draw |
| G6 | Per-frame uniform snapshot: type-from-declaration blocks + engine catalog + custom-expression graph; snapshot once, write per-pass `std140` | Stores/planners exist; **values not transported** (bridge javadoc); expression coverage partial | §7 Phase E: value transport + `isEyeInWater`-class parity |
| G7 | Frame orchestration: begins/prepares → feature redirects → composites/final or present-substitute; target/sampler/draw-buffer plans precomputed, replayed | `PackChainPlan` + coordinator exist; sampler/draw-buffer replay + substitute labeling unverified | §8 Phase F: chain replay + honesty |
| G8 | Dimension handling (`world/nether/end` overlay), `CUSTOM_SKY`, End/Nether time/fog, `renderStage` branching | Catalog has program entries; per-dimension fallback + sky/End behavior unverified | §8 Phase F + §9 Phase G |
| G9 | Shadows: `shadowtex0/1/HW`, `shadowcolor0-7`, HW vs arithmetic road, cull plan, translucent split, forward-Z store | `ShadowRenderer`/`ShadowProjection`/`ShadowFrameLag` exist; cull/HW/translucent-split unverified | §8 Phase F |
| G10 | Compute / custom images / SSBO voxel pipe (caves floodfill), PBR (`MC_MIPMAP_LEVEL` etc.), `DISTANT_HORIZONS` + `dh_*` programs + far-terrain proxy | Missing by design (compute rejected); no DH integration | §9 Phase G: staged, all default OFF |
| G11 | GUI close-out: ModMenu, Compatibility model, browser, diagnostics export, advanced flags, localization | Mostly done; remaining: export limits, advanced-only-real-flags, localization/accessibility | §10 Phase H |

Screenshot mapping (what each card needs):

* Distant Horizon Beta (Bliss/BSL) → G10 DH + G9 shadows + G6 fog/uniforms.
* Landscape / Short render distance (Complementary) → G7 chain + G9 shadows + G6 uniforms.
* Underwater / Ocean Ruins (Bliss) → G6 `isEyeInWater`/fog + translucent water road + G9 `watershadow` semantics.
* Lush Caves / Glowstone portal (BSL) → G10 voxel/compute or graceful fallback + G5 emission/AO mesh data + G6 ambient/fog.
* End (Complementary) → G8 dimension + sky + G6 time/uniforms.

---

## 3. Phase A — Include + pack-intake fidelity (Iris-owned)

Goal: `/program/x.glsl` and relative includes resolve exactly like the pack expects, before translation. No second loader.

1. Audit `translation/IncludeResolver.java` against pack semantics:
   * Spec starting with `/` → strip leading `/+`, resolve under Iris pack `shaders/` root; else relative to includer.
   * Normalize, confine to root (reject `..` escape with structured miss, never throw into render).
   * Exact-hit then case-insensitive fallback.
   * Bounds: depth 32, file count 20000, plus per-file byte/line caps.
   * Memo per `(pack generation, entry)`; `#error` naming pack file + reason on missing/cyclic.
   * Preserve `#line` markers so compiler errors map to pack files.
2. Pack-root access stays Iris-owned: resolve root via `IrisBridge` current-pack location. If unavailable, return structured miss.
3. Wire audit: confirm expansion runs on **pre-translation** text in `ShaderTranslator` + any direct-pack read used by `compatibility/ShaderAnalyzer` / `IrisBridge.validateGraphicsProgram`.
4. Tests: unit (absolute/relative/case-insensitive/missing/cyclic/escape) + integration on fixture zip containing `/program/*.glsl` includes.
5. Acceptance: `fix5`-class `#include /program/*` errors go to zero on Complementary/BSL fixtures; messages name pack file.

---

## 4. Phase B — Translation parity (program-together, VulkanMod adaptation)

Goal: one union program model; legacy packs compile to fixtures before touching driver.

1. Keep `ProgramTranslator.translate(units, …)` as the union owner: one shared uniform block + union varyings fixed first, per-stage rewrite after.
2. Binding policy (1.21.1 equivalent of Vitrail emit-none):
   * Keep `bool→int` normalization + `iris_*` attribute handling, extended to union varying set.
   * Compact every descriptor to dense `0..N-1` in deterministic order (injected block first if present, then first-seen vertex→fragment). Already in `VulkanModProgramBridge.buildUnifiedBindingMap`; formalize as shared contract with `ShaderTranslator.ensureDescriptorBindings/remapDescriptorBindings`.
   * Inject vertex input locations from chosen `DefaultVertexFormat` indices + shared varying locations (vertex `out` order, then uncovered fragment `in`). Fail link with reason when uncovered — never build a location-less pipeline.
3. Legacy coverage table (behaviors, not code): strip precision/demote non-const `const`; `texture2D*`→`texture*`; `texture2DLod`→`textureLod`; `shadow2D*` wrap (not rename); `gl_FragData[n]`→indexed outputs; `gl_TextureMatrix`/`ftransform`/`gl_Color`/`gl_Normal` mappings; depth-volume conversion; reversed-Z handling matching VulkanMod depth; mat/struct/array varying split with rank==location discipline; fullscreen/entity/sky vertex roots as needed by Iristransformed output.
4. Keep `SourceProvenance` generation gating so dimension/pack change invalidates cached translations.
5. Tests: golden translation fixtures for terrain/entity/cloud/sky pairs covering every mapping; location-collision regression (two stages with overlapping locations must not collide); binding-density test (`0..N-1` exactly once).
6. Acceptance: legacy errors (`texture2DLod`, `shadow2D`, `gl_FragData`, `gl_TextureMatrix`, `ftransform`) go to zero on fixtures before driver work.

---

## 5. Phase C — Compile path hardening (stack, cache, warmup, stages)

Goal: no OOM, no first-draw shaderc stall, no success-only amnesia, explicit stage policy.

1. `SpirvCompiler`: keep bounded malloc handoff + `MAX_SOURCE_CHARS` + fair semaphore. Convert native `Error` to structured `ShaderCompileResult` + fallback; keep fatal path (record → `VulkairisController.triggerFallback` → rethrow) for truly unrecoverable natives/JVM failures.
2. Cache: extend `ShaderCompileCache` to Vitrail-style module key (source + stage + defines + profile + mod/game/loader/LWJGL versions). Persist SPIR-V words bounded (size/count caps, LRU sweep, opt-out flag). Keep translation cache keyed on expanded text + program + defines. Only `SUCCESS` stored.
3. Warmup: pre-compile active dimension's program set after `IrisBridge.onPipelinePrepared` on worker (`ShaderWarmup`); progress via Compatibility tools; never block world load.
4. Stage policy: keep rejecting tess/geom/compute with explicit reasons until `VulkanPipelineFactory` config + capability check + dispatch exist. Add one stage at a time behind `VulkairisConfig` experimental flags (all default OFF).
5. Tests: oversized-source, concurrent-compile, cache-hit, warmup-before-draw, stage-rejection.
6. Acceptance: no stack OOM on prior failing pack set; second load of same pack is cache-hot.

---

## 6. Phase D — Sodium / VulkanMod terrain door

Goal: make the frozen `SodiumPrograms` path actually draw through VulkanMod.

1. Keep `@Overwrite` signatures frozen in `client/mixin/iris/SodiumProgramsMixin.java`. Add `@Inject`-based behavior around them (in `SodiumTerrainDoorMixin` + `VulkanModProgramBridge`): `compileProgram` HEAD cancellable returning linked pipeline handle when `TerrainPipelineDoor.decideCompile` says pack; render path swapping handed pass for pack descriptor. The swap site runs the descriptor gate (`decideRenderLenient`: compile linkage, then `decideRender`/`checkDescriptorSwap` over the merged pack reflection and the name-correlated linked descriptors); unknown gate inputs keep the compile verdict and record the skip, never silently.
2. Mesh contract: extend Sodium `0.8` vertex layout per `TerrainVertexContract` (Vitrail-analogous extras after 20B base) in `GlShaderMixin` / `IrisRenderSystemMixin` path; `settle()` once at renderer init. Validate against `DefaultVertexFormat.BLOCK` expectations (`isCoveredByBlock` + `describeBlockMapping` evidence: base served by the block fetch, 68B extensions read only when declared, uncovered inputs fail with reason); fail with reason when format uncovered.
3. Targets: pack `colortex` allocation/mip/resize via `rendering/` + `framebuffer/`; shadow map depth + `shadowcolor` with forward-Z clear `1.0`; shadow draw **one frame late by design** (document, do not “fix” into same-frame).
4. Tests: target-format/dimension/binding/lifetime validation (`rendering/FramebufferValidator.java`, `TransformLedger.java`); shadow-cull + fallback-reason unit tests.
5. Acceptance: world renders with pack terrain program bound (not vanilla fallback); per-frame pack pass/target evidence is recorded on `VulkairisController` (`activePass`/`currentTarget`) + `StageDiagnostics` and mirrored into `RenderStateTracker.observePackPass` (diagnostics mirror); fallback records exact reason when engaged. Cutout/shadow door passes are reachable (identity + shadow-bit mapping); the descriptor gate has a runtime caller at the swap site.

---

## 7. Phase E — Uniforms per frame (values, not just layouts)

Goal: snapshot once per frame, write per-pass `std140` from type-from-declaration blocks.

1. Snapshot once per frame (`rendering/state/UniformFrameStore` as `PackValues.advance` equivalent): camera, matrices, time, player (`isEyeInWater`-class), weather, celestial, shadow matrices/geometry, draw/`renderStage`, world/height. Share immutable snapshot across passes.
2. Type-from-declaration: uniform block layout + coercion from GLSL declaration so `bool` vs `int` spellings of the same pack uniform both work; zero-fill + `unanswered()` logging for gaps (e.g. `farPlane`-class).
3. Engine catalog + custom expressions: keep `StandardUniforms` engine table; extend `CustomUniformExpression` with small stareval-equivalent **only** for `uniform./variable.` expressions. No general expression language.
4. Transport: close the `VulkanModProgramBridge` “values not yet transported” gap — write snapshot into VulkanMod manual UBOs per-pass. UBOs declared manual so pipeline creation never requires VulkanMod-owned suppliers.
5. Tests: `std140` packing, type coercion (`bool`/`int`/`float` spellings), snapshot isolation (mid-frame mutation does not leak), custom-expression fixtures, per-pass write determinism.
6. Acceptance: Underwater fog/eye, time-of-day, shadow-matrix-driven shadows change with world state; missing uniforms degrade with logged reason, not silent wrong image.

---

## 8. Phase F — Frame orchestration, shadows, dimensions, fallback honesty

Goal: explicit chain plan replayed every frame, with honest status.

1. `rendering/exec/PackChainPlan`: begins → shadow-capture → prepares → feature redirects → composites/final. Precompute target/sampler/draw-buffer plans once per pack-dimension profile (`CompositeChainPlanner`, `SamplerPlan`, `DrawBuffers`-equivalent); replay without re-solving. Missing-`final` packs use present-substitute writing game main target, labeled as substitute.
2. Shadows: cull plan (swept frustum), depth + `shadowcolor` targets, translucent copy split (`shadowtex0` with vs `shadowtex1` without translucents), HW vs arithmetic road selection, mip chain. One-frame-late documented.
3. Dimensions: per-dimension overlay (`world/nether/end` replacing base where present); `CUSTOM_SKY` stage; fixed-time/fog/`renderStage` branching; End sky/flash + boss-bar no-sky-pass trap handled (no render when sky pass absent).
4. Fallback (`core/VulkairisController`, `diagnostics/*`): every engagement records requested vs effective backend, stage, pack+shader id, sanitized log excerpt. GUI status mirrors verbatim — never imply shaders work because a world opened.
5. Tests: plan replay determinism, substitute-final labeling, fallback-reason propagation to GUI + `/vulkairis status|shader`, shadow-cull fixtures, dimension-overlay fixtures.
6. Acceptance: per-frame pack pass/target evidence is recorded on `VulkairisController` + `StageDiagnostics` and mirrored into `RenderStateTracker`; corrupt pack / empty folder / dimension change all produce labeled fallback, not silent vanilla.

---

## 9. Phase G — Advanced pack features (all default OFF, one at a time)

Goal: close remaining Bliss/BSL/Complementary gaps without destabilizing base. Each sub-phase ships behind its own `VulkairisConfig` experimental flag (default OFF) with capability check + explicit rejection when unavailable.

* G-G1 Compute / custom images / SSBO voxel pipe (caves floodfill analog): dispatch only when pipeline config + capability + per-frame ordering (early, before opaque features) exist. Without it, caves fall back with logged reason.
* G-G2 PBR maps (`MC_MIPMAP_LEVEL` / normal / specular): load + bind only when pack declares + target formats allow; otherwise ignore with diagnostic.
* G-G3 Distant Horizons integration: `DISTANT_HORIZONS`-style define + `dh_terrain/dh_water/dh_shadow` program slots + far-terrain proxy. No hard DH dependency; reflection/proxy only; disabled when DH absent or rendering disabled. Horizon seam documented as pack-side.
* G-G4 Tessellation / geometry stages: one stage at a time, config + capability + dispatch + golden fixtures first.
* Acceptance per sub-phase: one fixture pack proving the feature + one proving graceful OFF fallback. Never hard-code pack names into runtime data.

---

## 10. Phase H — GUI / diagnostics close-out

Already done: ModMenu entry, Compatibility model, browser NPE guard, toggle contract, filesystem-only browser.

Remaining:

1. `ShaderPackService` + `ShaderPacksCategoryModel`: async scan on browser-open + Rescan; empty/scanning/duplicate/corrupt states; stale-on-change retest; never show `run/mods` Fabric mods as packs.
2. `DiagnosticReportService`: sanitize (redact usernames/absolute paths), bound size, Copy Details fallback, text+JSON export to `run/vulkairis-reports`.
3. `AdvancedCategoryModel`: expose only implemented flags (default OFF); safe reset preserving packs/worlds; cache invalidation only after Phase C cache exists.
4. Localization/accessibility: move labels/tooltips/errors to translation keys; keyboard-only + high GUI scale + narrow-window pass.
5. Acceptance: toggle ON/OFF color+label together; two-pack folder shows exactly two cards; empty folder shows no-packs state; rescan reconciles add/remove/rename/replace/duplicate/corrupt.

---

## 11. Build order and test gates

Order: **A (includes) → B (translation goldens) → C (compile/cache/warmup) → D (terrain door) → E (uniform values) → F (chain/shadow/dims/fallback) → G (advanced, one flag at a time) → H (GUI close-out).**

* Do not start D until A+B goldens pass.
* Do not expose new GUI toggles until backend exists with a test.
* Gate every phase with `runTestRunner` additions plus one `runClient` scenario each: (i) boot no-pack, (ii) select pack, (iii) reload pack, (iv) dimension change, (v) corrupt pack, (vi) empty folder.
* Keep `downloadAssets` skipped, `GRADLE_USER_HOME=C:\gradle-ascii` + JDK 21 for all runs.
* Fixture order: Complementary / BSL / Bliss first; niche variants only after include+legacy tables prove out.

---

## 12. Risks

* Iris `1.8.14` internals drift: every Iris bump re-runs signature checks on `SodiumPrograms` + transformer classes and the translation goldens; prefer injection points over overwrites.
* VulkanMod owns submission: never present swapchain, never assume GL state; all pack work via `VulkanModProgramBridge` / `VulkanPipelineFactory` contracts.
* Pack diversity: fixture-first, never pack-name branching in runtime.
* Privacy/perf: reports redacted + bounded; scan/compile/report off render thread with immutable handoff.
* Scope creep into second loader / native backend: explicitly out of scope. Vitrail behaviors are re-expressed through Iris+VulkanMod, not re-owned.

---

## 13. Review checklist (for approver)

* [ ] Phase order accepted (A→H, G staged per-flag)?
* [ ] Dense-binding + location injection accepted as the 1.21.1 equivalent of Vitrail emit-none?
* [ ] Shadow one-frame-late + substitute-`final` labeling accepted?
* [ ] Compute/DH/PBR all-default-OFF staging accepted?
* [ ] `runClient` scenario matrix + `runTestRunner` gating sufficient?
* [ ] Anything in §1 baseline misclassified (implemented vs stub)?

On approval, implement one phase at a time with tests + `runClient` evidence before opening the next.
