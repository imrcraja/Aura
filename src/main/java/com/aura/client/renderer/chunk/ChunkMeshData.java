package com.aura.client.renderer.chunk;

import java.util.Arrays;

/** Immutable CPU-side chunk mesh payload ready for backend upload. */
public final class ChunkMeshData {
    private final float[] vertices;
    private final int[] indices;

    public ChunkMeshData(float[] vertices, int[] indices) {
        this.vertices = vertices.clone();
        this.indices = indices.clone();
    }

    public int vertexCount() { return vertices.length / 3; }
    public int indexCount() { return indices.length; }
    public float[] vertices() { return Arrays.copyOf(vertices, vertices.length); }
    public int[] indices() { return Arrays.copyOf(indices, indices.length); }
    public long estimatedBytes() { return (long) vertices.length * Float.BYTES + (long) indices.length * Integer.BYTES; }
}
