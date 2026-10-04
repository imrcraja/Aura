package com.aura.client.renderer.compat;

/** Adapter between a Minecraft release's rendering internals and the stable Aura renderer API. */
public interface VersionAdapter {
    String minecraftVersion();
    void attach();
    void detach();
}
