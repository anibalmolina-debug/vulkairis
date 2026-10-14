package com.vulkairis.translation;

import com.vulkairis.translation.lexer.GlslLexer;
import com.vulkairis.translation.lexer.GlslToken;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Phase B: legacy-GLSL builtin coverage for the program-together translator.
 *
 * <p>OptiFine-format packs target the compatibility profile: {@code texture2D*},
 * {@code shadow2D*}, {@code gl_FragData}/{@code gl_FragColor}, {@code gl_TextureMatrix},
 * {@code ftransform()} and the other fixed-function inputs. Shaderc targeting Vulkan rejects
 * every one of them (see the fix5 dry-run errors), so they are rewritten to core-profile
 * equivalents <em>before</em> any binding/location policy runs.</p>
 *
 * <p>Semantics (reimplemented for this codebase, token-based so comments, string literals and
 * preprocessor directives are never touched):</p>
 * <ul>
 *   <li>Levelled/planar lookups map to their core callee with identical arguments:
 *       {@code texture2D}&#8594;{@code texture}, {@code texture2DLod}&#8594;{@code textureLod},
 *       {@code texture2DProj}&#8594;{@code textureProj}, {@code texture2DProjLod}&#8594;
 *       {@code textureProjLod}, {@code texture2DBias}&#8594;{@code texture},
 *       {@code textureCube}/{@code texture3D} families likewise, {@code texture2DRect}&#8594;
 *       {@code texture}.</li>
 *   <li>Depth-compare lookups are <em>wrapped, not renamed</em>: {@code shadow2D}&#8594;
 *       {@code texture}, {@code shadow2DProj}&#8594;{@code textureProj},
 *       {@code shadow2DLod}&#8594;{@code textureLod}. The sampler stays a
 *       {@code sampler*Shadow} and the argument list is preserved, so the comparison still
 *       happens in the sampler.</li>
 *   <li>Depth/volume handling: 3D (volume) lookups modernize the same way; {@code gl_FragDepth}
 *       and {@code gl_FragCoord} are already core and pass through untouched.</li>
 *   <li>{@code gl_FragColor} becomes indexed output {@code 0}; each literal
 *       {@code gl_FragData[n]} becomes {@code vulkairisFragDataN} and its index is recorded.
 *       Declarations are <em>not</em> emitted here; {@link ProgramTranslator} declares the
 *       indexed outputs with fragment-output locations.</li>
 *   <li>Fixed-function state maps to explicit {@code vulkairis_}-prefixed declarations:
 *       {@code gl_TextureMatrix}&#8594;{@code vulkairis_TextureMatrix} (mat4 array uniform),
 *       {@code gl_ModelViewProjectionMatrix}/{@code gl_ModelViewMatrix}/
 *       {@code gl_ProjectionMatrix}/{@code gl_NormalMatrix} to same-named uniforms,
 *       {@code gl_Vertex}/{@code gl_Normal}/{@code gl_Color}/{@code gl_MultiTexCoord0..1} to
 *       same-named vertex attributes, and {@code ftransform()} to
 *       {@code (vulkairis_ModelViewProjectionMatrix * vulkairis_Vertex)}.</li>
 *   <li>Varying-split helpers scalarize matrix/array varyings and account location slots
 *       (one slot per column/element), so the union-varying pass can verify that a split
 *       interface never collides with itself.</li>
 * </ul>
 *
 * <p>All rewrites are pure, deterministic and idempotent: mapped names never match the
 * mapping table again. Anything outside the table (for example {@code gl_Fog} or
 * {@code gl_LightSource}) is deliberately left for the compiler to report rather than
 * guessed at.</p>
 */
public final class LegacyGlsl {

    private LegacyGlsl() {
    }

    /** Prefix for every identifier this pass introduces. */
    public static final String INTRODUCED_PREFIX = "vulkairis";

    /** Indexed fragment-output base name: {@code gl_FragData[n]} becomes this plus {@code n}. */
    public static final String FRAG_DATA_BASE = "vulkairisFragData";

