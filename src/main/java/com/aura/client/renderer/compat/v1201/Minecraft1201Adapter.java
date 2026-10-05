package com.aura.client.renderer.compat.v1201;

import com.aura.client.core.AuraSafeExecutor;
import com.aura.client.renderer.compat.VersionAdapter;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.joml.Matrix4f;

/**
 * 1.20.1 version adapter.
 *
 * Exposes the render-layer boundary and camera matrices to the version-neutral runtime.
 * The adapter never cancels vanilla rendering; replacement is enabled only when a concrete
 * mesh/material bridge is available.
 */
public final class Minecraft1201Adapter implements VersionAdapter {
    private boolean attached;
    private WorldRenderContext currentContext;
    private RenderLayer currentLayer;
    private Matrix4f currentPositionMatrix;

    @Override public String minecraftVersion() { return "1.20.1"; }

    @Override
    public void attach() {
        if (attached) return;
        WorldRenderEvents.START.register(context -> AuraSafeExecutor.run(() -> currentContext = context));
        WorldRenderEvents.AFTER_SETUP.register(context -> AuraSafeExecutor.run(() -> currentContext = context));
        WorldRenderEvents.END.register(context -> AuraSafeExecutor.run(() -> {
            currentContext = null;
            currentLayer = null;
            currentPositionMatrix = null;
        }));
        attached = true;
    }

    public void observe(RenderLayer layer, MatrixStack matrices) {
        currentLayer = layer;
        currentPositionMatrix = matrices.peek().getPositionMatrix();
    }

    public WorldRenderContext currentContext() { return currentContext; }
    public RenderLayer currentLayer() { return currentLayer; }
    public Matrix4f currentPositionMatrix() { return currentPositionMatrix; }
    public boolean hasRenderContext() { return currentContext != null; }

    @Override public void detach() {
        currentContext = null;
        currentLayer = null;
        currentPositionMatrix = null;
        attached = false;
    }
}
