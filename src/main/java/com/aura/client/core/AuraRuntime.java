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
        drainGpuUploads(8);
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