    /** How many texture units the compatibility {@code gl_TextureMatrix} array exposes. */
    public static final int TEXTURE_MATRIX_UNITS = 8;

    /**
     * Aggregate result of {@link #translateLegacyBuiltins(String, boolean)}.
     *
     * @param source             the rewritten source
     * @param fragDataIndices    sorted literal {@code gl_FragData[n]} indices seen (plus
     *                           {@code 0} when {@code gl_FragColor} was rewritten)
     * @param needsTextureMatrix whether {@code vulkairis_TextureMatrix} is referenced
     * @param needsModelViewProjection whether {@code vulkairis_ModelViewProjectionMatrix} is referenced
     * @param needsModelView     whether {@code vulkairis_ModelViewMatrix} is referenced
     * @param needsProjection    whether {@code vulkairis_ProjectionMatrix} is referenced
     * @param needsNormalMatrix  whether {@code vulkairis_NormalMatrix} is referenced
     * @param needsVertexAttrib  whether {@code vulkairis_Vertex} is referenced
     * @param needsNormalAttrib  whether {@code vulkairis_Normal} is referenced
     * @param needsColorAttrib   whether {@code vulkairis_Color} is referenced
     * @param needsTexCoord0     whether {@code vulkairis_MultiTexCoord0} is referenced
     * @param needsTexCoord1     whether {@code vulkairis_MultiTexCoord1} is referenced
     */
    public record LegacyResult(
            String source,
            Set<Integer> fragDataIndices,
            boolean needsTextureMatrix,
            boolean needsModelViewProjection,
            boolean needsModelView,
            boolean needsProjection,
            boolean needsNormalMatrix,
            boolean needsVertexAttrib,
            boolean needsNormalAttrib,
            boolean needsColorAttrib,
            boolean needsTexCoord0,
            boolean needsTexCoord1) {
    }

    /**
     * Runs every legacy mapping over one stage source.
     *
     * @param source   GLSL source; may be {@code null}
     * @param isVertex stage selector (currently informational; mappings are stage-agnostic)
     * @return the rewritten source plus the set of introduced requirements
     */
    public static LegacyResult translateLegacyBuiltins(String source, boolean isVertex) {
        if (source == null) {
            return new LegacyResult(null, Set.of(),
                    false, false, false, false, false, false, false, false, false, false);
        }
        List<GlslToken> tokens;
        try {
            tokens = GlslLexer.tokenize(source);
        } catch (RuntimeException lexFailure) {
            return new LegacyResult(source, Set.of(),
                    false, false, false, false, false, false, false, false, false, false);
        }

        Set<Integer> fragData = new TreeSet<>();
        boolean[] flags = new boolean[10];
        List<GlslToken> rewritten = rewriteTokens(tokens, fragData, flags);

        return new LegacyResult(GlslLexer.emit(rewritten), Set.copyOf(fragData),
                flags[0], flags[1], flags[2], flags[3], flags[4],
                flags[5], flags[6], flags[7], flags[8], flags[9]);
    }

    /**
     * Rewrites only the texture/shadow/volume lookup callees.
     *
     * @param source GLSL source; may be {@code null}
     * @return the rewritten source, or the input when {@code null}
     */
    public static String rewriteTextureLookups(String source) {
        return translateLegacyBuiltins(source, false).source();
    }

    /**
     * Collects the literal {@code gl_FragData[n]} indices used by {@code source}, in order.
     *
     * @param source GLSL source; may be {@code null}
     * @return sorted indices; empty for {@code null} input or when none are used
     */
    public static Set<Integer> collectFragDataIndices(String source) {
        if (source == null) {
            return Set.of();
        }
        List<GlslToken> tokens;
        try {
            tokens = GlslLexer.tokenize(source);
        } catch (RuntimeException lexFailure) {
            return Set.of();
        }
        Set<Integer> indices = new TreeSet<>();
        for (int i = 0; i < tokens.size(); i++) {
            GlslToken token = tokens.get(i);
            if (!token.isCodeIdentifier("gl_FragData")) {
                continue;
            }
            Integer index = bracketedLiteral(tokens, i);
            if (index != null) {
                indices.add(index);
            }
        }
        return Set.copyOf(indices);
    }

