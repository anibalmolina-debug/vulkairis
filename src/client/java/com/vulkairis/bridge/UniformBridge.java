package com.vulkairis.bridge;

import com.vulkairis.core.RenderMatrices;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Manages standard Iris / Minecraft uniforms: camera matrices, projection,
 * frame timing, and resolution.
 *
 * <p>Matrix ownership is delegated to {@link RenderMatrices}, the single source of truth for
 * camera matrix conventions. The incoming {@code modelView} is an already-combined view-model
 * matrix; because this bridge never receives a separate view, it is stored as the model with the
 * default identity view, which makes {@code RenderMatrices.getModelView()} return exactly the
 * supplied matrix while keeping all derived matrices (normal, inverse, MVP) consistent.</p>
 */
public class UniformBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vulkairis/UniformBridge");

    /** std140 size of the two back-to-back mat4s written by {@link #writeStandardUniforms}. */
    private static final int STANDARD_UNIFORM_BYTES = 128;

    private final RenderMatrices matrices = new RenderMatrices();
    private float frameTime = 0.016f;
    private int screenWidth = 1920;
    private int screenHeight = 1080;
    private long worldTime = 0L;
    /** Phase 10: world-space camera position (source of truth: Iris render state, per frame). */
    private final org.joml.Vector3f cameraPosition = new org.joml.Vector3f();
    /** Phase 10: monotonic frame counter (source of truth: controller frame count, per frame). */
    private long frameCounter = 0L;

    private static final UniformBridge INSTANCE = new UniformBridge();

    public static UniformBridge getInstance() {
        return INSTANCE;
    }

    public void updateMatrices(Matrix4f modelView, Matrix4f projection) {
        if (modelView != null) matrices.setModel(modelView);
        if (projection != null) {
            // Phase 9: Iris supplies OpenGL-convention matrices (clip depth [-1, 1]). The stored
            // projection is premultiplied once with the authoritative depth-range correction so
            // every downstream consumer (MVP, inverses, uploads) works in Vulkan [0, 1] depth.
            // This is the single application point: Y is flipped exactly once at compile time
            // (shaderc invert_y) and depth exactly once here — never in both places.
            matrices.setProjection(new Matrix4f(RenderMatrices.CLIP_SPACE_CORRECTION).mul(projection));
        }
    }

    public void updateResolution(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    public void updateTime(float frameTimeDelta, long time) {
        this.frameTime = frameTimeDelta;
        this.worldTime = time;
    }

    public void updateCamera(float x, float y, float z, long frame) {
        this.cameraPosition.set(x, y, z);
        this.frameCounter = frame;
    }

    public org.joml.Vector3f getCameraPosition() {
        return cameraPosition;
    }

    public long getFrameCounter() {
        return frameCounter;
    }

    /**
     * Writes std140-aligned standard camera uniforms into a direct ByteBuffer for upload:
     * {@code modelView} (64 bytes) immediately followed by {@code projection} (64 bytes), for a
     * total of {@value #STANDARD_UNIFORM_BYTES} bytes.
     *
     * <p>If {@code target} does not have room for both matrices the write is skipped entirely, so
     * this method never writes past {@link ByteBuffer#limit()}.</p>
     */
    public void writeStandardUniforms(ByteBuffer target) {
        if (target == null || target.remaining() < STANDARD_UNIFORM_BYTES) return;
        target.order(ByteOrder.LITTLE_ENDIAN);

        // JOML writes matrix floats at absolute positions, so advance explicitly: callers
        // observe standard relative-put semantics (128 bytes consumed).
        int base = target.position();
        matrices.getModelView().get(base, target);
        matrices.getProjection().get(base + 64, target);
        target.position(base + STANDARD_UNIFORM_BYTES);
    }

    public Matrix4f getModelViewMatrix() {
        return matrices.getModelView();
    }

    public Matrix4f getProjectionMatrix() {
        return matrices.getProjection();
    }

    public Matrix4f getModelViewProjection() {
        return matrices.getModelViewProjection();
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
