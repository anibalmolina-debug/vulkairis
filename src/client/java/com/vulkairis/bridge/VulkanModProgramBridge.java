package com.vulkairis.bridge;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.vulkairis.core.ShaderLifecycleState;
import com.vulkairis.diagnostics.StageDiagnostics;
import com.vulkairis.translation.ShaderReflection;
import com.vulkairis.translation.ShaderTranslator;
import com.vulkairis.translation.VulkairisProgram;
import com.vulkairis.translation.VulkairisShader;
import com.vulkairis.backend.vulkanmod.VulkanBackendAdapter;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.PipelineConfig;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Links an intercepted Iris program into VulkanMod's rendering system.
 *
 * <p>VulkanMod's renderer can only bind its own {@link Pipeline} type through
 * {@code VkGlProgram.glUseProgram(int)}. Iris uses our virtual program ids as if they were GL
 * program handles, which VulkanMod's registry does not know about, so before this bridge existed
 * {@code glUseProgram} silently did nothing and no shader ever executed (the observed
 * "shaders load but the image never changes" symptom).</p>
 *
 * <p>This bridge compiles the intercepted GLSL into a real VulkanMod {@link GraphicsPipeline},
 * derives its descriptor layout from SPIR-V reflection, and registers it under the virtual program
 * id so that {@code glUseProgram} resolves to a genuine Vulkan pipeline. Failures are reported
 * explicitly — a program is never silently left as a no-op.</p>
 *
 * <p>Prototype limitations (documented, not hidden): the vertex format is chosen per program from
 * a small table of {@link DefaultVertexFormat} constants that covers the shader's declared vertex
 * inputs (smallest first), and uniform/UBO <em>values</em> are not yet transported from Iris's
 * {@code glUniform*} calls into the pipeline's buffers. UBOs are declared as VulkanMod manual UBOs
 * so pipeline creation does not require VulkanMod to own per-uniform suppliers; writing those
 * buffers from Iris state is the next milestone.</p>
 */