    /**
     * Declares one indexed fragment output per index, for the fragment stage only.
     *
     * @param indices literal indices (from {@link #collectFragDataIndices} or a
     *                {@link LegacyResult}); may be {@code null}/empty
     * @return {@code layout(location = n) out vec4 vulkairisFragDataN;} lines, or {@code ""}
     */
    public static String fragOutputDeclarations(Set<Integer> indices) {
        if (indices == null || indices.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int index : new TreeSet<>(indices)) {
            out.append("layout(location = ").append(index).append(") out vec4 ")
                    .append(FRAG_DATA_BASE).append(index).append(";\n");
        }
        return out.toString();
    }

    // ------------------------------------------------------------------
    // Token rewrite engine.
    // ------------------------------------------------------------------

    /** Flag slots into {@link #translateLegacyBuiltins} bookkeeping, in declaration order. */
    private static final int FLAG_TEXTURE_MATRIX = 0;
    private static final int FLAG_MVP = 1;
    private static final int FLAG_MODELVIEW = 2;
    private static final int FLAG_PROJECTION = 3;
    private static final int FLAG_NORMAL_MATRIX = 4;
    private static final int FLAG_VERTEX = 5;
    private static final int FLAG_NORMAL = 6;
    private static final int FLAG_COLOR = 7;
    private static final int FLAG_TEXCOORD0 = 8;
    private static final int FLAG_TEXCOORD1 = 9;

    private static List<GlslToken> rewriteTokens(
            List<GlslToken> tokens, Set<Integer> fragData, boolean[] flags) {
        List<GlslToken> out = new ArrayList<>(tokens.size());
        int i = 0;
        while (i < tokens.size()) {
            GlslToken token = tokens.get(i);

            if (token.kind() == GlslToken.Kind.IDENTIFIER && token.isCode()) {
                // Indexed fragment outputs: gl_FragData[<literal>] -> vulkairisFragData<literal>.
                if (token.text().equals("gl_FragData")) {
                    Integer index = bracketedLiteral(tokens, i);
                    if (index != null) {
                        int close = bracketClose(tokens, i);
                        out.add(new GlslToken(GlslToken.Kind.IDENTIFIER,
                                FRAG_DATA_BASE + index, token.line(), token.column(), token.offset()));
                        fragData.add(index);
                        i = close + 1;
                        continue;
                    }
                    out.add(token);
                    i++;
                    continue;
                }
                if (token.text().equals("gl_FragColor")) {
                    out.add(new GlslToken(GlslToken.Kind.IDENTIFIER,
                            FRAG_DATA_BASE + "0", token.line(), token.column(), token.offset()));
                    fragData.add(0);
                    i++;
                    continue;
                }
                // ftransform() takes no arguments; the empty call pair is consumed with it.
                if (token.text().equals("ftransform")) {
                    int close = emptyCallClose(tokens, i);
                    if (close >= 0) {
                        flags[FLAG_MVP] = true;
                        flags[FLAG_VERTEX] = true;
                        out.add(new GlslToken(GlslToken.Kind.IDENTIFIER,
                                "(vulkairis_ModelViewProjectionMatrix * vulkairis_Vertex)",
                                token.line(), token.column(), token.offset()));
                        i = close + 1;
                        continue;
                    }
                    out.add(token);
                    i++;
                    continue;
                }
                String mapped = mappedCallee(token.text());
                if (mapped != null) {
                    out.add(new GlslToken(GlslToken.Kind.IDENTIFIER,
                            mapped, token.line(), token.column(), token.offset()));
                    i++;
                    continue;
                }
                int flag = fixedFunctionFlag(token.text());
                if (flag >= 0) {
                    flags[flag] = true;
                    out.add(new GlslToken(GlslToken.Kind.IDENTIFIER,
                            "vulkairis_" + token.text().substring(3),
                            token.line(), token.column(), token.offset()));
                    i++;
                    continue;
                }
            }
            out.add(token);
            i++;
        }
        return out;
    }

