package com.aura.client.renderer.backend;

public interface RenderMesh extends RenderResource {
    int vertexCount();
    int indexCount();

    /** Approximate native GPU allocation used for adaptive cache accounting. */
    default long estimatedBytes() {
        return Math.max(0L, (long) vertexCount() * 12L + (long) indexCount() * Integer.BYTES);
    }
}
