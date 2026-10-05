package com.aura.client.core;

import com.aura.client.renderer.AuraRenderer;
import com.aura.client.renderer.backend.RenderBackendSelector;
import com.aura.client.renderer.compat.v1201.Minecraft1201Adapter;
import com.aura.client.renderer.chunk.AuraChunkBufferRegistry;
import com.aura.client.renderer.chunk.AuraGpuUploadQueue;
import com.aura.client.renderer.chunk.ChunkGpuCache;
import com.aura.client.renderer.chunk.ChunkMeshCache;
import com.aura.client.renderer.chunk.ChunkMeshData;
import com.aura.client.renderer.chunk.ChunkMeshExtractor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public final class AuraRuntime {
    private static AuraRenderer renderer;
    private static AuraDeviceProfile deviceProfile;
    private static AuraPerformanceManager performanceManager;
    private static Minecraft1201Adapter minecraft1201Adapter;
    private static ChunkMeshCache chunkMeshCache;
    private static ChunkGpuCache chunkGpuCache;
    private static AuraGpuUploadQueue gpuUploadQueue;

    private AuraRuntime() {}

    public static void initialize() {
        deviceProfile = AuraDeviceProfile.detect();
        renderer = new AuraRenderer(RenderBackendSelector.select(), deviceProfile);
        renderer.initialize();
        long cacheBudget = AuraAdaptiveCache.budgetBytes(deviceProfile);
        chunkMeshCache = new ChunkMeshCache(cacheBudget);
        chunkGpuCache = new ChunkGpuCache(cacheBudget);
        gpuUploadQueue = new AuraGpuUploadQueue(256);

        performanceManager = new AuraPerformanceManager();
        performanceManager.register();
        minecraft1201Adapter = new Minecraft1201Adapter();
        minecraft1201Adapter.attach();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdown());
    }

    public static AuraRenderer renderer() {
        if (renderer == null) throw new IllegalStateException("Aura has not been initialized");
        return renderer;
    }

    public static AuraDeviceProfile deviceProfile() {
        if (deviceProfile == null) throw new IllegalStateException("Aura has not been initialized");
        return deviceProfile;
    }

    public static boolean isInitialized() { return renderer != null; }

    public static void observeChunkRebuild() {}

    public static void observeBuiltBuffer(BufferBuilder.BuiltBuffer builtBuffer, VertexBuffer glBuffer) {
        if (!isInitialized() || builtBuffer == null || glBuffer == null ||
                chunkMeshCache == null || gpuUploadQueue == null) return;
        AuraChunkBufferRegistry.Binding binding = AuraChunkBufferRegistry.resolve(glBuffer);
        if (binding == null) return;

        ChunkMeshData extracted = ChunkMeshExtractor.fromBuiltBuffer(builtBuffer);
        if (extracted == null || extracted.indexCount() == 0) return;

        ChunkMeshCache.Key key = binding.key();
        chunkMeshCache.put(key, extracted);
        gpuUploadQueue.offer(key, extracted, glBuffer);
    }

    /** Render-thread-only GPU handoff. Limits uploads per frame to avoid rebuild spikes. */
    public static int drainGpuUploads(int maxUploads) {
        if (!isInitialized() || maxUploads <= 0 || gpuUploadQueue == null || chunkGpuCache == null) return 0;
        int uploaded = 0;
        for (; uploaded < maxUploads; uploaded++) {
            AuraGpuUploadQueue.Pending pending = gpuUploadQueue.poll();
            if (pending == null) break;
            try {
                chunkGpuCache.upload(renderer.backend(), pending.key(), pending.mesh());
            } catch (RuntimeException failure) {
                gpuUploadQueue.offer(pending.key(), pending.mesh(), pending.vanillaBuffer());
                break;
            }
        }
        return uploaded;
    }

    public static int pendingGpuUploads() {
        return gpuUploadQueue == null ? 0 : gpuUploadQueue.size();
    }

    public static void observeRenderLayer(RenderLayer layer, MatrixStack matrices, Matrix4f positionMatrix) {
        if (minecraft1201Adapter == null) return;
        minecraft1201Adapter.observe(layer, matrices, positionMatrix);
        drainGpuUploads(performanceManager == null ? 8 : performanceManager.uploadBudget());
    }

    /**
     * Draws only the GPU meshes belonging to the currently active vanilla render layer.
     * Vanilla remains responsible for the normal draw; Aura is an additional opt-in path
     * until full material/shader equivalence is proven.
     */
    public static int renderCachedLayer(RenderLayer layer, double cameraX, double cameraY, double cameraZ) {
        if (!isInitialized() || layer == null || chunkGpuCache == null) return 0;
        // Cached terrain remains opt-in until shader/material parity with every vanilla
        // render layer is verified. This prevents duplicate terrain in normal gameplay.
        if (!Boolean.parseBoolean(System.getProperty("aura.render.cached_terrain", "false"))) return 0;
        if (!renderer.backend().isContextReady()) return 0;
        String layerKey = AuraChunkBufferRegistry.layerKey(layer);
        var entries = chunkGpuCache.snapshotForLayer(layerKey);
        if (entries.isEmpty()) return 0;

        int maxDistance = Math.max(16, Integer.getInteger("aura.render.distance", 12) * 16);
        long maxDistanceSq = (long) maxDistance * maxDistance;
        java.util.ArrayList<AuraRenderer.PositionedMesh> visible = new java.util.ArrayList<>();
        for (var entry : entries) {
            ChunkMeshCache.Key key = entry.getKey();
            double dx = key.x() + 8.0 - cameraX;
            double dy = key.y() + 8.0 - cameraY;
            double dz = key.z() + 8.0 - cameraZ;
            if (dx * dx + dy * dy + dz * dz <= maxDistanceSq) {
                visible.add(new AuraRenderer.PositionedMesh(entry.getValue(), key.x(), key.y(), key.z()));
            }
        }
        if (visible.isEmpty()) return 0;
        layer.startDrawing();
        try {
            return renderer.drawPositionedMeshes(visible);
        } finally {
            layer.endDrawing();
        }
    }

    public static AuraFrameProfiler frameProfiler() {
        if (performanceManager == null) throw new IllegalStateException("Aura has not been initialized");
        return performanceManager.profiler();
    }

    private static void shutdown() {
        if (minecraft1201Adapter != null) {
            minecraft1201Adapter.detach();
            minecraft1201Adapter = null;
        }
        if (gpuUploadQueue != null) {
            gpuUploadQueue.clear();
            gpuUploadQueue = null;
        }
        if (chunkGpuCache != null) {
            chunkGpuCache.clear();
            chunkGpuCache = null;
        }
        chunkMeshCache = null;
        if (renderer != null) {
            renderer.shutdown();
            renderer = null;
        }
        performanceManager = null;
    }
}
