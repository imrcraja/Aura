package com.aura.client.renderer.backend.vulkan;

import com.aura.client.renderer.backend.RenderBackend;

/**
 * Vulkan capability boundary.
 *
 * This first implementation deliberately does not load native Vulkan libraries itself.
 * Native loading is isolated here so Android launcher/driver differences cannot leak into
 * the common renderer. The production backend will be supplied by the platform layer.
 */
public final class VulkanBackend implements RenderBackend {
    @Override public String id() { return "vulkan"; }
    @Override public void initialize() { }
    @Override public void shutdown() { }
    @Override public boolean isAvailable() {
        return Boolean.parseBoolean(System.getProperty("aura.vulkan.enabled", "false"));
    }
}
