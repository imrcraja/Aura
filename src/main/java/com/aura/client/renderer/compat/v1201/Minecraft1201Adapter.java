package com.aura.client.renderer.compat.v1201;

import com.aura.client.renderer.compat.VersionAdapter;
import com.aura.client.core.AuraSafeExecutor;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

/**
 * Minecraft 1.20.1 compatibility adapter.
 * Aura observes the lifecycle but never disables vanilla rendering; optional Aura work is isolated.
 */
public final class Minecraft1201Adapter implements VersionAdapter {
    private boolean attached;

    @Override public String minecraftVersion() { return "1.20.1"; }

    @Override
    public void attach() {
        if (attached) return;
        WorldRenderEvents.START.register(context -> AuraSafeExecutor.run(() -> {
            // Frame setup boundary; intentionally side-effect free until a compatible
            // terrain submission bridge is installed.
        }));
        WorldRenderEvents.AFTER_SETUP.register(context -> AuraSafeExecutor.run(() -> {
            // Safe discovery boundary after Minecraft has prepared visible render chunks.
        }));
        WorldRenderEvents.END.register(context -> AuraSafeExecutor.run(() -> {
            // Transient Aura cleanup boundary.
        }));
        attached = true;
    }

    @Override public void detach() {
        // Fabric events are registered once for the client lifetime.
        attached = false;
    }
}
