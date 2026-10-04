package com.aura.client.renderer.backend.opengl;

import com.aura.client.renderer.backend.RenderBackend;

/** OpenGL fallback. The actual Minecraft GL submission adapter is version-specific. */
public final class OpenGlBackend implements RenderBackend {
    @Override public String id() { return "opengl"; }
    @Override public void initialize() { }
    @Override public void shutdown() { }
    @Override public boolean isAvailable() { return true; }
}
