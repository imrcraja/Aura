package com.aura.client.renderer.compat.v1201;

import com.aura.client.renderer.compat.VersionAdapter;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

/**
 * Minecraft 1.20.1 integration boundary.
 *
 * Aura observes the vanilla render lifecycle through Fabric events first. This keeps the
 * adapter safe while the production GPU submission path is built in the version-neutral core.
 */
public final class Minecraft1201Adapter implements VersionAdapter {
    private boolean attached;

    @Override
    public String minecraftVersion() {
        return "1.20.1";
    }

    @Override
    public void attach() {
        if (attached) return;

        WorldRenderEvents.START.register(context -> {
            // Reserved for frame/context setup once the production renderer owns submission.
        });

        WorldRenderEvents.AFTER_SETUP.register(context -> {
            // Frustum/render-chunk discovery boundary. Aura must not replace vanilla submission yet.
        });

        WorldRenderEvents.END.register(context -> {
            // Reserved for transient Aura frame cleanup.
        });

        attached = true;
    }

    @Override
    public void detach() {
        // Fabric's event API does not provide per-listener removal here; the adapter is registered once.
        attached = false;
    }
}
