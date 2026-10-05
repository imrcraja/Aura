package com.aura.client.core;

/** Bounded adaptive cache policy; never attempts to consume the entire JVM heap. */
public final class AuraAdaptiveCache {
    private AuraAdaptiveCache() {}

    public static long budgetBytes(AuraDeviceProfile profile) {
        long heap = Math.max(32L * 1024 * 1024, profile.maxMemoryBytes());
        long target = heap / (profile.android() ? 5 : 4);
        long cap = profile.android() ? 512L * 1024 * 1024 : 1024L * 1024 * 1024;
        return Math.max(32L * 1024 * 1024, Math.min(target, cap));
    }

    public static int gpuEntryBudget(AuraDeviceProfile profile) {
        long mb = budgetBytes(profile) / (1024L * 1024L);
        return (int) Math.max(64, Math.min(4096, mb * 2));
    }
}
