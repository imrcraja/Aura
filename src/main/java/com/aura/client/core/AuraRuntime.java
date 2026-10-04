package com.aura.client.core;

import com.aura.client.renderer.AuraRenderer;
import com.aura.client.renderer.backend.RenderBackendSelector;
import com.aura.client.renderer.compat.v1201.Minecraft1201Adapter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

/** Owns the long-lived Aura runtime. No Minecraft-version-specific renderer code belongs here. */
public final class AuraRuntime {
    private static AuraRenderer renderer;
    private static AuraDeviceProfile deviceProfile;
    private static AuraPerformanceManager performanceManager;
    private static Minecraft1201Adapter minecraft1201Adapter;

    private AuraRuntime() {}

    public static void initialize() {
        deviceProfile = AuraDeviceProfile.detect();
        renderer = new AuraRenderer(RenderBackendSelector.select(), deviceProfile);
        renderer.initialize();

        performanceManager = new AuraPerformanceManager();
        performanceManager.register();

        minecraft1201Adapter = new Minecraft1201Adapter();
        minecraft1201Adapter.attach();

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> shutdown());
    }

    public static AuraRenderer renderer() {
        if (renderer == null) throw new IllegalStateException("Aura has not been initialized");
        return renderer;
    }

    public static AuraDeviceProfile deviceProfile() {
        if (deviceProfile == null) throw new IllegalStateException("Aura has not been initialized");
        return deviceProfile;
    }

    public static AuraFrameProfiler frameProfiler() {
        if (performanceManager == null) throw new IllegalStateException("Aura has not been initialized");
        return performanceManager.profiler();
    }

    private static void shutdown() {
        if (minecraft1201Adapter != null) {
            minecraft1201Adapter.detach();
            minecraft1201Adapter = null;
        }
        if (renderer != null) {
            renderer.shutdown();
            renderer = null;
        }
        performanceManager = null;
    }
}
