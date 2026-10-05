package com.aura.client.renderer.chunk;

import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Maps vanilla chunk VertexBuffers back to their owning built chunk and render layer. */
public final class AuraChunkBufferRegistry {
    public record Binding(int x, int y, int z, int section, String layer, int revision) {
        public ChunkMeshCache.Key key() {
            return new ChunkMeshCache.Key(x, y, z, section, revision, layer);
        }
    }

    private static final Map<VertexBuffer, BindingSeed> BUFFERS = new IdentityHashMap<>();
    private static final Map<ChunkBuilder.BuiltChunk, AtomicInteger> REVISIONS = new IdentityHashMap<>();

    private AuraChunkBufferRegistry() {}

    public static synchronized void bind(ChunkBuilder.BuiltChunk chunk, RenderLayer layer, VertexBuffer buffer) {
        if (chunk == null || layer == null || buffer == null) return;
        BlockPos origin = chunk.getOrigin();
        if (origin == null) return;
        REVISIONS.computeIfAbsent(chunk, ignored -> new AtomicInteger()).incrementAndGet();
        BUFFERS.put(buffer, new BindingSeed(
                origin.getX(), origin.getY(), origin.getZ(),
                origin.getY() >> 4,
                layerKey(layer), chunk));
    }

    public static synchronized Binding resolve(VertexBuffer buffer) {
        BindingSeed seed = BUFFERS.get(buffer);
        if (seed == null) return null;
        int revision = REVISIONS.getOrDefault(seed.chunk(), new AtomicInteger()).get();
        return new Binding(seed.x(), seed.y(), seed.z(), seed.section(), seed.layer(), revision);
    }

    public static synchronized void unbind(ChunkBuilder.BuiltChunk chunk) {
        if (chunk == null) return;
        BUFFERS.entrySet().removeIf(entry -> entry.getValue().chunk() == chunk);
        REVISIONS.remove(chunk);
    }

    public static String layerKey(RenderLayer layer) {
        if (layer == null) return "unknown";
        return layer.getClass().getName() + ":" +
                layer.getVertexFormat().getVertexSizeByte() + ":" +
                layer.getDrawMode().name();
    }

    private record BindingSeed(int x, int y, int z, int section, String layer,
                               ChunkBuilder.BuiltChunk chunk) {}
}
