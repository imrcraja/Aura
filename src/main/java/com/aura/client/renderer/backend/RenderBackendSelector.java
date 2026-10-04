package com.aura.client.renderer.backend;

import com.aura.client.renderer.backend.opengl.OpenGlBackend;
import com.aura.client.renderer.backend.vulkan.VulkanBackend;

/** Selects the safest available backend. Vulkan is opt-in until native capability probing is installed. */
public final class RenderBackendSelector {
    private RenderBackendSelector() {}

    public static RenderBackend select() {
        VulkanBackend vulkan = new VulkanBackend();
        if (vulkan.isAvailable()) return vulkan;
        return new OpenGlBackend();
    }
}
