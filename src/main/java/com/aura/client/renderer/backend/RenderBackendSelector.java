package com.aura.client.renderer.backend;

import com.aura.client.renderer.backend.opengl.OpenGlBackend;
import com.aura.client.renderer.backend.vulkan.VulkanBackend;

/** Chooses Vulkan only when explicitly enabled and available; otherwise uses OpenGL. */
public final class RenderBackendSelector {
    private RenderBackendSelector() {}

    public static RenderBackend select() {
        VulkanBackend vulkan = new VulkanBackend();
        if (vulkan.isAvailable()) return vulkan;
        return new OpenGlBackend();
    }
}
