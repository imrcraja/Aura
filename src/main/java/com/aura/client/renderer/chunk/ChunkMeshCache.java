package com.aura.client.renderer.chunk;

import java.util.Iterator;
import java.util.LinkedHashMap;

/** Bounded LRU cache for reusable CPU-side chunk meshes. */
public final class ChunkMeshCache {
    public record Key(int x, int y, int z, int section, int revision, String layer) {
        public Key {
            if (layer == null || layer.isBlank()) layer = "unknown";
        }
        public Key(int x, int y, int z, int section, int revision) {
            this(x, y, z, section, revision, "unknown");
        }
    }

    private final long budgetBytes;
    private long usedBytes;
    private final LinkedHashMap<Key, ChunkMeshData> entries = new LinkedHashMap<>(32, 0.75f, true);

    public ChunkMeshCache(long budgetBytes) {
        if (budgetBytes < 1) throw new IllegalArgumentException("budgetBytes must be positive");
        this.budgetBytes = budgetBytes;
    }

    public synchronized ChunkMeshData get(Key key) { return entries.get(key); }

    public synchronized void put(Key key, ChunkMeshData mesh) {
        if (key == null || mesh == null) return;
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

    public synchronized int removeOlderRevisions(int x, int y, int z, int section, String layer, int revision) {
        int removed = 0;
        Iterator<java.util.Map.Entry<Key, ChunkMeshData>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<Key, ChunkMeshData> entry = it.next();
            Key key = entry.getKey();
            if (key.x() == x && key.y() == y && key.z() == z &&
                    key.section() == section && key.revision() < revision &&
                    key.layer().equals(layer)) {
                usedBytes -= entry.getValue().estimatedBytes();
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    public synchronized int removeChunk(int x, int y, int z) {
        int removed = 0;
        var it = entries.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            Key key = entry.getKey();
            if (key.x() == x && key.y() == y && key.z() == z) {
                usedBytes -= entry.getValue().estimatedBytes();
                it.remove();
                removed++;
            }
        }
        return removed;
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
