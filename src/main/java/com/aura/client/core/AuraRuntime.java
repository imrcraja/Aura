package com.aura.client.core;

import com.aura.client.renderer.AuraRenderer;
import com.aura.client.renderer.backend.RenderBackend;
import com.aura.client.renderer.backend.RenderBackendSelector;
import com.aura.client.renderer.backend.opengl.OpenGlBackend;
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

    public static synchronized void initialize() {
        if (isInitialized()) return;
        deviceProfile = AuraDeviceProfile.detect();
        RenderBackend selected = RenderBackendSelector.select();
        renderer = new AuraRenderer(selected, deviceProfile);
        try {
            renderer.initialize();
        } catch (RuntimeException primaryFailure) {
            if (!"opengl".equals(selected.id())) {
                renderer = new AuraRenderer(new OpenGlBackend(), deviceProfile);
                renderer.initialize();
            } else {
                throw primaryFailure;
            }
        }
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
        chunkMeshCache.removeOlderRevisions(
                key.x(), key.y(), key.z(), key.section(), key.layer(), key.revision());
        chunkGpuCache.removeOlderRevisions(
                key.x(), key.y(), key.z(), key.section(), key.layer(), key.revision());
        gpuUploadQueue.removeOlderRevisions(
                key.x(), key.y(), key.z(), key.section(), key.layer(), key.revision());
        chunkMeshCache.put(key, extracted);
        gpuUploadQueue.offer(key, extracted, glBuffer);
    }

    public static int drainGpuUploads(int maxUploads) {
        if (!isInitialized() || maxUploads <= 0 || gpuUploadQueue == null || chunkGpuCache == null) return 0;
        int uploaded = 0;
        while (uploaded < maxUploads) {
            AuraGpuUploadQueue.Pending pending = gpuUploadQueue.poll();
            if (pending == null) break;
            if (chunkMeshCache.get(pending.key()) != pending.mesh()) continue;

            try {
                var gpuMesh = chunkGpuCache.upload(renderer.backend(), pending.key(), pending.mesh());
                if (gpuMesh == null) break;
                uploaded++;
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
     * Returns true only when Aura has a complete, GPU-resident cache for every
     * completed chunk for the supported opaque/cutout terrain layer. This gate
     * prevents cancelling vanilla rendering while the replacement cache is still
     * warming or incomplete.
     */
    public static boolean renderReplacementLayer(RenderLayer layer,
                                                 double cameraX,
                                                 double cameraY,
                                                 double cameraZ,
                                                 int completedChunkCount) {
        if (!isInitialized() || layer == null || chunkGpuCache == null) return false;
        if (!Boolean.parseBoolean(System.getProperty("aura.render.replace_terrain", "false"))) return false;
        if (!renderer.backend().isContextReady() || completedChunkCount <= 0) return false;
        if (!isSupportedReplacementLayer(layer)) return false;

        var entries = chunkGpuCache.snapshotForLayer(AuraChunkBufferRegistry.layerKey(layer));
        if (entries.size() < completedChunkCount) return false;

        int maxDistance = Math.max(16, Integer.getInteger("aura.render.distance", 12) * 16);
        long maxDistanceSq = (long) maxDistance * maxDistance;
        java.util.ArrayList<AuraRenderer.PositionedMesh> visible = new java.util.ArrayList<>(entries.size());
        for (var entry : entries) {
            ChunkMeshCache.Key key = entry.getKey();
            double dx = key.x() + 8.0 - cameraX;
            double dy = key.y() + 8.0 - cameraY;
            double dz = key.z() + 8.0 - cameraZ;
            if (dx * dx + dy * dy + dz * dz <= maxDistanceSq) {
                visible.add(new AuraRenderer.PositionedMesh(entry.getValue(), key.x(), key.y(), key.z()));
            }
        }
        if (visible.isEmpty()) return false;

        layer.startDrawing();
        try {
            return renderer.drawPositionedMeshes(visible) > 0;
        } finally {
            layer.endDrawing();
        }
    }

    private static boolean isSupportedReplacementLayer(RenderLayer layer) {
        return layer == RenderLayer.getSolid()
                || layer == RenderLayer.getCutoutMipped()
                || layer == RenderLayer.getCutout();
    }

    public static int renderCachedLayer(RenderLayer layer, double cameraX, double cameraY, double cameraZ) {
        if (!isInitialized() || layer == null || chunkGpuCache == null) return 0;
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
        if (chunkMeshCache != null) {
            chunkMeshCache.clear();
            chunkMeshCache = null;
        }
        if (renderer != null) {
            renderer.shutdown();
            renderer = null;
        }
        performanceManager = null;
        AuraChunkBufferRegistry.clear();
    }
}
