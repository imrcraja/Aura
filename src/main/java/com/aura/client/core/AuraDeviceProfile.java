package com.aura.client.core;

/** Device capability snapshot used by the adaptive performance layer. */
public record AuraDeviceProfile(long maxMemoryBytes, int availableProcessors, boolean android) {
    public static AuraDeviceProfile detect() {
        Runtime runtime = Runtime.getRuntime();
        boolean android = System.getProperty("java.runtime.name", "").toLowerCase().contains("android")
                || System.getProperty("os.name", "").toLowerCase().contains("android");
        return new AuraDeviceProfile(runtime.maxMemory(), runtime.availableProcessors(), android);
    }

    /** Conservative cache budget: use available heap without trying to consume all of it. */
    public long recommendedCacheBytes() {
        long quarter = maxMemoryBytes / 4;
        return Math.max(32L * 1024 * 1024, Math.min(quarter, 768L * 1024 * 1024));
    }
}
