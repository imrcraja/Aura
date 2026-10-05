package com.aura.client.renderer.chunk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChunkMeshDataTest {
    @Test
    void positionOnlyMeshUsesCanonicalStrideAndDefaults() {
        ChunkMeshData mesh = new ChunkMeshData(new float[]{0,1,2}, new int[]{0,0,0});
        assertEquals(ChunkMeshData.FLOAT_STRIDE, mesh.vertexStrideFloats());
        assertEquals(1, mesh.vertexCount());
        assertEquals(3, mesh.indexCount());
        assertEquals(1.0f, mesh.vertices()[3]);
        assertEquals(1.0f, mesh.vertices()[6]);
    }

    @Test
    void interleavedMeshRejectsWrongStride() {
        assertThrows(IllegalArgumentException.class, () ->
                new ChunkMeshData(new float[13], new int[]{0,1,2}));
    }
}
