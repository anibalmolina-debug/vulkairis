# Porting Vitrail's Vulkan-shader approach to 1.21.1 (VulkanMod + Iris)

Source analysis: `VITRAIL_VULKAN_REPORT.md` (Vitrail `0.12.0-beta+mc26.3`, native Vulkan game) vs this repo (MC 1.21.1, OpenGL game + Iris `1.8.14-beta.1` + VulkanMod `0.6.7` + `provides sodium`). Planning only — no behavior changes in this file.

## 0. Constraints (non-negotiable)

1. Stay on 1.21.1 + Fabric Loader `0.19.5` + Loom. No native `VulkanBackend`; VulkanMod remains the Vulkan owner and Sodium-0.8-era Iris remains the pack owner.
2. Iris keeps pack loading, settings, and pipeline orchestration. Vulkairis keeps intercepting **Iris-transformed** GLSL (see `iris/SourceProvenance.java`) and routing it to VulkanMod pipelines via `bridge/VulkanModProgramBridge.java`. Do not build a second pack loader.
3. Narrow wraps over `@Overwrite`. The fixed `SodiumProgramsMixin` signatures are correct but `@Overwrite` is version-fragile; all new interception uses `@Inject(cancellable)` / `@ModifyVariable` / `@WrapOperation` on stable call sites.
4. Every GUI control keeps plan contract (`Vulkairis_GUI_Implementation_Plan_Revision_2.docx`, `ToggleButton.java`, `ShaderPackService.java`): real effect + persisted value + default + validation + tooltip. No decorative controls.
5. No render-thread blocking for scan/compile/report/IO; publish immutable results to the client thread (`TranslationWorker`).

## 1. Gap map (Vitrail capability → Vulkairis status)

| # | Vitrail capability | Vulkairis status | Evidence |
|---|---|---|---|
| G1 | Absolute-`/` include resolver (`/program/x.glsl` → `shaders/` root; relative otherwise; confine; bounds; memo) | Missing — direct pack reads fail | `runclient-fix5.log`: `#error unexpected include directive for header name: /program/*.glsl`; `translation/ShaderTranslator.java` has no include handling |
| G2 | Program-together translation, union `OfGlobals`/varyings, emit-no-bindings (reflection assigns) | Partial — per-stage regex preprocess, per-file bindings, `iris_*` strip + block at binding 100 | `ShaderTranslator.java:17-60`, `ensureDescriptorBindings/remapDescriptorBindings` |
| G3 | Legacy-builtin coverage (`texture2DLod`→`textureLod`, `shadow2D*` wrap, `gl_FragData`/`gl_TextureMatrix`/`ftransform`, depth-volume conversion, varying-split) | Partial — vertex/fragment only, tess/geom/compute rejected; fix5 still fails on `texture2DLod`/`shadow2D`/`gl_FragData`/`gl_TextureMatrix` | `SpirvCompiler.java:20-22`, fix5 dry-run errors |
| G4 | In-process shaderc + translation/SPIR-V caches + warmup; bounded `MemoryStack` use | Partial — `SpirvCompiler` + `ShaderCompileCache` (success-only) exist; `OutOfMemoryError: Out of stack space` at `shaderc_compile_into_spv` (`SpirvCompiler.java:121` path) under large packs | fix3/fix5 logs |
| G5 | Sodium terrain door (compileProgram→pack pipeline; render→pack targets; mesh extend; shadow second walk, one frame late) | Partial — `SodiumProgramsMixin` now matches Iris signatures and compiles; runtime behavior (targets/descriptors/shadow) unverified in-game | `mixin/iris/SodiumProgramsMixin.java:23-48` |
| G6 | Per-frame uniform snapshot (`PackValues.advance` → `WorldState` → per-pass `write`) type-from-declaration | Partial — `bridge/UniformBridge.java`, `bridge/StandardUniforms.java`, `rendering/state/*` exist; custom-uniform expression graph missing | plan §5 services exist, expression eval absent |
| G7 | Frame orchestration (begins/prepares → feature redirects → composites/final or present-substitute) | Partial — `IrisBridge.onPipelinePrepared/refresh`, `PipelineManagerMixin`, `RenderBuffersMixin` exist; no explicit chain plan/sampler plan replay | `iris/IrisBridge.java`, `mixin/iris/PipelineManagerMixin.java` |
| G8 | GUI: ModMenu entry (done), Compatibility model (done), browser NPE (done); remaining plan phases (localization, export limits, advanced-only-real-flags) | Mostly done — verify in-game | `gui/VulkairisModMenu.java`, `gui/CompatibilityCategoryModel.java`, `gui/ShaderPackBrowserScreen.java` |

## 2. Phase A — Include resolver (unblocks all pack compiles)

Goal: `/program/x.glsl` and relative includes resolve exactly like Vitrail, before any translation runs.

