package com.aura.client.renderer.backend;

import com.aura.client.renderer.backend.opengl.OpenGlBackend;
import com.aura.client.renderer.backend.vulkan.VulkanBackend;

/** Chooses the highest-capability backend without making launcher-specific assumptions. */
public final class RenderBackendSelector {
    private RenderBackendSelector() {}

    public static RenderBackend select() {
        VulkanBackend vulkan = new VulkanBackend();
        if (vulkan.isAvailable()) return vulkan;
        return new OpenGlBackend();
    }
}
