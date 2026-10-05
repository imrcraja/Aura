package com.aura.client.renderer.chunk;

import net.minecraft.client.gl.VertexBuffer;

import java.util.ArrayDeque;
import java.util.Deque;

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

    public synchronized int removeOlderRevisions(int x, int y, int z, int section, String layer, int revision) {
        int removed = 0;
        var it = pending.iterator();
        while (it.hasNext()) {
            Pending item = it.next();
            ChunkMeshCache.Key key = item.key();
            if (key.x() == x && key.y() == y && key.z() == z &&
                    key.section() == section && key.revision() < revision &&
                    key.layer().equals(layer)) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    public synchronized Pending poll() { return pending.pollFirst(); }
    public synchronized int size() { return pending.size(); }
    public synchronized void clear() { pending.clear(); }
}
