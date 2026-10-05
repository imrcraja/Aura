package com.aura.client.renderer.chunk;

import java.util.Arrays;

/** Version-neutral boundary for adapters that decode Minecraft chunk vertices. */
public final class ChunkMeshExtractor {
    private ChunkMeshExtractor() {}

    public static ChunkMeshData positions(float[] positions, int[] indices) {
        if (positions == null || indices == null) throw new IllegalArgumentException("mesh arrays must be non-null");
        if ((positions.length % 3) != 0) throw new IllegalArgumentException("positions must contain XYZ triples");
        return new ChunkMeshData(Arrays.copyOf(positions, positions.length), Arrays.copyOf(indices, indices.length));
    }
}
