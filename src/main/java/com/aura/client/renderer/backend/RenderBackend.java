package com.aura.client.renderer.backend;

/** Backend contract. Implementations must not depend on a specific Minecraft version. */
public interface RenderBackend {
    String id();
    void initialize();
    void shutdown();
    boolean isAvailable();
}
