package com.aura.client.renderer.compat.v1201;

import com.aura.client.core.AuraSafeExecutor;
import com.aura.client.renderer.compat.VersionAdapter;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

/**
 * Minecraft 1.20.1 adapter.
 *
 * Fabric exposes the render lifecycle and the prepared render-chunk boundary, but it does
 * not expose vanilla chunk meshes as AuraMesh objects. This adapter therefore captures the
 * per-frame render context without replacing vanilla terrain. A future mixin/bridge can feed
 * the version-neutral mesh pipeline without changing the renderer core.
 */
public final class Minecraft1201Adapter implements VersionAdapter {
    private boolean attached;
    private WorldRenderContext currentContext;

    @Override public String minecraftVersion() { return "1.20.1"; }

    @Override
    public void attach() {
        if (attached) return;
        WorldRenderEvents.START.register(context -> AuraSafeExecutor.run(() -> currentContext = context));
        WorldRenderEvents.AFTER_SETUP.register(context -> AuraSafeExecutor.run(() -> currentContext = context));
        WorldRenderEvents.END.register(context -> AuraSafeExecutor.run(() -> currentContext = null));
        attached = true;
    }

    public WorldRenderContext currentContext() {
        return currentContext;
    }

    public boolean hasRenderContext() {
        return currentContext != null;
    }

    @Override public void detach() {
        currentContext = null;
        attached = false;
    }
}
