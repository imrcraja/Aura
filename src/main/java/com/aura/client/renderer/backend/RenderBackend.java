package com.aura.client.renderer.backend;

import com.aura.client.renderer.chunk.ChunkMeshData;

public interface RenderBackend {
    String id();
    void initialize();
    void shutdown();
    boolean isAvailable();

    default RenderMesh createMesh(String label, int vertexCount, int indexCount) {
        throw new UnsupportedOperationException(id() + " does not implement mesh creation yet");
    }

    default RenderMesh uploadChunkMesh(String label, ChunkMeshData mesh) {
        return createMesh(label, mesh.vertexCount(), mesh.indexCount());
    }

    default RenderTexture createTexture(String label, int width, int height) {
        throw new UnsupportedOperationException(id() + " does not implement texture creation yet");
    }

    default RenderCommandList createCommandList() {
        throw new UnsupportedOperationException(id() + " does not implement command recording yet");
    }
}
