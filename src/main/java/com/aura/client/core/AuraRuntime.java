package com.aura.client.core;

import com.aura.client.renderer.AuraRenderer;
import com.aura.client.renderer.backend.RenderBackend;
import com.aura.client.renderer.backend.RenderBackendSelector;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

/** Owns the long-lived Aura runtime. No Minecraft-version-specific renderer code belongs here. */
public final class AuraRuntime {
    private static AuraRenderer renderer;

    private AuraRuntime() {}

    public static void initialize() {
        renderer = new AuraRenderer(RenderBackendSelector.select());
        renderer.initialize();

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdown());
    }

    public static AuraRenderer renderer() {
        if (renderer == null) throw new IllegalStateException("Aura has not been initialized");
        return renderer;
    }

    private static void shutdown() {
        if (renderer != null) {
            renderer.shutdown();
            renderer = null;
        }
    }
}
