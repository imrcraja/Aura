package com.aura.client.renderer.compat.v1201;

import com.aura.client.renderer.compat.VersionAdapter;

/** Minecraft 1.20.1 adapter boundary. Renderer internals stay outside the common core. */
public final class Minecraft1201Adapter implements VersionAdapter {
    @Override public String minecraftVersion() { return "1.20.1"; }
    @Override public void attach() { }
    @Override public void detach() { }
}
