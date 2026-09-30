package com.vulkairis.bridge;

import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Manages standard Iris / Minecraft uniforms: camera matrices, projection,
 * frame timing, and resolution.
 */
public class UniformBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vulkairis/UniformBridge");

    private final Matrix4f modelViewMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();
    private float frameTime = 0.016f;
    private int screenWidth = 1920;
    private int screenHeight = 1080;
    private long worldTime = 0L;

    private static final UniformBridge INSTANCE = new UniformBridge();

    public static UniformBridge getInstance() {
        return INSTANCE;
    }

    public void updateMatrices(Matrix4f modelView, Matrix4f projection) {
        if (modelView != null) this.modelViewMatrix.set(modelView);
        if (projection != null) this.projectionMatrix.set(projection);
    }

    public void updateResolution(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public void updateTime(float frameTimeDelta, long time) {
        this.frameTime = frameTimeDelta;
        this.worldTime = time;
    }

    /**
     * Writes standard camera and projection matrices into direct ByteBuffer for uniform buffer upload.
     */
    public void writeStandardUniforms(ByteBuffer target) {
        if (target == null || target.remaining() < 128) return;
        target.order(ByteOrder.LITTLE_ENDIAN);

        modelViewMatrix.get(target);
        projectionMatrix.get(target);
    }

    public Matrix4f getModelViewMatrix() {
        return modelViewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public float getFrameTime() {
        return frameTime;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }
}
