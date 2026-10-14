package com.vulkairis.iris;

import com.vulkairis.compatibility.AnalysisResult;
import com.vulkairis.compatibility.ShaderAnalyzer;
import com.vulkairis.core.BackendState;
import com.vulkairis.core.PackReadinessState;
import com.vulkairis.core.VulkairisController;
import com.vulkairis.diagnostics.DiagnosticManager;
import com.vulkairis.translation.IncludeResolver;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Isolated bridge between Iris shader pack system and Vulkairis.
 */
public class IrisBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vulkairis/IrisBridge");

    private static String currentPackName = "None";
    private static volatile ShaderPackProgramCatalog programCatalog = null;
    /** Last dimension seen by the pipeline hooks; a change starts a new pipeline generation. */
    private static String lastDimension = null;

    /**
     * Phase 2: pack-change / pipeline-destroy / manual-reload event. Starts a new pipeline
     * generation so cached translations and pipelines from the previous input are stale, then
     * rediscovers the pack through Iris's own API (no second pack loader).
     */
    public static void onShaderPackChanged() {
        SourceProvenance.nextGeneration("shader-pack change / pipeline destroy / manual reload");
        PbrTextureState.invalidate("shader-pack change");
        VulkairisController.getInstance().setPendingReload(true);
        refresh();
    }

    /**
     * Phase 2: Iris finished (re)building its pipeline. A dimension change without a preceding
     * destroy still invalidates cached output; otherwise this only refreshes discovery state.
     */
    public static void onPipelinePrepared() {
        try {
            String dimension = String.valueOf(Iris.getCurrentDimension());
            if (lastDimension != null && !lastDimension.equals(dimension)) {
                SourceProvenance.nextGeneration(
                        "dimension change: " + lastDimension + " -> " + dimension);
                PbrTextureState.invalidate("dimension change: " + lastDimension + " -> " + dimension);
                VulkairisController.getInstance().setPendingReload(true);
            }
            lastDimension = dimension;
        } catch (Throwable ignored) {
        }
        refresh();
    }

    private static void refresh() {
        try {
            Optional<ShaderPack> packOpt = Iris.getCurrentPack();
            if (packOpt.isPresent()) {
                ShaderPack pack = packOpt.get();
                currentPackName = pack.getProfileInfo() != null ? pack.getProfileInfo() : "Custom Shaderpack";
                VulkairisController.getInstance().setActiveShaderPackName(currentPackName);
                LOGGER.info("[Vulkairis/IrisBridge] Detected active shader pack: {}", currentPackName);

                // Discover the complete program set before any translation/execution work.
                evaluatePack(pack);
            } else {
                currentPackName = "None (Internal)";
                programCatalog = null;
                com.vulkairis.rendering.exec.VulkairisFrameCoordinator.setActivePipeline(null);
                VulkairisController.getInstance().setActiveShaderPackName(currentPackName);
                VulkairisController.getInstance().getReadinessStateMachine().reset(
                        SourceProvenance.currentGeneration(), "No active shader pack");
                LOGGER.info("[Vulkairis/IrisBridge] No shader pack active (using internal/vanilla shaders)");
            }
        } catch (Throwable t) {
            LOGGER.error("[Vulkairis/IrisBridge] Error inspecting Iris shader pack: {}", t.getMessage());
            VulkairisController.getInstance().triggerFallback("Iris shader pack inspection error", t.getMessage());
        }
    }

    private static void evaluatePack(ShaderPack pack) {
        long generation = SourceProvenance.currentGeneration();
        String dimName = "default";
        try {
            dimName = Iris.getCurrentDimension() != null ? Iris.getCurrentDimension().toString() : "overworld";
        } catch (Throwable ignored) {
        }

        try {
            VulkairisController.getInstance().setState(BackendState.INITIALIZING);
            ProgramSet programSet = pack.getProgramSet(Iris.getCurrentDimension());
            if (programSet == null) {
                LOGGER.warn("[Vulkairis/IrisBridge] Shader pack '{}' exposed no program set for dimension {}",
                        currentPackName, Iris.getCurrentDimension());
                VulkairisController.getInstance().getReadinessStateMachine().transitionTo(
                        PackReadinessState.REJECTED, null, generation, currentPackName, dimName,
                        "all", false, "Exposed no program set for dimension");
                return;
            }

            // Phase 1: discover every program group (shadow, gbuffers, deferred, composite, final,
            // compute, ...), not just composites.
            ShaderPackProgramCatalog discovered = ShaderPackProgramCatalog.discover(programSet);
            programCatalog = discovered;

            LOGGER.info("[Vulkairis/IrisBridge] {}", discovered.describe().replace("\n", "\n[Vulkairis/IrisBridge]"));
            DiagnosticManager.log(DiagnosticManager.Category.COMPATIBILITY,
                    "Discovered shader-pack programs",
                    discovered.size() + " program(s) across groups " + discovered.getPresentGroups());

            VulkairisController.getInstance().getReadinessStateMachine().transitionTo(
                    PackReadinessState.DISCOVERED, discovered, generation, currentPackName, dimName,
                    "all", false, null);

            // Phase 12: capability gate before any partial rendering begins. Rejected packs
            // fall back with one actionable report; partial packs are noted and continue.
            com.vulkairis.compatibility.CapabilityGate.GateVerdict gate =
                    com.vulkairis.compatibility.CapabilityGate.evaluatePack(discovered);
            for (String note : gate.notes()) {
                DiagnosticManager.log(DiagnosticManager.Category.COMPATIBILITY,
                        "Pack capability note", note);
            }
            if (gate.verdict() == com.vulkairis.compatibility.CapabilityGate.PackVerdict.REJECT) {
                String reason = String.join("; ", gate.notes());
                VulkairisController.getInstance().getReadinessStateMachine().transitionTo(
                        PackReadinessState.REJECTED, discovered, generation, currentPackName, dimName,
                        "all", false, reason);
                VulkairisController.getInstance().triggerFallback("Shader pack rejected", reason);
                com.vulkairis.rendering.exec.VulkairisFrameCoordinator.setActivePipeline(null);
                LOGGER.warn("[Vulkairis/IrisBridge] Pack '{}' rejected: {}", currentPackName, reason);
                return;
            }

            VulkairisController.getInstance().getReadinessStateMachine().transitionTo(
                    PackReadinessState.TRANSLATING, discovered, generation, currentPackName, dimName,
                    "all", false, null);

            prewarmTranslations(discovered);

            for (ShaderPackProgram program : discovered.getGraphicsPrograms()) {
                validateGraphicsProgram((ShaderPackProgram.Graphics) program);
            }
            for (ShaderPackProgram program : discovered.getComputePrograms()) {
                DiagnosticManager.log(DiagnosticManager.Category.SHADER,
                        "Compute program discovered (execution path pending)", program.name());
            }

            if (!discovered.hasGraphicsExecutionPath()) {
                LOGGER.warn("[Vulkairis/IrisBridge] Pack '{}' declares no gbuffers/deferred/composite/final "
                        + "execution path; it cannot produce a rendered image on its own.", currentPackName);
            }

            VulkairisController.getInstance().getReadinessStateMachine().transitionTo(
                    PackReadinessState.INTERFACE_VALIDATED, discovered, generation, currentPackName, dimName,
                    "all", false, null);

            // Rebuild pack pipeline in frame coordinator
            try {
                com.vulkairis.rendering.exec.VulkairisFrameCoordinator.rebuildPipeline(1920, 1080, 1024, null);
            } catch (Throwable ignored) {
            }

            // Phase 1 Truthful Gate Check: attempting promotion to EXECUTION_READY enforces PassGates
            // and GPU resource backing. If mandatory families are CLOSED/PARTIAL, state machine
            // records DEGRADED and refuses promotion.
            VulkairisController.getInstance().getReadinessStateMachine().transitionTo(
                    PackReadinessState.EXECUTION_READY, discovered, generation, currentPackName, dimName,
                    "all", false, null);

            // Discovery is not readiness. Pipelines, resources and pass contracts are required
            // before READY; the bridge is active but the shader pipeline is not proven yet.
            VulkairisController.getInstance().setState(BackendState.BRIDGE_ACTIVE);
            LOGGER.info("[Vulkairis/IrisBridge] Program discovery complete for '{}'; pipeline/resource "
                    + "contracts are still required before READY (ReadinessState: {}).",
                    currentPackName, VulkairisController.getInstance().getReadinessState());
        } catch (Throwable t) {
            LOGGER.warn("[Vulkairis/IrisBridge] Pack validation notice: {}", t.getMessage());
            VulkairisController.getInstance().getReadinessStateMachine().transitionTo(
                    PackReadinessState.REJECTED, programCatalog, generation, currentPackName, dimName,
                    "all", false, t.getMessage());
            VulkairisController.getInstance().triggerFallback("Shader pack discovery failed", t.getMessage());
        }
    }

    private static void validateGraphicsProgram(ShaderPackProgram.Graphics program) {
        if (program.source() == null) {
            return;
        }
        String name = program.name();
        program.source().getVertexSource().ifPresent(src -> analyze(name + "_vert", src, 35633));
        program.source().getFragmentSource().ifPresent(src -> analyze(name + "_frag", src, 35632));
    }

    private static void analyze(String name, String source, int stage) {
        AnalysisResult result = ShaderAnalyzer.analyzeAndTest(name, source, stage);
        if (!result.isSuccess()) {
            DiagnosticManager.log(DiagnosticManager.Category.SHADER,
                    "Shader dry-run result: " + result.reason(), result.details());
        }
    }

    /**
     * Phase 12: background translation prewarm. Pure string translation (no GL/Vulkan objects)
     * runs off-thread against the current pipeline generation so the first real compile finds
     * warm translation caches. Stale tasks are purged; results are intentionally unconsumed —
     * installation still happens synchronously at interception, which Iris requires.
     */
    private static void prewarmTranslations(ShaderPackProgramCatalog catalog) {
        try {
            long generation = SourceProvenance.currentGeneration();
            com.vulkairis.translation.TranslationWorker.purgeStale(generation);
            for (ShaderPackProgram program : catalog.getGraphicsPrograms()) {
                if (!(program instanceof ShaderPackProgram.Graphics graphics)
                        || graphics.source() == null) {
                    continue;
                }
                graphics.source().getVertexSource().ifPresent(src -> prewarmOne(
                        graphics.name() + "_vert", src, true, generation));
                graphics.source().getFragmentSource().ifPresent(src -> prewarmOne(
                        graphics.name() + "_frag", src, false, generation));
            }
        } catch (Throwable t) {
            LOGGER.debug("[Vulkairis/IrisBridge] Translation prewarm notice: {}", t.getMessage());
        }
    }

    private static void prewarmOne(String name, String source, boolean isVertex, long generation) {
        try {
            com.vulkairis.translation.TranslationWorker.submit(name, generation,
                    () -> com.vulkairis.translation.ShaderTranslator.preprocessGlsl(source, isVertex));
        } catch (Throwable t) {
            LOGGER.debug("[Vulkairis/IrisBridge] Prewarm submit notice for '{}': {}",
                    name, t.getMessage());
        }
    }

    public static String getCurrentPackName() {
        return currentPackName;
    }

    private record PackFilesSnapshot(ShaderPack pack, long generation, IncludeResolver.PackFiles files) {
    }

    private static volatile PackFilesSnapshot packFilesSnapshot = null;

    /**
     * Phase A: Iris-owned pack file access for include expansion.
     *
     * <p>Reads through the {@code ShaderPack} Iris already loaded (its include graph plus its
     * source-provider fallback) and never mounts a second copy of the archive. The snapshot is
     * cached per pack instance and pipeline generation, so repeated compilations in one frame do
     * not re-introspect. When no pack is active or introspection fails, the result is an
     * unavailable-files miss — the resolver turns those into named {@code #error} lines and
     * never throws into the render path.</p>
     *
     * @return pack file access, possibly {@linkplain IncludeResolver.PackFiles#available()
     *         unavailable}
     */
    public static IncludeResolver.PackFiles packFiles() {
        Optional<ShaderPack> packOpt;
        try {
            packOpt = Iris.getCurrentPack();
        } catch (Throwable t) {
            return IncludeResolver.unavailable("Iris pack query failed: " + t);
        }
        if (packOpt.isEmpty()) {
            return IncludeResolver.unavailable("no Iris shader pack is active");
        }
        ShaderPack pack = packOpt.get();
        long generation = SourceProvenance.currentGeneration();
        PackFilesSnapshot cached = packFilesSnapshot;
        if (cached != null && cached.pack() == pack && cached.generation() == generation) {
            return cached.files();
        }
        IncludeResolver.PackFiles files;
        try {
            files = IrisPackFiles.snapshot(pack);
        } catch (Throwable t) {
            LOGGER.debug("[Vulkairis/IrisBridge] Pack file introspection unavailable: {}",
                    String.valueOf(t));
            return IncludeResolver.unavailable("pack introspection failed: " + t);
        }
        packFilesSnapshot = new PackFilesSnapshot(pack, generation, files);
        return files;
    }

    /**
     * @return the most recently discovered program catalog, or {@code null} when no shader pack is
     * active or discovery has not run yet
     */
    public static ShaderPackProgramCatalog getProgramCatalog() {
        return programCatalog;
    }

    /**
     * Phase E: the pack identity used as the chain-plan profile key. Never null.
     */
    public static String currentPackId() {
        return currentPackName != null ? currentPackName : "unknown-pack";
    }

    /**
     * Phase E: the dimension identity used as the chain-plan profile key. Headless-safe:
     * {@code "unknown-dimension"} when Iris cannot answer (tests, menus, early startup).
     */
    public static String currentDimensionId() {
        try {
            Object dimension = Iris.getCurrentDimension();
            return dimension != null ? String.valueOf(dimension) : "unknown-dimension";
        } catch (Throwable t) {
            return "unknown-dimension";
        }
    }

    /**
     * Phase E: orchestration hook the frame coordinator calls after it (re)solves or replays the
     * chain plan. Publishes the plan outcome (profile, digest, substitute labeling) to
     * diagnostics without touching discovery state. Append-only: discovery logic above is
     * unchanged.
     */
    public static void noteChainPlan(com.vulkairis.rendering.exec.PackChainPlan plan) {
        if (plan == null) {
            return;
        }
        try {
            DiagnosticManager.log(DiagnosticManager.Category.COMPATIBILITY,
                    "Chain plan " + plan.profileKey() + " digest=" + plan.digest(),
                    plan.finalLabel());
        } catch (Throwable ignored) {
        }
    }

    /**
     * Phase E: orchestration hook for engaging fallback with an explicit stage and shader
     * identity. Routes through the controller's detailed path so the engagement carries
     * requested-vs-effective backends, stage, pack+shader id and a sanitized excerpt.
     * Append-only: discovery logic above is unchanged.
     */
    public static void engageFallback(String stage, String shaderId,
                                      String reason, String logExcerpt) {
        try {
            VulkairisController.getInstance().triggerFallbackDetailed(
                    VulkairisController.requestedBackendName(),
                    VulkairisController.EFFECTIVE_FALLBACK_BACKEND,
                    stage, currentPackId(), shaderId, reason, logExcerpt);
        } catch (Throwable t) {
            LOGGER.debug("[Vulkairis/IrisBridge] Fallback engagement notice: {}",
                    String.valueOf(t));
        }
    }
}
