package com.vulkairis.test;

import com.vulkairis.translation.ProgramTranslator;
import com.vulkairis.translation.SpirvCompiler;
import com.vulkairis.translation.ShaderCompileResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.shaderc.Shaderc;

import java.util.List;
import java.util.Map;

/**
 * Phase B: golden program-together translation fixtures plus the location-collision
 * regression test.
 *
 * <p>Four vertex/fragment pairs (terrain, entity, cloud, sky) cover every legacy mapping:
 * {@code texture2D*}/{@code texture2DLod}&#8594;{@code texture*}/{@code textureLod},
 * {@code shadow2D*} wrapping, {@code gl_FragData[n]}/{@code gl_FragColor} indexed outputs,
 * {@code gl_TextureMatrix}/{@code ftransform}/{@code gl_Color}/{@code gl_Normal} mappings and
 * depth/volume conversion. Each translated stage must carry no {@code layout(binding =)},
 * no {@code layout(location =)} except fragment outputs, and must compile to SPIR-V — the
 * fix5 legacy errors go to zero before anything touches the driver.</p>
 */
public class ProgramTranslatorTest {

    // ------------------------------------------------------------------
    // Golden fixtures: legacy Iris-style sources per program.
    // ------------------------------------------------------------------

    static String terrainVertex() {
        return """
                #version 120
                attribute vec3 Position;
                attribute vec2 UV0;
                uniform mat4 projection;
                uniform vec3 sunPosition;
                uniform sampler2D occlusionTex;
                varying vec2 texCoord;
                void main() {
                    texCoord = UV0;
                    vec4 uvTransform = gl_TextureMatrix[1] * vec4(UV0, 0.0, 1.0);
                    float occluded = texture2DLod(occlusionTex, uvTransform.xy, 0.0).r;
                    vec4 worldPos = vec4(Position + sunPosition * occluded, 1.0);
                    gl_Position = projection * worldPos + ftransform() * 0.0;
                }
                """;
    }

    static String terrainFragment() {
        return """
                #version 120
                uniform sampler2D gtexture;
                uniform sampler2DShadow shadowtex0;
                uniform float rainStrength = 1.0;
                varying vec2 texCoord;
                void main() {
                    vec4 albedo = texture2D(gtexture, texCoord);
                    float shadow = shadow2D(shadowtex0, vec3(texCoord, 0.5));
                    gl_FragData[0] = albedo * shadow * rainStrength;
                }
                """;
    }

    static String entityVertex() {
        return """
                #version 120
                attribute vec3 Position;
                attribute vec2 UV0;
                uniform sampler2D gtexture;
                varying vec2 texCoord;
                varying vec3 normalOut;
                varying vec4 colorOut;
                void main() {
                    texCoord = UV0;
                    normalOut = gl_Normal;
                    colorOut = gl_Color;
                    vec4 projected = texture2DProj(gtexture, vec4(UV0, 0.0, 1.0));
                    gl_Position = gl_ModelViewProjectionMatrix * vec4(Position + projected.xyz * 0.01, 1.0);
                }
                """;
    }

    static String entityFragment() {
        return """
                #version 120
                uniform sampler2D gtexture;
                uniform sampler2D lightmap;
                varying vec2 texCoord;
                varying vec3 normalOut;
                varying vec4 colorOut;
                void main() {
                    vec4 albedo = texture2DLod(gtexture, texCoord, 2.0);
                    vec4 light = texture(lightmap, normalOut.xy);
                    gl_FragData[0] = albedo * colorOut;
                    gl_FragData[1] = light;
                }
                """;
    }

    static String cloudVertex() {
        return """
                #version 120
                varying vec2 texCoord;
                void main() {
                    texCoord = gl_MultiTexCoord0.xy;
                    gl_Position = ftransform();
                }
                """;
    }

    static String cloudFragment() {
        return """
                #version 120
                uniform sampler3D cloudVolume;
                varying vec2 texCoord;
                void main() {
                    vec4 density = texture3D(cloudVolume, vec3(texCoord, 0.5));
                    vec4 levelled = texture3DLod(cloudVolume, vec3(texCoord, 0.25), 1.0);
                    gl_FragColor = density + levelled;
                }
                """;
    }

    static String skyVertex() {
        return """
                #version 120
                attribute vec3 Position;
                varying vec3 skyDir;
                void main() {
                    skyDir = (gl_TextureMatrix[0] * vec4(Position, 1.0)).xyz;
                    gl_Position = vec4(Position.xy, 0.0, 1.0);
                }
                """;
    }

