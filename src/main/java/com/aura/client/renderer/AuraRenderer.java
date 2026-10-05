package com.aura.client.renderer;

import com.aura.client.core.AuraDeviceProfile;
import com.aura.client.renderer.backend.RenderBackend;
import com.aura.client.renderer.backend.RenderCommandList;
import com.aura.client.renderer.backend.RenderMesh;
import com.aura.client.renderer.batching.RenderBatcher;
import com.aura.client.renderer.chunk.ChunkMeshCache;
import com.aura.client.renderer.culling.AuraFrustum;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Stable renderer facade shared by all supported Minecraft version adapters. */
public final class AuraRenderer {
    private final RenderBackend backend;
    private final AuraDeviceProfile deviceProfile;
    private final RenderBatcher batcher = new RenderBatcher();

    public AuraRenderer(RenderBackend backend, AuraDeviceProfile deviceProfile) {
        this.backend = backend;
        this.deviceProfile = deviceProfile;
    }

    public void initialize() { backend.initialize(); }
    public void shutdown() { backend.shutdown(); }
    public RenderBackend backend() { return backend; }
    public AuraDeviceProfile deviceProfile() { return deviceProfile; }

    /**
     * Records visible cached meshes through one command list. Backends may optimize the
     * command recording/submission without version adapters knowing backend details.
     */
    public int renderVisible(Collection<ChunkMeshCache.Key> candidates,
                             AuraFrustum frustum,
                             Collection<VisibleMesh> meshes) {
        batcher.clear();
        Set<ChunkMeshCache.Key> candidateSet = candidates instanceof Set<ChunkMeshCache.Key> set
                ? set : new HashSet<>(candidates);
        for (VisibleMesh mesh : meshes) {
            if (candidateSet.contains(mesh.key()) && frustum.isVisible(
                    mesh.minX(), mesh.minY(), mesh.minZ(),
                    mesh.maxX(), mesh.maxY(), mesh.maxZ())) {
                batcher.add(mesh.materialKey(), mesh.mesh());
            }
        }

        if (batcher.meshCount() == 0) return 0;
        try (RenderCommandList commands = backend.createCommandList()) {
            commands.begin();
            for (var batch : batcher.batches().values()) {
                for (RenderMesh mesh : batch.meshes()) commands.draw(mesh);
            }
            commands.end();
        }
        return batcher.meshCount();
    }

    public record VisibleMesh(ChunkMeshCache.Key key, String materialKey, RenderMesh mesh,
                              double minX, double minY, double minZ,
                              double maxX, double maxY, double maxZ) {}
}