    /**
     * Maps one legacy lookup callee to its core equivalent, or {@code null} to leave it alone.
     * Introduced names never match this table, which keeps the pass idempotent.
     */
    private static String mappedCallee(String name) {
        return switch (name) {
            case "texture2D" -> "texture";
            case "texture2DProj" -> "textureProj";
            case "texture2DProjLod" -> "textureProjLod";
            case "texture2DLod" -> "textureLod";
            case "texture2DBias" -> "texture";
            case "texture2DRect" -> "texture";
            case "textureCube" -> "texture";
            case "textureCubeLod" -> "textureLod";
            case "textureCubeBias" -> "texture";
            case "texture3D" -> "texture";
            case "texture3DLod" -> "textureLod";
            case "texture3DBias" -> "texture";
            case "texture3DProj" -> "textureProj";
            case "shadow2D" -> "texture";
            case "shadow2DLod" -> "textureLod";
            case "shadow2DProj" -> "textureProj";
            default -> null;
        };
    }

    /** Maps a fixed-function {@code gl_} identifier to its requirement-flag slot, or -1. */
    private static int fixedFunctionFlag(String name) {
        return switch (name) {
            case "gl_TextureMatrix" -> FLAG_TEXTURE_MATRIX;
            case "gl_ModelViewProjectionMatrix" -> FLAG_MVP;
            case "gl_ModelViewMatrix" -> FLAG_MODELVIEW;
            case "gl_ProjectionMatrix" -> FLAG_PROJECTION;
            case "gl_NormalMatrix" -> FLAG_NORMAL_MATRIX;
            case "gl_Vertex" -> FLAG_VERTEX;
            case "gl_Normal" -> FLAG_NORMAL;
            case "gl_Color" -> FLAG_COLOR;
            case "gl_MultiTexCoord0" -> FLAG_TEXCOORD0;
            case "gl_MultiTexCoord1" -> FLAG_TEXCOORD1;
            default -> -1;
        };
    }

    /** Skips trivia (whitespace/comments/newlines) forward from {@code from}. */
    private static int skipTrivia(List<GlslToken> tokens, int from) {
        int i = from;
        while (i < tokens.size() && tokens.get(i).isTrivia()) {
            i++;
        }
        return i;
    }

    /** Whether the token is the exact single-character symbol {@code text}. */
    private static boolean isSymbol(List<GlslToken> tokens, int index, String text) {
        return index < tokens.size()
                && tokens.get(index).kind() == GlslToken.Kind.SYMBOL
                && tokens.get(index).text().equals(text);
    }

    /**
     * Reads {@code gl_FragData[<integer literal>]} at {@code identifierIndex}.
     *
     * @return the literal index, or {@code null} when the subscript is absent, non-literal
     *         or unbalanced (the identifier is then left untouched)
     */
    private static Integer bracketedLiteral(List<GlslToken> tokens, int identifierIndex) {
        int open = skipTrivia(tokens, identifierIndex + 1);
        if (!isSymbol(tokens, open, "[")) {
            return null;
        }
        int number = skipTrivia(tokens, open + 1);
        if (number >= tokens.size() || tokens.get(number).kind() != GlslToken.Kind.NUMBER) {
            return null;
        }
        int close = skipTrivia(tokens, number + 1);
        if (!isSymbol(tokens, close, "]")) {
            return null;
        }
        try {
            int value = Integer.parseInt(tokens.get(number).text().trim());
            return value >= 0 ? value : null;
        } catch (NumberFormatException notLiteral) {
            return null;
        }
    }

    /** Index of the {@code ]} closing {@code gl_FragData[...]} at {@code identifierIndex}, or -1. */
    private static int bracketClose(List<GlslToken> tokens, int identifierIndex) {
        int open = skipTrivia(tokens, identifierIndex + 1);
        int number = skipTrivia(tokens, open + 1);
        return skipTrivia(tokens, number + 1);
    }

