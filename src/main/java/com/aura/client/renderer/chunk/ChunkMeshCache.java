package com.aura.client.renderer.chunk;

import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded LRU cache for reusable CPU-side chunk meshes. */
public final class ChunkMeshCache {
    public record Key(int x, int y, int z, int section, int revision) {}

    private final long budgetBytes;
    private long usedBytes;
    private final LinkedHashMap<Key, ChunkMeshData> entries = new LinkedHashMap<>(32, 0.75f, true);

    public ChunkMeshCache(long budgetBytes) {
        if (budgetBytes < 1) throw new IllegalArgumentException("budgetBytes must be positive");
        this.budgetBytes = budgetBytes;
    }

    public synchronized ChunkMeshData get(Key key) { return entries.get(key); }

    public synchronized void put(Key key, ChunkMeshData mesh) {
        ChunkMeshData old = entries.put(key, mesh);
        if (old != null) usedBytes -= old.estimatedBytes();
        usedBytes += mesh.estimatedBytes();
        trim();
    }

    public synchronized ChunkMeshData remove(Key key) {
        ChunkMeshData old = entries.remove(key);
        if (old != null) usedBytes -= old.estimatedBytes();
        return old;
    }

    public synchronized void clear() { entries.clear(); usedBytes = 0; }
    public synchronized long usedBytes() { return usedBytes; }
    public long budgetBytes() { return budgetBytes; }
    public synchronized int size() { return entries.size(); }

    private void trim() {
        var it = entries.entrySet().iterator();
        while (usedBytes > budgetBytes && it.hasNext()) {
            usedBytes -= it.next().getValue().estimatedBytes();
            it.remove();
        }
    }
}