1. Add `translation/IncludeResolver.java` (new): `follow(from, spec)` — spec starting with `/` → resolve under Iris pack `shaders/` root, else relative to includer; strip leading `/+`, normalize, confine to root, exact-hit then case-insensitive fallback; `MAX_DEPTH=32`, `MAX_FILES=20000`, per-file byte/line caps; memo per `(generation, entry)`; `#error` on missing/cyclic with filename + reason.
2. Pack-root access stays Iris-owned: resolve root via `Iris.getCurrentPack()` file location (`IrisBridge`), never a second zip mount. If root is unavailable, return a structured miss (do not throw into render).
3. Wire into the capture path: expand includes on the **pre-translation** text in `translation/ShaderTranslator.java` (and any direct-pack read used by `compatibility/ShaderAnalyzer.java` / `iris/IrisBridge.validateGraphicsProgram`), preserving `#line` markers so compiler errors map to pack files.
4. Tests: unit tests for absolute/relative/case-insensitive/missing/cyclic/escape-attempt; integration test on a fixture zip containing `/program/*.glsl` includes.
5. Acceptance: fix5-style `#include /program/*` errors go to zero on Complementary/BSL fixtures; error messages name the pack file.

## 3. Phase B — Translation parity (program-together, no per-file bindings)

Goal: stop emitting bindings the driver must fight over; translate vertex+fragment (+tess/geom later) as one program.

1. Refactor `ShaderTranslator` toward `ProgramTranslator.translate(units, ...)`: union pass over all stages to build one `VulkairisUniformBlock` + union varyings; per-stage rewrite after the union is fixed.
2. Binding policy: emit **no `layout(binding=)` and no `layout(location=)` except fragment outputs**; let `SpirvReflector` + `DescriptorSetLayoutBuilder` assign from reflection (Vitrail rule). Remove per-file numbering in `ensureDescriptorBindings/remapDescriptorBindings`; keep the `bool→int` normalization and `iris_*` attribute handling, extended to the union varying set.
3. Legacy coverage table (port `LegacyGlsl` semantics, not code): `texture2D*`/`texture2DLod`→`texture*`/`textureLod`, `shadow2D*` wrap, `gl_FragData[n]`→indexed outputs, `gl_TextureMatrix`/`ftransform`/`gl_Color`/`gl_Normal` mappings, depth-volume conversion, mat/struct/array varying split with rank==location workaround.
4. Keep `SourceProvenance` generation gating so a dimension/pack change invalidates cached translations.
5. Tests: golden translation fixtures for terrain/entity/cloud/sky pairs covering every mapping above; location-collision regression test (two stages declaring overlapping locations must not collide after translation).
6. Acceptance: fix5 legacy errors (`texture2DLod`, `shadow2D`, `gl_FragData`, `gl_TextureMatrix`, `ftransform`) go to zero on fixtures before touching the driver.

## 4. Phase C — Compile path hardening (stack, cache, stages)

Goal: no `OutOfMemoryError`, no first-draw shaderc, no success-only amnesia.

1. `SpirvCompiler`: wrap `shaderc_compile_into_spv` in an explicit bounded stack frame (push/pop per compile, chunk large sources, cap concurrent `vulkairis-compile-*` workers); convert native `Error` into structured `ShaderCompileResult` + fallback (existing discipline in `SpirvCompiler.java:28-29`).
2. Cache: extend `ShaderCompileCache` with a Vitrail-style `ModuleCache` key (source + stage + defines + profile + mod/game/loader/LWJGL versions), persist SPIR-V words (bounded size/count, opt-out flag), keep translation cache keyed on expanded text + program + defines.
3. Warmup: pre-compile the active dimension's program set after `IrisBridge.onPipelinePrepared` on a worker (mirror `AheadCompiler`/`FamilyWarmup`); surface progress in Compatibility tools, never block world load.
4. Stage policy: keep rejecting tess/geom/compute with explicit reasons until their `VulkanPipelineFactory` configuration + capability check + dispatch exist; add one stage at a time behind `VulkairisConfig` experimental flags (all default OFF).
5. Tests: oversized-source compile, concurrent-compile, cache-hit, warmup-before-draw, stage-rejection tests.
6. Acceptance: no stack OOM on the fix5 pack set; second load of the same pack is cache-hot.

## 5. Phase D — Sodium/VulkanMod door (targets, descriptors, shadow)

Goal: make the fixed `SodiumProgramsMixin` actually draw through VulkanMod.

