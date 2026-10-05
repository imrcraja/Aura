package com.aura.client.renderer.chunk;

import net.minecraft.client.gl.VertexBuffer;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Render-thread handoff for extracted chunk meshes.
 * Chunk rebuilding may happen off-thread, while OpenGL resource creation must happen
 * with Minecraft's active render context.
 */
public final class AuraGpuUploadQueue {
    public record Pending(ChunkMeshCache.Key key, ChunkMeshData mesh, VertexBuffer vanillaBuffer) {}

    private final Deque<Pending> pending = new ArrayDeque<>();
    private final int maxPending;

    public AuraGpuUploadQueue(int maxPending) {
        if (maxPending < 1) throw new IllegalArgumentException("maxPending must be positive");
        this.maxPending = maxPending;
    }

    public synchronized void offer(ChunkMeshCache.Key key, ChunkMeshData mesh, VertexBuffer vanillaBuffer) {
        if (key == null || mesh == null || mesh.indexCount() == 0) return;
        if (pending.size() >= maxPending) pending.removeFirst();
        pending.addLast(new Pending(key, mesh, vanillaBuffer));
    }

    public synchronized Pending poll() { return pending.pollFirst(); }
    public synchronized int size() { return pending.size(); }
    public synchronized void clear() { pending.clear(); }
}
