package com.aura.client.renderer.backend.vulkan;

import com.aura.client.renderer.backend.RenderBackend;

/** Safe Vulkan capability boundary; native context ownership stays with the launcher/runtime. */
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
        return os.contains("linux") || os.contains("android") || os.contains("windows");
    }

    public boolean initialized() { return initialized; }
}