    static String skyFragment() {
        return """
                #version 120
                uniform sampler2DShadow shadowtex1;
                uniform samplerCube skybox;
                uniform sampler2D gtexture;
                uniform bool useDetail = true;
                varying vec3 skyDir;
                void main() {
                    float shadow = shadow2DProj(shadowtex1, vec4(skyDir.xy, 0.5, 1.0));
                    vec4 sky = textureCubeLod(skybox, skyDir, 1.0);
                    vec4 detail = texture2DBias(gtexture, skyDir.xy, -1.0);
                    vec4 shaded = sky * shadow;
                    if (useDetail != 0) {
                        shaded += detail * 0.01;
                    }
                    gl_FragData[0] = shaded;
                }
                """;
    }

    // ------------------------------------------------------------------
    // Golden assertions shared by every pair.
    // ------------------------------------------------------------------

    private static ProgramTranslator.TranslatedProgram translatePair(
            String program, String vertex, String fragment) {
        ProgramTranslator.clearCacheForTests();
        return ProgramTranslator.translate(program, List.of(
                new ProgramTranslator.StageUnit("VERTEX", vertex, program + ".vsh"),
                new ProgramTranslator.StageUnit("FRAGMENT", fragment, program + ".fsh")),
                "golden-pack");
    }

    private static void assertFix5ErrorsGone(String translated) {
        // "texture2D" covers texture2DProj/Bias/Lod/Rect as substrings; "texture(" itself
        // must survive, so the bare call form is asserted with its parenthesis.
        for (String legacy : List.of("texture2D", "texture3D", "textureCube", "shadow2D",
                "gl_FragData", "gl_FragColor", "gl_TextureMatrix", "ftransform(",
                "gl_Color", "gl_Normal", "gl_MultiTexCoord", "gl_ModelViewProjectionMatrix",
                "gl_Vertex")) {
            Assertions.assertFalse(translated.contains(legacy),
                    "Legacy builtin must not survive translation: " + legacy + "\n" + translated);
        }
    }

    private static void assertBindingPolicy(
            String vertex, String fragment, int expectedFragmentOutputs) {
        Assertions.assertFalse(vertex.contains("layout(binding"), vertex);
        Assertions.assertFalse(fragment.contains("layout(binding"), fragment);
        Assertions.assertFalse(vertex.contains("layout(location"), vertex);

        int fragmentLocations = countOccurrences(fragment, "layout(location = ");
        Assertions.assertEquals(expectedFragmentOutputs, fragmentLocations,
                "Only fragment outputs may carry locations, one each:\n" + fragment);
    }

    private static void assertCompiles(String name, String glsl, int shadercKind) {
        ShaderCompileResult result =
                SpirvCompiler.compileGlslToSpirvDetailed(name, glsl, shadercKind);
        Assertions.assertTrue(result.succeeded(),
                "Translated stage must compile: " + name + ": " + result.compilerLog());
        Assertions.assertNotNull(result.spirv());
        Assertions.assertTrue(result.spirv().remaining() > 0);
    }

    // ------------------------------------------------------------------
    // The four golden pairs.
    // ------------------------------------------------------------------

    @Test
    public void testTerrainPairTranslatesAndCompiles() {
        ProgramTranslator.TranslatedProgram program =
                translatePair("gbuffers_terrain", terrainVertex(), terrainFragment());

        String vert = program.stages().get("VERTEX").glsl();
        String frag = program.stages().get("FRAGMENT").glsl();

        assertFix5ErrorsGone(vert);
        assertFix5ErrorsGone(frag);
        Assertions.assertTrue(vert.contains("textureLod(occlusionTex, uvTransform.xy, 0.0)"), vert);
        Assertions.assertTrue(vert.contains("vulkairis_TextureMatrix[1]"), vert);
        Assertions.assertTrue(frag.contains("texture(gtexture, texCoord)"), frag);
        Assertions.assertTrue(frag.contains("texture(shadowtex0, vec3(texCoord, 0.5))"), frag);
        Assertions.assertTrue(frag.contains("vulkairisFragData0"), frag);

        // One union block shared by both stages: projection/sunPosition from the vertex side,
        // rainStrength from the fragment side, plus the legacy matrix uniforms.
        for (String member : List.of("mat4 projection;", "vec3 sunPosition;",
                "float rainStrength;", "mat4 vulkairis_TextureMatrix[8];",
                "mat4 vulkairis_ModelViewProjectionMatrix;")) {
            Assertions.assertTrue(vert.contains(member), "Vertex misses union member " + member);
            Assertions.assertTrue(frag.contains(member), "Fragment misses union member " + member);
        }
        Assertions.assertTrue(program.unionVaryings().get("texCoord").equals("both"), program.toString());

        assertBindingPolicy(vert, frag, 1);
        Assertions.assertEquals(Map.of("vulkairisFragData0", 0), program.fragmentOutputLocations());

        assertCompiles("gbuffers_terrain.vsh", vert, Shaderc.shaderc_glsl_vertex_shader);
        assertCompiles("gbuffers_terrain.fsh", frag, Shaderc.shaderc_glsl_fragment_shader);
    }

