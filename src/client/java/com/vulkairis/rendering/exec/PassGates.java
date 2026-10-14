package com.vulkairis.rendering.exec;

import com.vulkairis.iris.ShaderPackProgram;
import com.vulkairis.iris.ShaderPackProgramCatalog;
import net.irisshaders.iris.shaderpack.loading.ProgramGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The incremental pass gates (Phase 11): which render-stage families exist, which the pack
 * needs, and which are actually implemented.
 *
 * <p>Order follows the plan's small gates: a known fullscreen composition first, then terrain,
 * opaque entities, shadows, translucency, deferred/composite work, and finally the final pass
 * with its HUD boundary. A gate that is required but not implemented is reported, never
 * silently skipped — a pack is not "supported" when its expected families never ran.</p>
 */
public final class PassGates {

    /** Render-stage families in plan gate order. */
    public enum Gate {
        FULLSCREEN_COMPOSITION,
        TERRAIN,
        OPAQUE_ENTITIES,
        SHADOW,
        TRANSLUCENT,
        DEFERRED_COMPOSITE,
        FINAL_HUD
    }

    /** Implementation state of one gate. */
    public enum Status {
        OPEN,
        PARTIAL,
        CLOSED
    }

    /** One gate with its implementation state and the reason. */
    public record GateStatus(Gate gate, Status status, String note) {
    }

    private PassGates() {
    }

    /**
     * @return the honest implementation state of every gate. The controlled composition quad
     * exists behind an explicit toggle; the composite/final post chain executes when bound;
     * world-stage families have no execution path yet; the HUD boundary is enforced by
     * never intercepting GUI rendering.
     */
    public static Map<Gate, GateStatus> implemented() {
        Map<Gate, GateStatus> table = new EnumMap<>(Gate.class);
        table.put(Gate.FULLSCREEN_COMPOSITION, new GateStatus(Gate.FULLSCREEN_COMPOSITION,
                Status.OPEN, "controlled quad behind an explicit debug toggle"));
        table.put(Gate.TERRAIN, new GateStatus(Gate.TERRAIN,
                Status.CLOSED, "no terrain execution path yet"));
        table.put(Gate.OPAQUE_ENTITIES, new GateStatus(Gate.OPAQUE_ENTITIES,
                Status.CLOSED, "no entity execution path yet"));
        table.put(Gate.SHADOW, new GateStatus(Gate.SHADOW,
                Status.CLOSED, "shadow config exists but no shadow execution path yet"));
        table.put(Gate.TRANSLUCENT, new GateStatus(Gate.TRANSLUCENT,
                Status.CLOSED, "no translucent execution path yet"));
        table.put(Gate.DEFERRED_COMPOSITE, new GateStatus(Gate.DEFERRED_COMPOSITE,
                Status.PARTIAL, "composite/final chain executes when bound; deferred not executed"));
        table.put(Gate.FINAL_HUD, new GateStatus(Gate.FINAL_HUD,
                Status.PARTIAL, "final programs run inside the post chain; GUI stays uninterrupted"));
        return Collections.unmodifiableMap(table);
    }

    /**
     * Maps a discovered catalog to the gates it needs, from Iris's own program groups and
     * program names (never Vitrail seam names).
     */
    public static Set<Gate> requiredFor(ShaderPackProgramCatalog catalog) {
        if (catalog == null || catalog.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Gate> required = EnumSet.noneOf(Gate.class);
        for (ShaderPackProgram program : catalog.getPrograms()) {
            if (program.isCompute()) {
                continue;
            }
            ProgramGroup group = program.group();
            switch (group) {
                case Shadow, ShadowComposite -> required.add(Gate.SHADOW);
                case Gbuffers -> {
                    required.add(Gate.TERRAIN);
                    required.add(Gate.OPAQUE_ENTITIES);
                    String name = program.name().toLowerCase();
                    if (name.contains("water") || name.contains("translucent")) {
                        required.add(Gate.TRANSLUCENT);
                    }
                    if (name.contains("hand")) {
                        required.add(Gate.FINAL_HUD);
                    }
                }
                case Deferred, Composite -> required.add(Gate.DEFERRED_COMPOSITE);
                case Final -> {
                    required.add(Gate.DEFERRED_COMPOSITE);
                    required.add(Gate.FINAL_HUD);
                }
                case Setup, Begin, Prepare, Dh -> required.add(Gate.TERRAIN);
            }
        }
        return Collections.unmodifiableSet(required);
    }

    /** @return the required gates with their implementation states, in gate order */
    public static List<GateStatus> evaluate(ShaderPackProgramCatalog catalog) {
        Map<Gate, GateStatus> table = implemented();
        Set<Gate> required = requiredFor(catalog);
        List<GateStatus> result = new ArrayList<>();
        for (Gate gate : Gate.values()) {
            if (required.contains(gate)) {
                result.add(table.get(gate));
            }
        }
        return Collections.unmodifiableList(result);
    }
}