public final class VulkanModProgramBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vulkairis/ProgramBridge");

    private static final Pattern SAMPLER_DECL = Pattern.compile(
            "uniform\\s+(sampler\\w+|image\\w+)\\s+(\\w+)\\s*;");

    /** Matches a {@code binding = N} token anywhere in a source; used to verify density. */
    private static final Pattern BINDING_QUALIFIER = Pattern.compile("\\bbinding\\s*=\\s*(\\d+)");

    /**
     * Ordered candidate vertex formats used to cover the vertex inputs an intercepted shader
     * declares. Ordered smallest (fewest elements) first so the first format that contains every
     * required attribute name wins.
     *
     * <p>Element compositions verified against the 1.21.1 {@code DefaultVertexFormat}
     * (Blaze3D) class in this dev environment: {@code POSITION=[Position]};
     * {@code POSITION_COLOR=[Position,Color]}; {@code POSITION_TEX=[Position,UV0]};
     * {@code POSITION_TEX_COLOR=[Position,UV0,Color]};
     * {@code POSITION_COLOR_NORMAL=[Position,Color,Normal]};
     * {@code POSITION_COLOR_LIGHTMAP=[Position,Color,UV2]};
     * {@code POSITION_COLOR_TEX_LIGHTMAP=[Position,Color,UV0,UV2]};
     * {@code POSITION_TEX_LIGHTMAP_COLOR=[Position,UV0,UV2,Color]};
     * {@code POSITION_TEX_COLOR_NORMAL=[Position,UV0,Color,Normal]};
     * {@code BLOCK=[Position,Color,UV0,UV2,Normal]};
     * {@code PARTICLE=[Position,UV0,Color,UV2]};
     * {@code NEW_ENTITY=[Position,Color,UV0,UV1,UV2,Normal]}.</p>
     *
     * <p>Note: 1.21.1 has <em>no</em> {@code POSITION_COLOR_TEX} constant; the
     * position + colour + UV0 format is named {@code POSITION_TEX_COLOR}.</p>
     */
    private static final List<VertexFormat> VERTEX_FORMAT_CANDIDATES = List.of(
            DefaultVertexFormat.POSITION,
            DefaultVertexFormat.POSITION_COLOR,
            DefaultVertexFormat.POSITION_TEX,
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            DefaultVertexFormat.POSITION_TEX_COLOR,
            DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
            DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
            DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR,
            DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL,
            DefaultVertexFormat.BLOCK,
            DefaultVertexFormat.PARTICLE,
            DefaultVertexFormat.NEW_ENTITY);

    private static final Map<Integer, GraphicsPipeline> LINKED = new ConcurrentHashMap<>();
    private static final AtomicInteger CONSECUTIVE_FAILURES = new AtomicInteger();
    private static final int MAX_FAILURES = 3;
    private static volatile boolean disabled = false;
    private static boolean reflectionReady = false;
    private static Constructor<?> glProgramCtor;
    private static Method glProgramBindPipeline;
    private static Field glProgramMap;

    private VulkanModProgramBridge() {
    }

    /**
     * Attempts to link a program. Returns true only if a real Vulkan pipeline was created and
     * registered. Never throws: failures are logged with diagnostics.
     */
    public static boolean link(VulkairisProgram program) {
        if (program == null) return false;
        int id = program.getVirtualProgramId();
        if (LINKED.containsKey(id)) return true;
        if (disabled) return false;

        if (!VulkanBackendAdapter.isLive(VulkanBackendAdapter.getDevice())) {
            StageDiagnostics.warn("Program linking", program.getName() + ": Vulkan device not ready");
            return false;
        }

        VulkairisShader vert = program.getVertexShader();
        VulkairisShader frag = program.getFragmentShader();
        if (vert == null || frag == null) {
            return failLink(program.getName(), "missing vertex or fragment stage");
        }
        if (vert.glslSource() == null || frag.glslSource() == null) {
            return failLink(program.getName(), "missing GLSL source");
        }

        try {
            // preprocessGlsl emits the injected VulkairisUniformBlock at binding 100, which VulkanMod
            // indexes out of range (its descriptor array is sized buffers+images but indexed by the
            // raw binding number). Compact every descriptor into a dense 0..N-1 range before handing
            // the source to VulkanMod, using one shared map so both stages agree on bindings.
            String vertSrc = ShaderTranslator.normalizeTypesForVulkanMod(
                    ShaderTranslator.preprocessGlsl(vert.glslSource(), true));
            String fragSrc = ShaderTranslator.normalizeTypesForVulkanMod(
                    ShaderTranslator.preprocessGlsl(frag.glslSource(), false));

            Map<String, Integer> bindings = buildUnifiedBindingMap(vertSrc, fragSrc);
            // remapDescriptorBindings already routes opaque declarations through
            // ensureDescriptorBindings; the explicit second pass is an idempotent belt-and-braces
            // guarantee that every sampler/image in the source handed to setShaderSrc carries the
            // same dense binding that buildDescriptorConfig() will register. Without it, a bare
            // `uniform sampler2D x;` reaches Shaderc, which rejects it for lacking layout(binding).
            String vertRemapped = ShaderTranslator.ensureDescriptorBindings(
                    ShaderTranslator.remapDescriptorBindings(vertSrc, bindings), bindings);
            String fragRemapped = ShaderTranslator.ensureDescriptorBindings(
                    ShaderTranslator.remapDescriptorBindings(fragSrc, bindings), bindings);

            if (!areBindingsDense(vertRemapped, fragRemapped, bindings.size())) {
                return failLink(program.getName(),
                        "descriptor bindings are not dense 0.." + Math.max(0, bindings.size() - 1)
                                + " after remap (injected block binding " + ShaderTranslator.UNIFORM_BLOCK_BINDING
                                + "); refusing to let VulkanMod throw IndexOutOfBoundsException");
            }

            // VulkanMod compiles the raw source handed to setShaderSrc directly with Shaderc, which
            // rejects any user in/out lacking an explicit location ("SPIR-V requires location for
            // user input/output"). Iris relies on glBindAttribLocation and ships bare declarations,
            // so pick a vertex format that covers the shader's inputs, then inject locations that
            // agree with that format and with the shared varying interface between the stages.
            List<String> requiredInputs = ShaderTranslator.collectVertexInputNames(vertRemapped);
            VertexFormat vertexFormat = chooseVertexFormat(requiredInputs);
            if (vertexFormat == null) {
                return failLink(program.getName(),
                        "no DefaultVertexFormat covers vertex inputs " + requiredInputs
                                + "; refusing to build a pipeline whose SPIR-V would lack locations");
            }
            Map<String, Integer> vertexInputLocations = buildVertexInputLocations(vertexFormat);
            List<String> uncovered = new ArrayList<>();
            for (String name : requiredInputs) {
                if (!vertexInputLocations.containsKey(name)) {
                    uncovered.add(name);
                }
            }
            if (!uncovered.isEmpty()) {
                return failLink(program.getName(),
                        "vertex inputs " + uncovered + " are not covered by the chosen format "
                                + vertexFormat.getElementAttributeNames());
            }
            Map<String, Integer> varyingLocations = buildVaryingLocations(vertRemapped, fragRemapped);

            String vertFinal = ShaderTranslator.injectLayoutLocations(
                    vertRemapped, true, vertexInputLocations, varyingLocations);
            String fragFinal = ShaderTranslator.injectLayoutLocations(
                    fragRemapped, false, vertexInputLocations, varyingLocations);

            Pipeline.Builder builder = Pipeline.builder(vertexFormat, program.getName());
            builder.setShaderSrc(SPIRVUtils.ShaderKind.VERTEX_SHADER, vertFinal);
            builder.setShaderSrc(SPIRVUtils.ShaderKind.FRAGMENT_SHADER, fragFinal);

            PipelineConfig config = buildDescriptorConfig(vert, frag, bindings, vertSrc, fragSrc, vertRemapped, fragRemapped);
            if (config == null) {
                return failLink(program.getName(),
                        "could not build a config entry for every descriptor binding; refusing to leave a"
                                + " gap that VulkanMod would index out of range");
            }
            builder.applyConfig(config);

            GraphicsPipeline pipeline;
            try {
                pipeline = builder.createGraphicsPipeline();
            } catch (Throwable t) {
                dumpFailedSource(program.getName(), vertFinal, fragFinal);
                throw t;
            }
            LINKED.put(id, pipeline);

            if (!registerWithGlProgram(id, pipeline)) {
                LINKED.remove(id);
                pipeline.scheduleCleanUp();
                return failLink(program.getName(), "could not register VkGlProgram " + id);
            }

            CONSECUTIVE_FAILURES.set(0);
            program.advanceLifecycle(ShaderLifecycleState.PIPELINE_CREATED);
            StageDiagnostics.pass("Program linking", program.getName()
                    + " -> VkGlProgram " + id + " (" + pipeline.getImageDescriptors().size()
                    + " images, " + pipeline.getBuffers().size() + " UBOs)");
            return true;
        } catch (Throwable t) {
            return failLink(program.getName(), t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName(), t);
        }
    }

    /**
     * Writes the final VulkanMod-bound vertex/fragment sources to {@code run/vulkairis/debug/} when
     * pipeline creation fails, so a Shaderc error line number can be resolved against the exact
     * text handed to VulkanMod. Development aid; failures here must never mask the original error.
     */
    private static void dumpFailedSource(String name, String vertFinal, String fragFinal) {
        try {
            java.nio.file.Path dir = net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getGameDir().resolve("vulkairis").resolve("debug");
            java.nio.file.Files.createDirectories(dir);
            String safe = name.replaceAll("[^A-Za-z0-9_.-]", "_");
            java.nio.file.Files.writeString(dir.resolve(safe + ".vert.glsl"), vertFinal);
            java.nio.file.Files.writeString(dir.resolve(safe + ".frag.glsl"), fragFinal);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Records a link failure, warns only for the first few programs, then disables the feature for
     * the session so a pack that the VulkanMod SPIR-V compiler cannot handle does not flood the log.
     * The originating throwable is logged with its full stack trace so future failures are
     * diagnosable from a single log capture.
     */
    private static boolean failLink(String name, String reason) {
        return failLink(name, reason, null);
    }

    /**
     * Variant that carries the originating {@link Throwable}. The stack trace is logged once per
     * failing program (alongside the human-readable reason) while keeping the existing
     * failure-count disabling behaviour.
     */
    private static boolean failLink(String name, String reason, Throwable cause) {
        int n = CONSECUTIVE_FAILURES.incrementAndGet();
        if (n <= MAX_FAILURES) {
            if (cause != null) {
                LOGGER.warn("[Vulkairis/ProgramBridge] Program linking failed for '{}': {}", name, reason, cause);
            }
            StageDiagnostics.warn("Program linking", name + ": " + reason);
        }
        if (n == MAX_FAILURES) {
            disabled = true;
            StageDiagnostics.warn("Program linking",
                    "disabling Iris pipeline linking for this session after " + n
                            + " failures; VulkanMod's SPIR-V compiler currently rejects this pack's GLSL");
        }
        return false;
    }

    /**
     * Builds one unified, deterministic descriptor assignment from the descriptors actually declared
     * in both (already preprocessed) stages. Order is: the injected {@link ShaderTranslator#UNIFORM_BLOCK_NAME}
     * first (if either stage declares it), then every other descriptor in first-seen declaration order
     * (vertex stage before fragment stage). Values are exactly {@code 0..N-1}.
     *
     * <p>Never returns {@code null}: an empty program simply yields an empty map.</p>
     */
    private static Map<String, Integer> buildUnifiedBindingMap(String vertSrc, String fragSrc) {
        Map<String, Integer> bindings = new LinkedHashMap<>();

        // The injected block is one shared descriptor across both stages; pin it to binding 0 first
        // regardless of which stage (or both) declares it, so the unified order matches the
        // single-stage remap contract.
        if (ShaderTranslator.collectDescriptorNames(vertSrc).contains(ShaderTranslator.UNIFORM_BLOCK_NAME)
                || ShaderTranslator.collectDescriptorNames(fragSrc).contains(ShaderTranslator.UNIFORM_BLOCK_NAME)) {
            bindings.put(ShaderTranslator.UNIFORM_BLOCK_NAME, 0);
        }

        for (String source : List.of(vertSrc, fragSrc)) {
            for (String name : ShaderTranslator.collectDescriptorNames(source)) {
                if (!bindings.containsKey(name)) {
                    bindings.put(name, bindings.size());
                }
            }
        }
        return bindings;
    }

    /**
     * Verifies that the union of binding numbers used across both rewritten stages is exactly
     * {@code 0..count-1}, each used at least once and none out of range. This catches a descriptor
     * the remap failed to recognise (for example an unexpected layout form), which would otherwise
     * reach VulkanMod and throw {@code IndexOutOfBoundsException}.
     */
    private static boolean areBindingsDense(String vertSrc, String fragSrc, int count) {
        Set<Integer> seen = new HashSet<>();
        for (String source : List.of(vertSrc, fragSrc)) {
            Matcher matcher = BINDING_QUALIFIER.matcher(source);
            while (matcher.find()) {
                int binding;
                try {
                    binding = Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException e) {
                    return false;
                }
                if (binding < 0 || binding >= count) {
                    return false;
                }
                seen.add(binding);
            }
        }
        return seen.size() == count;
    }

    /**
     * Picks the smallest {@link DefaultVertexFormat} from {@link #VERTEX_FORMAT_CANDIDATES} whose
     * element attribute names contain every required vertex input name.
     *
     * @return the first covering format, or {@code null} if none covers all required names
     */
    private static VertexFormat chooseVertexFormat(List<String> requiredInputs) {
        Set<String> required = new HashSet<>(requiredInputs);
        for (VertexFormat format : VERTEX_FORMAT_CANDIDATES) {
            if (new HashSet<>(format.getElementAttributeNames()).containsAll(required)) {
                return format;
            }
        }
        return null;
    }

    /**
     * Maps each element attribute name of {@code format} to its index in the format's element list.
     *
     * <p>VulkanMod's {@code GraphicsPipeline.getAttributeDescriptions} assigns
     * {@code VkVertexInputAttributeDescription.location = i} for the element at list index
     * {@code i}, so these indices are exactly the shader input locations the SPIR-V must declare.</p>
     */
    private static Map<String, Integer> buildVertexInputLocations(VertexFormat format) {
        Map<String, Integer> locations = new LinkedHashMap<>();
        List<String> names = format.getElementAttributeNames();
        for (int i = 0; i < names.size(); i++) {
            locations.putIfAbsent(names.get(i), i);
        }
        return locations;
    }

    /**
     * Builds the single varying-location map shared by both stages: every vertex {@code out} name in
     * declaration order, then any fragment {@code in} names not already present. Sharing the map
     * guarantees a vertex output and its matching fragment input receive the same location.
     */
    private static Map<String, Integer> buildVaryingLocations(String vertSrc, String fragSrc) {
        Map<String, Integer> varyings = new LinkedHashMap<>();
        int nextLocation = 0;
        for (Map.Entry<String, Integer> entry : ShaderTranslator.collectInterfaceSlots(vertSrc, "out").entrySet()) {
            varyings.put(entry.getKey(), nextLocation);
            nextLocation += entry.getValue();
        }
        for (Map.Entry<String, Integer> entry : ShaderTranslator.collectInterfaceSlots(fragSrc, "in").entrySet()) {
            if (!varyings.containsKey(entry.getKey())) {
                varyings.put(entry.getKey(), nextLocation);
                nextLocation += entry.getValue();
            }
        }
        return varyings;
    }

    /**
     * Builds the VulkanMod descriptor config from the shared dense binding map. Samplers and the
     * injected uniform block are emitted at their remapped bindings (never their raw reflected
     * bindings), and the block is emitted once even when both stages declare it, with the stage
     * flags merged.
     *
     * <p>Returns {@code null} if any descriptor named in {@code bindings} cannot be emitted. Dense
     * bindings only stay safe when the config has exactly one entry per binding: a skipped entry
     * would leave an uninitialised slot while a higher binding is still indexed, which is the same
     * {@code IndexOutOfBoundsException} this remap exists to prevent.</p>
     */
    private static PipelineConfig buildDescriptorConfig(VulkairisShader vert, VulkairisShader frag,
                                                        Map<String, Integer> bindings,
                                                        String vertSrc, String fragSrc,
                                                        String vertRemapped, String fragRemapped) {
        PipelineConfig.Builder config = PipelineConfig.builder();
        String mergedSource = vertRemapped + "\n" + fragRemapped;

        // Reflection ran on the pre-remap sources, so a reflected block still carries its raw
        // binding. Correlate a descriptor with its reflected block by that raw binding (robust to a
        // named block instance, whose reflected variable name is the instance, not the block type).
        Map<String, Integer> rawBindings = new LinkedHashMap<>();
        for (String source : List.of(vertSrc, fragSrc)) {
            for (Map.Entry<String, Integer> raw : ShaderTranslator.collectDescriptorBindings(source).entrySet()) {
                rawBindings.putIfAbsent(raw.getKey(), raw.getValue());
            }
        }

        int imageIdx = 0;
        for (Map.Entry<String, Integer> entry : bindings.entrySet()) {
            String name = entry.getKey();
            int binding = entry.getValue();

            if (findSampler(vert, frag, name) != null) {
                try {
                    String type = samplerGlslType(mergedSource, name);
                    config.addImageDescriptor(binding, type, name, imageIdx++);
                } catch (Throwable t) {
                    LOGGER.warn("[Vulkairis/ProgramBridge] Could not add sampler '{}' at binding {}: {}",
                            name, binding, t.getMessage());
                    return null;
                }
                continue;
            }

            // Not a sampler: treat as a reflected interface block (the injected block or a
            // pack-declared one). Merge stage flags/size across the stages that declare it.
            Integer rawBinding = ShaderTranslator.UNIFORM_BLOCK_NAME.equals(name)
                    ? ShaderTranslator.UNIFORM_BLOCK_BINDING
                    : rawBindings.get(name);
            if (rawBinding == null) {
                LOGGER.warn("[Vulkairis/ProgramBridge] Descriptor '{}' (binding {}) has no raw binding",
                        name, binding);
                return null;
            }

            int stage = 0;
            int size = 0;
            boolean found = false;
            for (VulkairisShader shader : List.of(vert, frag)) {
                for (ShaderReflection.UniformBlock block : shader.getReflection().getUniformBlocks()) {
                    if (!matchesBlock(block, name, rawBinding)) {
                        continue;
                    }
                    found = true;
                    stage |= block.stage();
                    size = Math.max(size, block.size());
                }
            }
            if (!found) {
                LOGGER.warn("[Vulkairis/ProgramBridge] Descriptor '{}' (binding {}, raw {}) has no reflected block",
                        name, binding, rawBinding);
                return null;
            }
            try {
                if (stage == 0) {
                    stage = 0x1 | 0x10; // VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT
                }
                // A manual UBO (binding/stage/size, no members) so VulkanMod skips its uniform-supplier
                // lookup; Vulkairis owns writing this buffer.
                PipelineConfig.UB.Builder ub = PipelineConfig.UB.builder(binding, stage);
                ub.setSize(Math.max(size, 16));
                config.addUB(ub.build());
            } catch (Throwable t) {
                LOGGER.warn("[Vulkairis/ProgramBridge] Could not add UBO '{}' at binding {}: {}",
                        name, binding, t.getMessage());
                return null;
            }
        }

        PipelineConfig built = config.build();
        int entries = built.ubs.size() + built.imageDescriptors.size();
        if (entries != bindings.size()) {
            LOGGER.warn("[Vulkairis/ProgramBridge] Descriptor config has {} entries for {} dense bindings",
                    entries, bindings.size());
            return null;
        }
        return built;
    }

    private static ShaderReflection.SamplerBinding findSampler(VulkairisShader vert, VulkairisShader frag, String name) {
        ShaderReflection.SamplerBinding sampler = vert.getReflection().getSamplers().get(name);
        if (sampler == null) {
            sampler = frag.getReflection().getSamplers().get(name);
        }
        return sampler;
    }

    /**
     * Whether a reflected block corresponds to the descriptor named {@code name}. Matches by reflected
     * variable name when possible, but falls back to the pre-remap raw binding so a named block
     * instance (whose reflected name is the instance, not the GLSL block type) still resolves.
     */
    private static boolean matchesBlock(ShaderReflection.UniformBlock block, String name, int rawBinding) {
        return block.name().equals(name) || block.binding() == rawBinding;
    }

    private static String samplerGlslType(String source, String name) {
        if (source != null) {
            Matcher matcher = SAMPLER_DECL.matcher(source);
            while (matcher.find()) {
                if (matcher.group(2).equals(name)) {
                    String glslType = matcher.group(1);
                    return glslType.startsWith("image") ? "image2D" : "sampler2D";
                }
            }
        }
        return "sampler2D";
    }

    /**
     * Inserts a {@code VkGlProgram} entry for the virtual id via reflection, so VulkanMod's own
     * {@code glUseProgram} path binds the pipeline at the correct point in the frame.
     */
    private static boolean registerWithGlProgram(int id, Pipeline pipeline) {
        try {
            Class<?> glProgramClass = Class.forName("net.vulkanmod.gl.VkGlProgram");
            if (!reflectionReady) {
                glProgramCtor = glProgramClass.getDeclaredConstructor(int.class);
                glProgramCtor.setAccessible(true);
                glProgramBindPipeline = glProgramClass.getMethod("bindPipeline", Pipeline.class);
                glProgramMap = glProgramClass.getDeclaredField("map");
                glProgramMap.setAccessible(true);
                reflectionReady = true;
            }

            Object glProgram = glProgramCtor.newInstance(id);
            glProgramBindPipeline.invoke(glProgram, pipeline);

            Object map = glProgramMap.get(null);
            if (map instanceof Map<?, ?> raw) {
                @SuppressWarnings("unchecked")
                Map<Object, Object> typed = (Map<Object, Object>) raw;
                typed.put(id, glProgram);
                return true;
            }
            return false;
        } catch (Throwable t) {
            LOGGER.warn("[Vulkairis/ProgramBridge] VkGlProgram registration failed for id {}: {}", id, t.getMessage());
            return false;
        }
    }

    public static GraphicsPipeline getLinkedPipeline(int virtualProgramId) {
        return LINKED.get(virtualProgramId);
    }

    // ------------------------------------------------------------------
    // Phase D terrain door (narrow additions; existing link() behavior unchanged).
    //
    // The frozen SodiumPrograms path asks here, by pack program name, whether a VulkanMod
    // pipeline is already linked for the handed terrain pass. Registration is explicit:
    // linkTerrainProgram() records the virtual id a successful link() produced, so the
    // door's HEAD decision (TerrainPipelineDoor.decideCompile) can return the pipeline
    // handle path without re-solving descriptors every draw.
    // ------------------------------------------------------------------

    private static final Map<String, Integer> TERRAIN_PROGRAM_IDS = new ConcurrentHashMap<>();

    /**
     * Records the virtual program id backing a terrain pack program.
     *
     * @param programName pack program name (e.g. {@code "gbuffers_terrain"})
     * @param virtualProgramId linked virtual id, or {@code <= 0} to clear the entry
     */
    public static void noteTerrainProgram(String programName, int virtualProgramId) {
        if (programName == null || programName.isBlank()) {
            return;
        }
        if (virtualProgramId <= 0) {
            TERRAIN_PROGRAM_IDS.remove(programName);
        } else {
            TERRAIN_PROGRAM_IDS.put(programName, virtualProgramId);
        }
    }

    /**
     * @param programName pack program name
     * @return the linked virtual id with a live pipeline, or 0 when none
     */
    public static int terrainLinkedId(String programName) {
        if (programName == null || programName.isBlank()) {
            return 0;
        }
        Integer id = TERRAIN_PROGRAM_IDS.get(programName);
        if (id == null || id <= 0) {
            return 0;
        }
        return LINKED.containsKey(id) ? id : 0;
    }

    /**
     * Head-of-compile terrain decision for an Iris pass name.
     *
     * @param irisPassName handed pass name (e.g. {@code "TERRAIN"})
     * @return pack decision when a live pipeline is linked, otherwise a fallback decision
     *         naming the missing pipeline
     */
    public static com.vulkairis.vulkan.pipeline.TerrainPipelineDoor.DoorDecision terrainCompileDecision(
            String irisPassName) {
        com.vulkairis.vulkan.pipeline.TerrainPipelineDoor.TerrainPass pass =
                com.vulkairis.vulkan.pipeline.TerrainPipelineDoor.forPassName(irisPassName);
        String program = com.vulkairis.vulkan.pipeline.TerrainPipelineDoor.programNameFor(pass);
        return com.vulkairis.vulkan.pipeline.TerrainPipelineDoor.decideCompile(pass, terrainLinkedId(program));
    }

    /** Clears terrain program registrations. Test/shutdown helper; leaves LINKED untouched. */
    public static void clearTerrainRegistrations() {
        TERRAIN_PROGRAM_IDS.clear();
    }

    public static void unlink(int virtualProgramId) {
        GraphicsPipeline pipeline = LINKED.remove(virtualProgramId);
        if (pipeline != null) {
            try {
                pipeline.scheduleCleanUp();
            } catch (Throwable ignored) {
            }
        }
    }

    public static void cleanUp() {
        for (GraphicsPipeline pipeline : LINKED.values()) {
            try {
                pipeline.scheduleCleanUp();
            } catch (Throwable ignored) {
            }
        }
        LINKED.clear();
    }
}
