package com.aura.client.renderer.batching;

import com.aura.client.renderer.backend.RenderMesh;

import java.util.LinkedHashMap;
import java.util.Map;

/** Collects compatible submissions in stable order before command recording. */
public final class RenderBatcher {
    private final Map<String, RenderBatch> batches = new LinkedHashMap<>();

    public void add(String materialKey, RenderMesh mesh) {
        batches.computeIfAbsent(materialKey, RenderBatch::new).add(mesh);
    }

    public Map<String, RenderBatch> batches() {
        return Map.copyOf(batches);
    }

    public int batchCount() { return batches.size(); }

    public int meshCount() {
        return batches.values().stream().mapToInt(RenderBatch::size).sum();
    }

    public void clear() { batches.clear(); }
}