    @Test
    public void testEntityPairTranslatesAndCompiles() {
        ProgramTranslator.TranslatedProgram program =
                translatePair("gbuffers_entity", entityVertex(), entityFragment());

        String vert = program.stages().get("VERTEX").glsl();
        String frag = program.stages().get("FRAGMENT").glsl();

        assertFix5ErrorsGone(vert);
        assertFix5ErrorsGone(frag);
        Assertions.assertTrue(vert.contains("vulkairis_Normal"), vert);
        Assertions.assertTrue(vert.contains("vulkairis_Color"), vert);
        Assertions.assertTrue(vert.contains("textureProj(gtexture, vec4(UV0, 0.0, 1.0))"), vert);
        Assertions.assertTrue(vert.contains("vulkairis_ModelViewProjectionMatrix"), vert);
        Assertions.assertTrue(frag.contains("textureLod(gtexture, texCoord, 2.0)"), frag);

        // Two indexed outputs: gl_FragData[0] and gl_FragData[1] keep their indices.
        Assertions.assertEquals(Map.of("vulkairisFragData0", 0, "vulkairisFragData1", 1),
                program.fragmentOutputLocations());
        assertBindingPolicy(vert, frag, 2);

        assertCompiles("gbuffers_entity.vsh", vert, Shaderc.shaderc_glsl_vertex_shader);
        assertCompiles("gbuffers_entity.fsh", frag, Shaderc.shaderc_glsl_fragment_shader);
    }

    @Test
    public void testCloudPairTranslatesAndCompiles() {
        ProgramTranslator.TranslatedProgram program =
                translatePair("gbuffers_cloud", cloudVertex(), cloudFragment());

        String vert = program.stages().get("VERTEX").glsl();
        String frag = program.stages().get("FRAGMENT").glsl();

        assertFix5ErrorsGone(vert);
        assertFix5ErrorsGone(frag);
        Assertions.assertTrue(vert.contains("vulkairis_MultiTexCoord0"), vert);
        Assertions.assertTrue(vert.contains("(vulkairis_ModelViewProjectionMatrix * vulkairis_Vertex)"),
                vert);
        Assertions.assertTrue(vert.contains("in vec4 vulkairis_Vertex;"), vert);
        Assertions.assertTrue(frag.contains("texture(cloudVolume, vec3(texCoord, 0.5))"), frag);
        Assertions.assertTrue(frag.contains("textureLod(cloudVolume, vec3(texCoord, 0.25), 1.0)"),
                frag);

        assertBindingPolicy(vert, frag, 1);

        assertCompiles("gbuffers_cloud.vsh", vert, Shaderc.shaderc_glsl_vertex_shader);
        assertCompiles("gbuffers_cloud.fsh", frag, Shaderc.shaderc_glsl_fragment_shader);
    }

    @Test
    public void testSkyPairTranslatesAndCompiles() {
        ProgramTranslator.TranslatedProgram program =
                translatePair("gbuffers_sky", skyVertex(), skyFragment());

        String vert = program.stages().get("VERTEX").glsl();
        String frag = program.stages().get("FRAGMENT").glsl();

        assertFix5ErrorsGone(vert);
        assertFix5ErrorsGone(frag);
        Assertions.assertTrue(vert.contains("vulkairis_TextureMatrix[0]"), vert);
        Assertions.assertTrue(frag.contains("textureProj(shadowtex1, vec4(skyDir.xy, 0.5, 1.0))"),
                frag);
        Assertions.assertTrue(frag.contains("textureLod(skybox, skyDir, 1.0)"), frag);
        Assertions.assertTrue(frag.contains("texture(gtexture, skyDir.xy, -1.0)"), frag);
        // The defaulted bool uniform joins the union block as int (VulkanMod-bound rule).
        // Scope the check to the block body: the stubbed original survives as a comment.
        int blockStart = frag.indexOf("VulkairisUniformBlock");
        Assertions.assertTrue(blockStart >= 0, frag);
        String members = frag.substring(frag.indexOf('{', blockStart), frag.indexOf('}', blockStart));
        Assertions.assertTrue(members.contains("int useDetail;"), members);
        Assertions.assertFalse(members.contains("bool"), members);

        assertBindingPolicy(vert, frag, 1);

        assertCompiles("gbuffers_sky.vsh", vert, Shaderc.shaderc_glsl_vertex_shader);
        assertCompiles("gbuffers_sky.fsh", frag, Shaderc.shaderc_glsl_fragment_shader);
    }

    // ------------------------------------------------------------------
    // Location-collision regression: overlapping user locations collapse.
    // ------------------------------------------------------------------

