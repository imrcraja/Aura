package com.aura.client.renderer.chunk;

import java.util.Arrays;

public final class ChunkMeshData {
    public static final int FLOAT_STRIDE = 14;
    private final float[] vertices;
    private final int[] indices;

    public ChunkMeshData(float[] vertices, int[] indices) {
        if (vertices == null || indices == null) throw new IllegalArgumentException("mesh arrays must be non-null");
        if ((vertices.length % 3) != 0) throw new IllegalArgumentException("position-only vertices must contain XYZ triples");
        int count = vertices.length / 3;
        this.vertices = new float[count * FLOAT_STRIDE];
        for (int i = 0; i < count; i++) {
            int s = i * 3, t = i * FLOAT_STRIDE;
            this.vertices[t] = vertices[s];
            this.vertices[t + 1] = vertices[s + 1];
            this.vertices[t + 2] = vertices[s + 2];
            this.vertices[t + 3] = this.vertices[t + 4] = this.vertices[t + 5] = this.vertices[t + 6] = 1.0f;
            this.vertices[t + 13] = 1.0f;
        }
        this.indices = indices.clone();
    }

    public ChunkMeshData(float[] interleavedVertices, int[] indices, boolean interleaved) {
        if (!interleaved) throw new IllegalArgumentException("Use the position-only constructor for non-interleaved data");
        if (interleavedVertices == null || indices == null) throw new IllegalArgumentException("mesh arrays must be non-null");
        if (interleavedVertices.length % FLOAT_STRIDE != 0) throw new IllegalArgumentException("invalid canonical stride");
        this.vertices = interleavedVertices.clone();
        this.indices = indices.clone();
    }

    public int vertexCount() { return vertices.length / FLOAT_STRIDE; }
    public int vertexStrideFloats() { return FLOAT_STRIDE; }
    public int indexCount() { return indices.length; }
    public float[] vertices() { return Arrays.copyOf(vertices, vertices.length); }
    public int[] indices() { return Arrays.copyOf(indices, indices.length); }
    public long estimatedBytes() { return (long) vertices.length * Float.BYTES + (long) indices.length * Integer.BYTES; }
}
