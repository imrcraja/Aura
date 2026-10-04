package com.aura.client.renderer.culling;

import com.aura.client.renderer.chunk.ChunkMeshCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Filters chunk mesh keys through a supplied camera frustum before submission. */
public final class ChunkVisibility {
    private ChunkVisibility() {}

    public static List<ChunkMeshCache.Key> visible(
            Collection<ChunkMeshCache.Key> candidates,
            AuraFrustum frustum,
            int sectionHeight) {
        List<ChunkMeshCache.Key> result = new ArrayList<>(candidates.size());
        for (ChunkMeshCache.Key key : candidates) {
            double minX = key.x() * 16.0;
            double minY = key.section() * sectionHeight;
            double minZ = key.z() * 16.0;
            if (frustum.isVisible(minX, minY, minZ,
                    minX + 16.0, minY + sectionHeight, minZ + 16.0)) {
                result.add(key);
            }
        }
        return result;
    }
}
