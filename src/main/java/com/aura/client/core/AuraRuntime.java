package com.aura.client.core;

import com.aura.client.renderer.AuraRenderer;
import net.minecraft.client.render.RenderLayer;
import com.aura.client.renderer.backend.RenderBackendSelector;
import com.aura.client.renderer.compat.v1201.Minecraft1201Adapter;
import com.aura.client.renderer.chunk.ChunkGpuCache;
import com.aura.client.renderer.chunk.ChunkMeshCache;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

/** Owns the long-lived Aura runtime. No Minecraft-version-specific renderer code belongs here. */
public final class AuraRuntime {
    private static AuraRenderer renderer;
    private static AuraDeviceProfile deviceProfile;
    private static AuraPerformanceManager performanceManager;
    private static Minecraft1201Adapter minecraft1201Adapter;
    private static ChunkMeshCache chunkMeshCache;
    private static ChunkGpuCache chunkGpuCache;

    private AuraRuntime() {}

    public static void initialize() {
        deviceProfile = AuraDeviceProfile.detect();
        renderer = new AuraRenderer(RenderBackendSelector.select(), deviceProfile);
        renderer.initialize();
        chunkMeshCache = new ChunkMeshCache(AuraAdaptiveCache.budgetBytes(deviceProfile));
        chunkGpuCache = new ChunkGpuCache(AuraAdaptiveCache.gpuEntryBudget(deviceProfile));

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

    public static void observeChunkRebuildTask(net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk.Task task) {
        // Extraction is intentionally deferred until a version-safe RenderData bridge exists.
        // Keeping the task reference out of the runtime avoids retaining chunk rebuild state.
    }

    public static void observeRenderLayer(RenderLayer layer) {
        if (minecraft1201Adapter == null) return;
        // Layer observation is intentionally side-effect free until the version-specific
        // vertex/material extraction bridge is ready.
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
