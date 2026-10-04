package com.aura.client.core;

import java.util.Arrays;

/**
 * Lightweight render-frame telemetry. It does not change vanilla rendering;
 * it gives the renderer core stable timing data for adaptive decisions.
 */
public final class AuraFrameProfiler {
    private static final int WINDOW = 120;
    private final long[] frameTimesNanos = new long[WINDOW];
    private int cursor;
    private int samples;
    private long frameStart;
    private long lastFrameNanos;

    public void beginFrame() {
        frameStart = System.nanoTime();
    }

    public void endFrame() {
        if (frameStart == 0L) return;
        lastFrameNanos = Math.max(0L, System.nanoTime() - frameStart);
        frameTimesNanos[cursor] = lastFrameNanos;
        cursor = (cursor + 1) % WINDOW;
        samples = Math.min(WINDOW, samples + 1);
    }

    public long lastFrameNanos() {
        return lastFrameNanos;
    }

    public double averageFrameMillis() {
        if (samples == 0) return 0.0;
        long total = 0L;
        for (int i = 0; i < samples; i++) total += frameTimesNanos[i];
        return total / (double) samples / 1_000_000.0;
    }

    public double estimatedFps() {
        double millis = averageFrameMillis();
        return millis <= 0.0 ? 0.0 : 1000.0 / millis;
    }

    public int samples() {
        return samples;
    }

    public void reset() {
        Arrays.fill(frameTimesNanos, 0L);
        cursor = 0;
        samples = 0;
        frameStart = 0L;
        lastFrameNanos = 0L;
    }
}
