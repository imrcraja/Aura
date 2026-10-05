package com.aura.client.renderer.chunk;

import com.aura.client.renderer.backend.RenderBackend;
import com.aura.client.renderer.backend.RenderMesh;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChunkGpuCache {
    private final long budgetBytes;
    private long usedBytes;
    private final LinkedHashMap<ChunkMeshCache.Key, RenderMesh> entries = new LinkedHashMap<>(32, 0.75f, true);

    public ChunkGpuCache(long budgetBytes) {
        if (budgetBytes < 1) throw new IllegalArgumentException("budgetBytes must be positive");
        this.budgetBytes = budgetBytes;
    }

    public synchronized RenderMesh get(ChunkMeshCache.Key key) { return entries.get(key); }

    public synchronized RenderMesh upload(RenderBackend backend, ChunkMeshCache.Key key, ChunkMeshData mesh) {
        if (backend == null || key == null || mesh == null) throw new IllegalArgumentException("backend, key and mesh are required");
        RenderMesh old = entries.remove(key);
        if (old != null) usedBytes -= old.estimatedBytes();
        RenderMesh uploaded = backend.uploadChunkMesh(
                "chunk-" + key.x() + "-" + key.y() + "-" + key.z() + "-" + key.layer(), mesh);
        entries.put(key, uploaded);
        usedBytes += uploaded.estimatedBytes();
        trim();
        return uploaded;
    }

    public synchronized int removeOlderRevisions(int x, int y, int z, int section, String layer, int revision) {
        int removed = 0;
        Iterator<Map.Entry<ChunkMeshCache.Key, RenderMesh>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ChunkMeshCache.Key, RenderMesh> entry = it.next();
            ChunkMeshCache.Key key = entry.getKey();
            if (key.x() == x && key.y() == y && key.z() == z &&
                    key.section() == section && key.revision() < revision &&
                    key.layer().equals(layer)) {
                usedBytes -= entry.getValue().estimatedBytes();
                entry.getValue().close();
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    public synchronized void remove(ChunkMeshCache.Key key) {
        RenderMesh old = entries.remove(key);
        if (old != null) {
            usedBytes -= old.estimatedBytes();
            old.close();
        }
    }

    public synchronized int removeChunk(int x, int y, int z) {
        int removed = 0;
        var it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ChunkMeshCache.Key, RenderMesh> entry = it.next();
            ChunkMeshCache.Key key = entry.getKey();
            if (key.x() == x && key.y() == y && key.z() == z) {
                usedBytes -= entry.getValue().estimatedBytes();
                entry.getValue().close();
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    public synchronized List<Map.Entry<ChunkMeshCache.Key, RenderMesh>> snapshotForLayer(String layer) {
        List<Map.Entry<ChunkMeshCache.Key, RenderMesh>> result = new ArrayList<>();
        for (Map.Entry<ChunkMeshCache.Key, RenderMesh> entry : entries.entrySet()) {
            if (layer == null || layer.equals(entry.getKey().layer())) result.add(Map.entry(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    public synchronized void clear() {
        for (RenderMesh mesh : entries.values()) mesh.close();
        entries.clear();
        usedBytes = 0L;
    }

    public synchronized int size() { return entries.size(); }
    public synchronized long usedBytes() { return usedBytes; }
    public long budgetBytes() { return budgetBytes; }

    private void trim() {
        var it = entries.entrySet().iterator();
        while (usedBytes > budgetBytes && it.hasNext()) {
            Map.Entry<ChunkMeshCache.Key, RenderMesh> oldest = it.next();
            usedBytes -= oldest.getValue().estimatedBytes();
            oldest.getValue().close();
            it.remove();
        }
    }
}