    /**
     * Index of the {@code )} closing an empty {@code ftransform()} call at
     * {@code identifierIndex}, or -1 when arguments or unbalanced parentheses follow.
     */
    private static int emptyCallClose(List<GlslToken> tokens, int identifierIndex) {
        int open = skipTrivia(tokens, identifierIndex + 1);
        if (!isSymbol(tokens, open, "(")) {
            return -1;
        }
        int close = skipTrivia(tokens, open + 1);
        return isSymbol(tokens, close, ")") ? close : -1;
    }

    // ------------------------------------------------------------------
    // Varying-split helpers.
    // ------------------------------------------------------------------

    /**
     * One scalarized leaf of a split varying declaration.
     *
     * @param name  leaf identifier (for example {@code shadowMatrix_c2} or {@code texCoords_1})
     * @param type  leaf GLSL type (always a scalar or vector, never a matrix or array)
     * @param slots location slots this leaf consumes (always 1)
     */
    public record SplitLeaf(String name, String type, int slots) {
    }

    /**
     * Counts the SPIR-V location slots consumed by one varying declarator.
     *
     * <p>Vectors/scalars consume 1 slot, matrices consume 1 slot per column, and arrayed
     * declarations multiply by the element count. This is the rank==location accounting the
     * union-varying pass uses to keep split leaves collision-free.</p>
     *
     * @param type        GLSL type name (for example {@code vec3}, {@code mat4})
     * @param arrayLength array element count, or {@code 1} when not an array
     * @return slots consumed, at least 1
     */
    public static int locationSlotsFor(String type, int arrayLength) {
        int columns = switch (type) {
            case "mat2", "dmat2", "mat2x2", "dmat2x2" -> 2;
            case "mat3", "dmat3", "mat3x3", "dmat3x3" -> 3;
            case "mat4", "dmat4", "mat4x4", "dmat4x4" -> 4;
            case "mat2x3", "mat2x4", "dmat2x3", "dmat2x4" -> 2;
            case "mat3x2", "mat3x4", "dmat3x2", "dmat3x4" -> 3;
            case "mat4x2", "mat4x3", "dmat4x2", "dmat4x3" -> 4;
            default -> 1;
        };
        return Math.max(1, columns) * Math.max(1, arrayLength);
    }

    /**
     * Scalarizes one varying declarator into collision-free leaves.
     *
     * <ul>
     *   <li>Matrices become one {@code vec<rows>} leaf per column
     *       ({@code shadowMatrix} &#8594; {@code shadowMatrix_c0..c3}).</li>
     *   <li>Arrays of scalars/vectors become one leaf per element
     *       ({@code texCoords[3]} &#8594; {@code texCoords_0..2}).</li>
     *   <li>Anything else (scalars, vectors, structs without member metadata) stays one leaf.</li>
     * </ul>
     *
     * @param type        GLSL type name
     * @param baseName    declared identifier
     * @param arrayLength array element count, or {@code 1} when not an array
     * @return leaves in declaration order; never empty
     */
    public static List<SplitLeaf> splitVarying(String type, String baseName, int arrayLength) {
        String base = baseName != null ? baseName : "varying";
        String kind = type != null ? type : "vec4";
        int elements = Math.max(1, arrayLength);

        if (isMatrixType(kind)) {
            List<SplitLeaf> leaves = new ArrayList<>();
            int columns = locationSlotsFor(kind, 1);
            String columnType = "vec" + matrixRows(kind);
            for (int c = 0; c < columns; c++) {
                for (int e = 0; e < elements; e++) {
                    String leaf = base + "_c" + c + (elements > 1 ? "_" + e : "");
                    leaves.add(new SplitLeaf(leaf, columnType, 1));
                }
            }
            return List.copyOf(leaves);
        }
        if (elements > 1) {
            List<SplitLeaf> leaves = new ArrayList<>(elements);
            for (int e = 0; e < elements; e++) {
                leaves.add(new SplitLeaf(base + "_" + e, kind, 1));
            }
            return List.copyOf(leaves);
        }
        return List.of(new SplitLeaf(base, kind, 1));
    }

