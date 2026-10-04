package com.aura.client.renderer.batching;

import com.aura.client.renderer.backend.RenderMesh;

import java.util.ArrayList;
import java.util.List;

/** Groups compatible mesh submissions to reduce command/state transitions. */
public final class RenderBatch {
    private final String materialKey;
    private final List<RenderMesh> meshes = new ArrayList<>();

    public RenderBatch(String materialKey) {
        this.materialKey = materialKey;
    }

    public void add(RenderMesh mesh) { meshes.add(mesh); }
    public String materialKey() { return materialKey; }
    public List<RenderMesh> meshes() { return List.copyOf(meshes); }
    public int size() { return meshes.size(); }
}
