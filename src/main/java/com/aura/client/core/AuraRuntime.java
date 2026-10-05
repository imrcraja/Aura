package com.aura.client.core;

import com.aura.client.renderer.AuraRenderer;
import com.aura.client.renderer.backend.RenderBackendSelector;
import com.aura.client.renderer.compat.v1201.Minecraft1201Adapter;
import com.aura.client.renderer.chunk.AuraChunkBufferRegistry;
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
        chunkGpuCache = new ChunkGpuCache(AuraAdaptiveCache.budgetBytes(deviceProfile));

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
        if (!isInitialized() || builtBuffer == null || glBuffer == null || chunkMeshCache == null) return;
        AuraChunkBufferRegistry.Binding binding = AuraChunkBufferRegistry.resolve(glBuffer);
        if (binding == null) return;

        ChunkMeshData extracted = ChunkMeshExtractor.fromBuiltBuffer(builtBuffer);
        if (extracted == null || extracted.indexCount() == 0) return;
        chunkMeshCache.put(binding.key(), extracted);
    }

    public static void observeRenderLayer(RenderLayer layer, MatrixStack matrices, Matrix4f positionMatrix) {
        if (minecraft1201Adapter == null) return;
        minecraft1201Adapter.observe(layer, matrices, positionMatrix);
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