1. Keep `@Overwrite` signatures frozen; add `@Inject`-based behavior around them: `compileProgram` HEAD cancellable returning `TerrainDraw`-equivalent VulkanMod pipeline handle; `render` path swapping the handed pass for the pack descriptor (Vitrail `MixinDefaultChunkRenderer` semantics adapted to `VulkanModProgramBridge` + `DescriptorSetLayoutBuilder` + `VulkanPipelineFactory`).
2. Mesh contract: extend Sodium 0.8 vertex layout exactly like `TerrainMesh` (append `BLOCK_ID/MID_TEX_COORD/MID_BLOCK/TANGENT_FRAME/TINT_AND_AO` after the 20 B base) in `GlShaderMixin`/`IrisRenderSystemMixin` path; `settle()` once at renderer init.
3. Targets: pack `colortex` allocation/mip/resize via `rendering/` + `framebuffer/` (mirror `ColorTargets/TargetSurface`); shadow map depth + `shadowcolor` via `ShadowTargets` semantics with forward-Z clear 1.0; shadow draw one frame late by design (document, do not "fix" into same-frame).
4. Uniforms per frame: snapshot once (`PackValues.advance` equivalent in `rendering/state/UniformFrameStore`), per-pass `write` from type-from-declaration blocks + engine catalog + custom-uniform expressions (add a small stareval-equivalent only for `uniform./variable.` expressions; no general expression language).
5. Tests: target-format/dimension/binding/lifetime validation in debug modes (`rendering/FramebufferValidator.java`, `TransformLedger.java`); shadow-cull and fallback-reason unit tests.
6. Acceptance: world renders with pack terrain program bound (not vanilla fallback), `RenderStateTracker` shows pack pass/target, fallback records exact reason when it engages.

## 6. Phase E — Frame orchestration and fallback honesty

Goal: an explicit chain plan replayed every frame, with honest status.

1. Introduce `rendering/exec/PackChainPlan` (already partially present as `VulkairisFrameCoordinator`/`PackRenderPipeline`): begins → shadow-capture → prepares → feature redirects → composites/final; precompute target/sampler/draw-buffer plans once per pack-dimension profile, replay without re-solving.
2. Missing-`final` packs: substitute a present-equivalent that writes the game main target (Vitrail `ChainPresent`), labeled as substitute in diagnostics.
3. Fallback (`core/VulkairisController`, `diagnostics/*`): every engagement records requested vs effective backend, stage, pack+shader id, sanitized log excerpt; GUI status text mirrors it verbatim — never imply shaders work because a world opened.
4. Tests: plan replay determinism, substitute-final labeling, fallback-reason propagation to GUI + `/vulkairis status|shader`.

## 7. Phase F — GUI close-out (plan Rev 2 remainder)

Already done: ModMenu entry, Compatibility model, browser NPE guard, toggle contract, filesystem-only browser. Remaining:

1. `ShaderPackService` + `ShaderPacksCategoryModel`: keep async scan on browser-open + Rescan, empty/scanning/duplicate/corrupt states, stale-on-change retest; never show `run/mods` Fabric mods as packs.
2. `DiagnosticReportService`: sanitize (redact usernames/absolute paths), bound size, Copy Details fallback, text+JSON export to `run/vulkairis-reports`.
3. `AdvancedCategoryModel`: expose only implemented flags (default OFF), safe reset preserving packs/worlds, cache invalidation only after the Phase C cache exists.
4. Localization/accessibility: move labels/tooltips/errors to translation keys; keyboard-only + high GUI scale + narrow-window pass.
5. Acceptance: Rev 2 §12.3–12.4 matrix (toggle ON/OFF color+label together; two-pack folder shows exactly two cards; empty folder shows no-packs state; rescan reconciles add/remove/rename/replace/duplicate/corrupt).

## 8. Build order and test gates

1. A (includes) → B (translation goldens) → C (compile/cache/warmup) → D (terrain door) → E (chain/fallback) → F (GUI close-out). Do not start D until A+B goldens pass; do not expose new GUI toggles until their backend exists with a test.
2. Gate every phase with `runTestRunner` additions plus one `runClient` scenario: (i) boot no-pack, (ii) select pack, (iii) reload pack, (iv) dimension change, (v) corrupt pack, (vi) empty folder.
3. Keep `downloadAssets` skipped, `GRADLE_USER_HOME=C:\gradle-ascii` + JDK 21 for all runs (non-ASCII profile breaks the dev launcher classpaths).

## 9. Risks

- Iris `1.8.14` internals drift: every Iris bump re-runs `javap` on `SodiumPrograms` + transformer classes and the translation goldens; prefer injection points over overwrites.
- VulkanMod owns submission: never present the swapchain, never assume GL state; all pack work goes through `VulkanModProgramBridge`/`VulkanPipelineFactory` contracts.
- Pack diversity: Complementary/BSL/Bliss fixtures first; Sildur's-style variants only after the include+legacy tables prove out — never hard-code pack names into runtime data.
- Privacy/perf: reports redacted + bounded; scan/compile/report off the render thread with immutable handoff.
