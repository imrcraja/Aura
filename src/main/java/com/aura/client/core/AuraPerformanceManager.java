package com.aura.client.core;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

/** Frame telemetry plus bounded adaptive render workload control. */
public final class AuraPerformanceManager {
    private final AuraFrameProfiler profiler = new AuraFrameProfiler();
    private boolean registered;
    private int uploadBudget = 8;
    private int stableFrames;

    public void register() {
        if (registered) return;
        registered = true;
        WorldRenderEvents.START.register(context -> AuraSafeExecutor.run(profiler::beginFrame));
        WorldRenderEvents.END.register(context -> AuraSafeExecutor.run(() -> {
            profiler.endFrame();
            adapt();
        }));
    }

    public AuraFrameProfiler profiler() { return profiler; }

    public int uploadBudget() { return uploadBudget; }

    /** Conservative scale for expensive render work. */
    public double workloadScale() {
        double ms = profiler.averageFrameMillis();
        if (ms <= 0.0) return 1.0;
        if (ms >= 33.0) return 0.60;
        if (ms >= 25.0) return 0.70;
        if (ms >= 20.0) return 0.80;
        if (ms <= 9.0) return 1.0;
        return 0.90;
    }

    private void adapt() {
        double ms = profiler.averageFrameMillis();
        if (ms <= 0.0) return;

        if (ms >= 33.0) {
            uploadBudget = Math.max(2, uploadBudget - 2);
            stableFrames = 0;
        } else if (ms >= 25.0) {
            uploadBudget = Math.max(3, uploadBudget - 1);
            stableFrames = 0;
        } else if (ms <= 14.0) {
            stableFrames++;
            if (stableFrames >= 30) {
                uploadBudget = Math.min(16, uploadBudget + 1);
                stableFrames = 0;
            }
        } else {
            stableFrames = 0;
        }
    }
}
