package com.aura.client.core;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

/**
 * Connects version-neutral performance telemetry to Fabric's 1.20.1 world-render
 * lifecycle. Later version adapters can feed the same manager without changing
 * the renderer core.
 */
public final class AuraPerformanceManager {
    private final AuraFrameProfiler profiler = new AuraFrameProfiler();
    private boolean registered;

    public void register() {
        if (registered) return;
        registered = true;
        WorldRenderEvents.START.register(context -> profiler.beginFrame());
        WorldRenderEvents.END.register(context -> profiler.endFrame());
    }

    public AuraFrameProfiler profiler() {
        return profiler;
    }
}
