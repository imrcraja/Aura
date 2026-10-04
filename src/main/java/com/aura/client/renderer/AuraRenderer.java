package com.aura.client.renderer;

import com.aura.client.core.AuraDeviceProfile;
import com.aura.client.renderer.backend.RenderBackend;

/** Stable renderer facade shared by all supported Minecraft version adapters. */
public final class AuraRenderer {
    private final RenderBackend backend;
    private final AuraDeviceProfile deviceProfile;

    public AuraRenderer(RenderBackend backend, AuraDeviceProfile deviceProfile) {
        this.backend = backend;
        this.deviceProfile = deviceProfile;
    }

    public void initialize() { backend.initialize(); }
    public void shutdown() { backend.shutdown(); }
    public RenderBackend backend() { return backend; }
    public AuraDeviceProfile deviceProfile() { return deviceProfile; }
}
