package com.aura.client.core;

import java.util.function.Supplier;

/** Prevents optional renderer work from taking down the Minecraft client. */
public final class AuraSafeExecutor {
    private AuraSafeExecutor() {}

    public static <T> T fallback(Supplier<T> action, Supplier<T> fallback) {
        try {
            return action.get();
        } catch (Throwable ignored) {
            return fallback.get();
        }
    }

    public static void run(Runnable action) {
        try {
            action.run();
        } catch (Throwable ignored) {
            // Optional renderer work must never become a hard client crash.
        }
    }
}
