package com.vulkairis.translation;

import java.util.*;

/**
 * Encapsulates reflected metadata from a compiled SPIR-V or GLSL shader.
 * Tracks uniform variables, texture sampler bindings, storage buffers, and vertex attributes.
 */
public class ShaderReflection {
    public record UniformInfo(String name, String type, int offset, int size) {}
    public record SamplerBinding(String name, int binding, int textureUnit) {}
    public record VertexAttribute(String name, int location, String type, int size) {}

    private final Map<String, UniformInfo> uniforms = new HashMap<>();
    private final Map<String, SamplerBinding> samplers = new HashMap<>();
    private final List<VertexAttribute> vertexAttributes = new ArrayList<>();
    private int uboBlockSize = 0;
    private int pushConstantSize = 0;

    public ShaderReflection() {}

    public void addUniform(String name, String type, int offset, int size) {
        uniforms.put(name, new UniformInfo(name, type, offset, size));
    }

    public void addSampler(String name, int binding, int textureUnit) {
        samplers.put(name, new SamplerBinding(name, binding, textureUnit));
    }

    public void addVertexAttribute(String name, int location, String type, int size) {
        vertexAttributes.add(new VertexAttribute(name, location, type, size));
    }

    public void setUboBlockSize(int size) {
        this.uboBlockSize = size;
    }

    public int getUboBlockSize() {
        return uboBlockSize;
    }

    public void setPushConstantSize(int size) {
        this.pushConstantSize = size;
    }

    public int getPushConstantSize() {
        return pushConstantSize;
    }

    public Map<String, UniformInfo> getUniforms() {
        return Collections.unmodifiableMap(uniforms);
    }

    public Map<String, SamplerBinding> getSamplers() {
        return Collections.unmodifiableMap(samplers);
    }

    public List<VertexAttribute> getVertexAttributes() {
        return Collections.unmodifiableList(vertexAttributes);
    }

    public boolean hasUniform(String name) {
        return uniforms.containsKey(name);
    }

    public UniformInfo getUniform(String name) {
        return uniforms.get(name);
    }
}
