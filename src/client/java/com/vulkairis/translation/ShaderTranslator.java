package com.vulkairis.translation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Preprocesses and adapts Iris / Minecraft GLSL shader sources into Vulkan-compatible GLSL
 * before handing them to the in-process SPIR-V compiler.
 */
public class ShaderTranslator {
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\s*#version\\s+(\\d+)(?:\\s+(core|compatibility))?", Pattern.MULTILINE);
    private static final Pattern NON_OPAQUE_UNIFORM_PATTERN = Pattern.compile(
            "^\\s*uniform\\s+(bool|int|uint|float|double|bvec[234]|ivec[234]|uvec[234]|vec[234]|dvec[234]|mat[234](?:x[234])?)\\s+([^;]+);",
            Pattern.MULTILINE
    );

    /**
     * Prepares shader GLSL source for Vulkan compilation:
     * - Ensures valid #version directive (upgrades legacy versions to 450 core)
     * - Injects GL_ARB_separate_shader_objects and GL_ARB_shading_language_420pack
     * - Adapts legacy attribute/varying keywords to in/out
     * - Wraps non-opaque uniforms into uniform block for Vulkan compliance
     */
    public static String preprocessGlsl(String rawSource, boolean isVertex) {
        if (rawSource == null || rawSource.isBlank()) {
            return rawSource;
        }

        StringBuilder processed = new StringBuilder();
        Matcher matcher = VERSION_PATTERN.matcher(rawSource);

        if (matcher.find()) {
            matcher.appendReplacement(processed, "#version 450 core");
            matcher.appendTail(processed);
        } else {
            processed.append("#version 450 core\n").append(rawSource);
        }

        String result = processed.toString();

        // Convert legacy keywords attribute/varying
        if (isVertex) {
            result = result.replaceAll("(?m)^\\s*attribute\\s+", "in ");
            result = result.replaceAll("(?m)^\\s*varying\\s+", "out ");
        } else {
            result = result.replaceAll("(?m)^\\s*varying\\s+", "in ");
        }

        // Collect non-opaque uniforms
        Matcher uniformMatcher = NON_OPAQUE_UNIFORM_PATTERN.matcher(result);
        StringBuilder uniformBlockContent = new StringBuilder();
        StringBuilder noUniforms = new StringBuilder();

        while (uniformMatcher.find()) {
            String type = uniformMatcher.group(1);
            String name = uniformMatcher.group(2).trim();
            uniformBlockContent.append("    ").append(type).append(" ").append(name).append(";\n");
            uniformMatcher.appendReplacement(noUniforms, "// uniform " + type + " " + name + ";");
        }
        uniformMatcher.appendTail(noUniforms);
        result = noUniforms.toString();

        // Inject extensions and precision
        int versionIdx = result.indexOf("#version 450 core");
        if (versionIdx >= 0) {
            int lineEnd = result.indexOf('\n', versionIdx);
            StringBuilder header = new StringBuilder();
            header.append("\n#extension GL_ARB_separate_shader_objects : enable\n");
            header.append("#extension GL_ARB_shading_language_420pack : enable\n");
            header.append("precision highp float;\n");
            header.append("precision highp int;\n");

            if (uniformBlockContent.length() > 0) {
                header.append("layout(std140, binding = 0) uniform VulkairisUniformBlock {\n")
                      .append(uniformBlockContent)
                      .append("};\n");
            }

            result = result.substring(0, lineEnd + 1) + header + result.substring(lineEnd + 1);
        }

        return result;
    }
}
