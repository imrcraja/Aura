package com.aura.client.renderer;

import com.aura.client.renderer.backend.RenderBackend;

/** Stable renderer facade shared by all supported Minecraft version adapters. */
public final class AuraRenderer {
    private final RenderBackend backend;

    public AuraRenderer(RenderBackend backend) {
        this.backend = backend;
    }

    public void initialize() {
        backend.initialize();
    }

    public void shutdown() {
        backend.shutdown();
    }

    public RenderBackend backend() {
        return backend;
    }
}