    @Test
    public void testOverlappingLocationsDoNotCollideAfterTranslation() {
        String vertex = """
                #version 120
                layout(location = 0) in vec3 first;
                layout(location = 0) in vec3 second;
                out vec3 bridge;
                void main() {
                    bridge = first + second;
                    gl_Position = vec4(bridge, 1.0);
                }
                """;
        String fragment = """
                #version 120
                layout(location = 0) in vec3 bridge;
                layout(location = 0) out vec4 targetA;
                layout(location = 1) out vec4 targetB;
                void main() {
                    targetA = vec4(bridge, 1.0);
                    targetB = vec4(bridge, 1.0);
                }
                """;

        ProgramTranslator.clearCacheForTests();
        ProgramTranslator.TranslatedProgram program = ProgramTranslator.translate(
                "collision", List.of(
                        new ProgramTranslator.StageUnit("VERTEX", vertex, "collision.vsh"),
                        new ProgramTranslator.StageUnit("FRAGMENT", fragment, "collision.fsh")),
                "golden-pack");

        String vert = program.stages().get("VERTEX").glsl();
        String frag = program.stages().get("FRAGMENT").glsl();

        // The vertex stage keeps no locations at all: nothing can collide there.
        Assertions.assertFalse(vert.contains("layout(location"), vert);
        Assertions.assertTrue(vert.contains("in vec3 first;"), vert);
        Assertions.assertTrue(vert.contains("in vec3 second;"), vert);

        // Fragment outputs are renumbered densely from the stripped sources.
        Assertions.assertEquals(Map.of("targetA", 0, "targetB", 1),
                program.fragmentOutputLocations());
        Assertions.assertEquals(1, countOccurrences(frag, "layout(location = 0)"), frag);
        Assertions.assertEquals(1, countOccurrences(frag, "layout(location = 1)"), frag);
        Assertions.assertTrue(frag.contains("layout(location = 0) out vec4 targetA;"), frag);
        Assertions.assertTrue(frag.contains("layout(location = 1) out vec4 targetB;"), frag);
        Assertions.assertFalse(frag.contains("layout(binding"), frag);

        assertCompiles("collision.vsh", vert, Shaderc.shaderc_glsl_vertex_shader);
        assertCompiles("collision.fsh", frag, Shaderc.shaderc_glsl_fragment_shader);
    }

    // ------------------------------------------------------------------
    // Generation gating: a pack/dimension change invalidates the cache.
    // ------------------------------------------------------------------

    @Test
    public void testGenerationChangeInvalidatesProgramCache() {
        ProgramTranslator.clearCacheForTests();
        List<ProgramTranslator.StageUnit> units = List.of(
                new ProgramTranslator.StageUnit("VERTEX", terrainVertex(), "gated.vsh"),
                new ProgramTranslator.StageUnit("FRAGMENT", terrainFragment(), "gated.fsh"));

        ProgramTranslator.TranslatedProgram first =
                ProgramTranslator.translate("gated", units, "pack", 7001);
        ProgramTranslator.TranslatedProgram cached =
                ProgramTranslator.translate("gated", units, "pack", 7001);

        Assertions.assertSame(first, cached, "Same generation must hit the program cache");
        Assertions.assertEquals(7001, cached.generation());

        ProgramTranslator.TranslatedProgram next =
                ProgramTranslator.translate("gated", units, "pack", 7002);

        Assertions.assertNotSame(first, next, "A new generation must never reuse the old entry");
        Assertions.assertEquals(7002, next.generation());
        Assertions.assertEquals(first.translationKey(), next.translationKey(),
                "Only the generation differs; the content key is stable");
    }

    @Test
    public void testStripHelpersAndNullSafety() {
        Assertions.assertNull(ProgramTranslator.stripBindingQualifiers(null));
        Assertions.assertNull(ProgramTranslator.stripAllLocationQualifiers(null));
        Assertions.assertEquals("", ProgramTranslator.stripBindingQualifiers(""));
        Assertions.assertEquals("", ProgramTranslator.stripAllLocationQualifiers(""));

        String bound = "layout(binding = 100) uniform sampler2D tex;\n";
        String stripped = ProgramTranslator.stripBindingQualifiers(bound);
        Assertions.assertFalse(stripped.contains("binding"), stripped);
        Assertions.assertTrue(stripped.contains("uniform sampler2D tex;"), stripped);

        // std140 survives while the binding goes: the block stays valid for reflection.
        String block = "layout(std140, binding = 100) uniform VulkairisUniformBlock {\n    mat4 m;\n};\n";
        String blockStripped = ProgramTranslator.stripBindingQualifiers(block);
        Assertions.assertTrue(blockStripped.contains("layout(std140) uniform VulkairisUniformBlock"),
                blockStripped);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int index = 0;
        while ((index = haystack.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
