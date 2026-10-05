package com.aura.client.core;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

/** Lightweight frame telemetry with no render-path allocations per frame. */
public final class AuraPerformanceManager {
    private final AuraFrameProfiler profiler = new AuraFrameProfiler();
    private boolean registered;

    public void register() {
        if (registered) return;
        registered = true;
        WorldRenderEvents.START.register(context -> AuraSafeExecutor.run(profiler::beginFrame));
        WorldRenderEvents.END.register(context -> AuraSafeExecutor.run(profiler::endFrame));
    }

    public AuraFrameProfiler profiler() { return profiler; }
}