    /**
     * Scalarizes a struct varying whose member types are known.
     *
     * @param baseName    declared identifier
     * @param memberTypes member GLSL types in order; may be {@code null}/empty
     * @return one leaf per member ({@code base_member<index>}), or the single base leaf
     */
    public static List<SplitLeaf> splitStructVarying(String baseName, List<String> memberTypes) {
        String base = baseName != null ? baseName : "varying";
        if (memberTypes == null || memberTypes.isEmpty()) {
            return List.of(new SplitLeaf(base, "vec4", 1));
        }
        List<SplitLeaf> leaves = new ArrayList<>(memberTypes.size());
        for (int i = 0; i < memberTypes.size(); i++) {
            String memberType = memberTypes.get(i) != null ? memberTypes.get(i) : "vec4";
            for (SplitLeaf nested : splitVarying(memberType, base + "_m" + i, 1)) {
                leaves.add(nested);
            }
        }
        return List.copyOf(leaves);
    }

    /**
     * Assigns dense locations over split leaves in declaration order, advancing by each
     * leaf's slot count (the rank==location workaround: a matrix leaf run never overlaps
     * the next varying).
     *
     * @param leaves leaves in declaration order; may be {@code null}/empty
     * @return leaf name to location, in encounter order; empty for {@code null}/empty input
     */
    public static Map<String, Integer> assignDenseLocations(List<SplitLeaf> leaves) {
        Map<String, Integer> locations = new LinkedHashMap<>();
        if (leaves == null || leaves.isEmpty()) {
            return locations;
        }
        int next = 0;
        for (SplitLeaf leaf : leaves) {
            if (leaf == null || locations.containsKey(leaf.name())) {
                continue;
            }
            locations.put(leaf.name(), next);
            next += Math.max(1, leaf.slots());
        }
        return locations;
    }

    private static boolean isMatrixType(String type) {
        return type.startsWith("mat") || type.startsWith("dmat");
    }

    /** Row count of a matrix type name ({@code mat4x3} has 3 rows; square forms have N). */
    private static int matrixRows(String type) {
        String core = type.startsWith("dmat") ? type.substring(4) : type.substring(3);
        int by = core.indexOf('x');
        try {
            if (by >= 0) {
                return Math.max(2, Math.min(4, Integer.parseInt(core.substring(by + 1))));
            }
            return Math.max(2, Math.min(4, Integer.parseInt(core)));
        } catch (NumberFormatException notMatrix) {
            return 4;
        }
    }

    /**
     * Union helper used by {@link ProgramTranslator}: merges per-stage varying slot maps,
     * keeping the widest declaration per name and reporting names seen on only one side.
     *
     * @param vertexOuts   vertex-stage {@code out} name to slots, in declaration order
     * @param fragmentIns  fragment-stage {@code in} name to slots, in declaration order
     * @return merged name to slots in first-seen order
     */
    public static Map<String, Integer> unionVaryingSlots(
            Map<String, Integer> vertexOuts, Map<String, Integer> fragmentIns) {
        Map<String, Integer> union = new LinkedHashMap<>();
        if (vertexOuts != null) {
            union.putAll(vertexOuts);
        }
        if (fragmentIns != null) {
            for (Map.Entry<String, Integer> entry : fragmentIns.entrySet()) {
                union.merge(entry.getKey(), entry.getValue(), Math::max);
            }
        }
        return union;
    }

    /**
     * Names declared on exactly one side of a vertex/fragment varying interface.
     *
     * @return names in union order that are missing a writer (vertex {@code out}) or a
     *         reader (fragment {@code in}); empty when the interface agrees
     */
    public static Set<String> oneSidedVaryings(
            Map<String, Integer> vertexOuts, Map<String, Integer> fragmentIns) {
        Set<String> oneSided = new LinkedHashSet<>();
        Map<String, Integer> outs = vertexOuts != null ? vertexOuts : Map.of();
        Map<String, Integer> ins = fragmentIns != null ? fragmentIns : Map.of();
        for (String name : unionVaryingSlots(outs, ins).keySet()) {
            if (!outs.containsKey(name) || !ins.containsKey(name)) {
                oneSided.add(name);
            }
        }
        return Set.copyOf(oneSided);
    }
}
