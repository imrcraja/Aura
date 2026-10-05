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

public final class AuraRenderer {
    private final RenderBackend backend;
    private final AuraDeviceProfile deviceProfile;
    private final RenderBatcher batcher = new RenderBatcher();
    private boolean active = true;
    private Throwable lastFailure;

    public AuraRenderer(RenderBackend backend, AuraDeviceProfile deviceProfile) {
        this.backend = backend;
        this.deviceProfile = deviceProfile;
    }

    public void initialize() { backend.initialize(); }

    public void shutdown() {
        active = false;
        backend.shutdown();
    }

    public RenderBackend backend() { return backend; }
    public AuraDeviceProfile deviceProfile() { return deviceProfile; }
    public boolean active() { return active; }
    public Throwable lastFailure() { return lastFailure; }

    public int drawMeshes(Collection<RenderMesh> meshes) {
        if (!active || meshes == null || meshes.isEmpty()) return 0;
        try (RenderCommandList commands = backend.createCommandList()) {
            commands.begin();
            int count = 0;
            for (RenderMesh mesh : meshes) {
                if (mesh == null) continue;
                commands.draw(mesh);
                count++;
            }
            commands.end();
            return count;
        } catch (RuntimeException failure) {
            lastFailure = failure;
            active = false;
            return 0;
        }
    }

    public int renderVisible(Collection<ChunkMeshCache.Key> candidates,
                             AuraFrustum frustum,
                             Collection<VisibleMesh> meshes) {
        if (!active || candidates == null || frustum == null || meshes == null) return 0;

        batcher.clear();
        Set<ChunkMeshCache.Key> candidateSet = candidates instanceof Set<ChunkMeshCache.Key> set
                ? set : new HashSet<>(candidates);

        for (VisibleMesh mesh : meshes) {
            if (mesh == null || mesh.mesh() == null) continue;
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
            return batcher.meshCount();
        } catch (RuntimeException failure) {
            lastFailure = failure;
            active = false;
            return 0;
        }
    }

    public record VisibleMesh(ChunkMeshCache.Key key, String materialKey, RenderMesh mesh,
                              double minX, double minY, double minZ,
                              double maxX, double maxY, double maxZ) {}
}
