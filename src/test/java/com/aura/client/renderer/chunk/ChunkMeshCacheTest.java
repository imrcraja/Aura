package com.aura.client.renderer.chunk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChunkMeshCacheTest {
    @Test
    void replacesSameKeyWithoutDoubleCountingBytes() {
        ChunkMeshCache cache = new ChunkMeshCache(1024 * 1024);
        ChunkMeshCache.Key key = new ChunkMeshCache.Key(0,0,0,0,1,"solid");
        ChunkMeshData first = new ChunkMeshData(new float[]{0,0,0}, new int[]{0});
        ChunkMeshData second = new ChunkMeshData(new float[]{0,0,0,1,1,1}, new int[]{0,0});
        cache.put(key, first);
        cache.put(key, second);
        assertEquals(second.estimatedBytes(), cache.usedBytes());
        assertEquals(second, cache.get(key));
    }

    @Test
    void removesOnlyOlderMatchingRevision() {
        ChunkMeshCache cache = new ChunkMeshCache(1024 * 1024);
        ChunkMeshData mesh = new ChunkMeshData(new float[]{0,0,0}, new int[]{0});
        cache.put(new ChunkMeshCache.Key(1,2,3,0,1,"solid"), mesh);
        cache.put(new ChunkMeshCache.Key(1,2,3,0,2,"solid"), mesh);
        cache.put(new ChunkMeshCache.Key(1,2,3,0,1,"cutout"), mesh);
        assertEquals(1, cache.removeOlderRevisions(1,2,3,0,"solid",2));
        assertNull(cache.get(new ChunkMeshCache.Key(1,2,3,0,1,"solid")));
        assertNotNull(cache.get(new ChunkMeshCache.Key(1,2,3,0,1,"cutout")));
    }
}
