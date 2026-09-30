package com.vulkairis.iris;

import com.vulkairis.compatibility.AnalysisResult;
import com.vulkairis.compatibility.ShaderAnalyzer;
import com.vulkairis.core.BackendState;
import com.vulkairis.core.VulkairisController;
import com.vulkairis.diagnostics.DiagnosticManager;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Isolated bridge between Iris shader pack system and Vulkairis.
 */
public class IrisBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vulkairis/IrisBridge");

    private static String currentPackName = "None";

    public static void onShaderPackChanged() {
        try {
            Optional<ShaderPack> packOpt = Iris.getCurrentPack();
            if (packOpt.isPresent()) {
                ShaderPack pack = packOpt.get();
                currentPackName = pack.getProfileInfo() != null ? pack.getProfileInfo() : "Custom Shaderpack";
                VulkairisController.getInstance().setActiveShaderPackName(currentPackName);
                LOGGER.info("[Vulkairis/IrisBridge] Detected active shader pack: {}", currentPackName);

                // Run compatibility dry run on pack programs
                evaluatePack(pack);
            } else {
                currentPackName = "None (Internal)";
                VulkairisController.getInstance().setActiveShaderPackName(currentPackName);
                LOGGER.info("[Vulkairis/IrisBridge] No shader pack active (using internal/vanilla shaders)");
            }
        } catch (Throwable t) {
            LOGGER.error("[Vulkairis/IrisBridge] Error inspecting Iris shader pack: {}", t.getMessage());
            VulkairisController.getInstance().triggerFallback("Iris shader pack inspection error", t.getMessage());
        }
    }

    private static void evaluatePack(ShaderPack pack) {
        try {
            ProgramSet programSet = pack.getProgramSet(Iris.getCurrentDimension());
            if (programSet == null) return;

            // Analyze composite programs if present
            ProgramSource[] composites = programSet.getComposite(net.irisshaders.iris.shaderpack.loading.ProgramArrayId.Composite);
            if (composites != null) {
                for (ProgramSource prog : composites) {
                    if (prog != null && prog.isValid()) {
                        validateProgramSource(prog);
                    }
                }
            }

            VulkairisController.getInstance().setState(BackendState.READY);
            DiagnosticManager.log(DiagnosticManager.Category.COMPATIBILITY, "Iris shader pack evaluation passed", currentPackName);
        } catch (Throwable t) {
            LOGGER.warn("[Vulkairis/IrisBridge] Pack validation notice: {}", t.getMessage());
        }
    }

    private static void validateProgramSource(ProgramSource source) {
        String name = source.getName();
        source.getVertexSource().ifPresent(vSrc -> {
            AnalysisResult res = ShaderAnalyzer.analyzeAndTest(name + "_vert", vSrc, 35633);
            if (!res.isSuccess()) {
                DiagnosticManager.log(DiagnosticManager.Category.SHADER, "Vertex shader dry-run result: " + res.reason(), res.details());
            }
        });

        source.getFragmentSource().ifPresent(fSrc -> {
            AnalysisResult res = ShaderAnalyzer.analyzeAndTest(name + "_frag", fSrc, 35632);
            if (!res.isSuccess()) {
                DiagnosticManager.log(DiagnosticManager.Category.SHADER, "Fragment shader dry-run result: " + res.reason(), res.details());
            }
        });
    }

    public static String getCurrentPackName() {
        return currentPackName;
    }
}
