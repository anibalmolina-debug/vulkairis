package com.vulkairis.translation;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Validates generated SPIR-V bytecode headers and structure.
 */
public class SpirvValidator {
    public static final int SPIRV_MAGIC = 0x07230203;

    public static void validate(ByteBuffer spirv) {
        if (spirv == null) {
            throw new IllegalArgumentException("SPIR-V bytecode buffer is null");
        }

        if (spirv.remaining() < 20) {
            throw new IllegalArgumentException("SPIR-V buffer too small for header (size: " + spirv.remaining() + " bytes)");
        }

        if (spirv.remaining() % 4 != 0) {
            throw new IllegalArgumentException("SPIR-V buffer size must be a multiple of 4 bytes (size: " + spirv.remaining() + ")");
        }

        ByteBuffer slice = spirv.slice().order(ByteOrder.LITTLE_ENDIAN);
        int magic = slice.getInt(0);
        if (magic != SPIRV_MAGIC) {
            // Check big-endian
            slice.order(ByteOrder.BIG_ENDIAN);
            magic = slice.getInt(0);
            if (magic != SPIRV_MAGIC) {
                throw new IllegalStateException(String.format("Invalid SPIR-V magic number: 0x%08X (expected 0x%08X)",
                        magic, SPIRV_MAGIC));
            }
        }

        int version = slice.getInt(4);
        int generator = slice.getInt(8);
        int bound = slice.getInt(12);

        if (bound <= 0) {
            throw new IllegalStateException("Invalid SPIR-V ID bound: " + bound);
        }
    }
}
