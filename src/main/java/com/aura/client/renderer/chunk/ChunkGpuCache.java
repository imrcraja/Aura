package com.aura.client.renderer.chunk;

import com.aura.client.renderer.backend.RenderBackend;
import com.aura.client.renderer.backend.RenderMesh;

import java.util.LinkedHashMap;
import java.util.Map;

/** Reuses backend mesh handles with bounded ownership and explicit cleanup. */
public final class ChunkGpuCache {
    private final int maxEntries;
    private final LinkedHashMap<ChunkMeshCache.Key, RenderMesh> entries =
            new LinkedHashMap<>(32, 0.75f, true);

    public ChunkGpuCache(int maxEntries) {
        if (maxEntries < 1) throw new IllegalArgumentException("maxEntries must be positive");
        this.maxEntries = maxEntries;
    }

    public synchronized RenderMesh get(ChunkMeshCache.Key key) { return entries.get(key); }

    public synchronized RenderMesh upload(RenderBackend backend, ChunkMeshCache.Key key, ChunkMeshData mesh) {
        RenderMesh old = entries.remove(key);
        if (old != null) old.close();
        RenderMesh uploaded = backend.uploadChunkMesh(
                "chunk-" + key.x() + "-" + key.y() + "-" + key.z(), mesh);
        entries.put(key, uploaded);
        trim();
        return uploaded;
    }

    public synchronized void remove(ChunkMeshCache.Key key) {
        RenderMesh old = entries.remove(key);
        if (old != null) old.close();
    }

    public synchronized void clear() {
        for (RenderMesh mesh : entries.values()) mesh.close();
        entries.clear();
    }

    public synchronized int size() { return entries.size(); }

    private void trim() {
        while (entries.size() > maxEntries) {
            var it = entries.entrySet().iterator();
            Map.Entry<ChunkMeshCache.Key, RenderMesh> oldest = it.next();
            oldest.getValue().close();
            it.remove();
        }
    }
}
