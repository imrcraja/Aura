package com.aura.client.renderer.chunk;

import java.util.ArrayList;
import java.util.List;

/** Version-neutral CPU mesh builder used by Minecraft version adapters. */
public final class ChunkMeshBuilder {
    private final List<Float> vertices = new ArrayList<>();
    private final List<Integer> indices = new ArrayList<>();

    public int addVertex(float x, float y, float z) {
        int index = vertices.size() / 3;
        vertices.add(x); vertices.add(y); vertices.add(z);
        return index;
    }

    public void addTriangle(int a, int b, int c) {
        indices.add(a); indices.add(b); indices.add(c);
    }

    public boolean isEmpty() { return indices.isEmpty(); }

    public ChunkMeshData build() {
        float[] v = new float[vertices.size()];
        int[] i = new int[indices.size()];
        for (int n = 0; n < v.length; n++) v[n] = vertices.get(n);
        for (int n = 0; n < i.length; n++) i[n] = indices.get(n);
        return new ChunkMeshData(v, i);
    }

    public void clear() { vertices.clear(); indices.clear(); }
}
