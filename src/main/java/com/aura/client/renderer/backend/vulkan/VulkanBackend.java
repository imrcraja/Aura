package com.aura.client.renderer.backend.vulkan;

import com.aura.client.renderer.backend.RenderBackend;
import com.aura.client.renderer.backend.RenderCommandList;
import com.aura.client.renderer.backend.RenderMesh;
import com.aura.client.renderer.backend.RenderTexture;
import com.aura.client.renderer.chunk.ChunkMeshData;

/**
 * Vulkan capability boundary. Native Vulkan device/swapchain ownership is intentionally
 * delegated to the launcher/runtime until a loader/device handle can be obtained safely.
 * This backend never pretends to be available merely because Android exposes Vulkan.
 */
public final class VulkanBackend implements RenderBackend {
    private boolean initialized;

    @Override public String id() { return "vulkan"; }

    @Override public void initialize() {
        if (!isAvailable()) throw new IllegalStateException("Aura Vulkan backend is unavailable");
        initialized = true;
    }

    @Override public void shutdown() { initialized = false; }

    @Override public boolean isAvailable() {
        if (!Boolean.parseBoolean(System.getProperty("aura.vulkan.enabled", "false"))) return false;
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!(os.contains("linux") || os.contains("android") || os.contains("windows"))) return false;

        // Explicit runtime contract: a launcher/host must provide a native Vulkan context.
        // The Java-side backend is only selectable when a real native resource bridge
        // has been installed by the host/launcher. A context string alone is not enough.
        return Boolean.parseBoolean(System.getProperty("aura.vulkan.bridge", "false"));
    }

    @Override public boolean isContextReady() { return initialized; }

    @Override public RenderMesh createMesh(String label, int vertexCount, int indexCount) {
        throw new UnsupportedOperationException("Vulkan native resource bridge is not installed");
    }

    @Override public RenderMesh uploadChunkMesh(String label, ChunkMeshData mesh) {
        throw new UnsupportedOperationException("Vulkan native resource bridge is not installed");
    }

    @Override public RenderTexture createTexture(String label, int width, int height) {
        throw new UnsupportedOperationException("Vulkan native resource bridge is not installed");
    }

    @Override public RenderCommandList createCommandList() {
        throw new UnsupportedOperationException("Vulkan native resource bridge is not installed");
    }

    public boolean initialized() { return initialized; }
}
