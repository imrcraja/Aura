package com.aura.client.renderer.backend.opengl;

import com.aura.client.renderer.backend.RenderBackend;
import com.aura.client.renderer.backend.RenderCommandList;
import com.aura.client.renderer.backend.RenderMesh;
import com.aura.client.renderer.backend.RenderTexture;

public final class OpenGlBackend implements RenderBackend {
    private boolean initialized;

    @Override public String id() { return "opengl"; }

    @Override public void initialize() { initialized = true; }

    @Override public void shutdown() { initialized = false; }

    @Override public boolean isAvailable() { return true; }

    private void requireInitialized() {
        if (!initialized) throw new IllegalStateException("Aura OpenGL backend is not initialized");
    }

    @Override public RenderMesh createMesh(String label, int vertexCount, int indexCount) {
        requireInitialized();
        return new StubMesh(label, vertexCount, indexCount);
    }

    @Override public RenderTexture createTexture(String label, int width, int height) {
        requireInitialized();
        return new StubTexture(label, width, height);
    }

    @Override public RenderCommandList createCommandList() {
        requireInitialized();
        return new StubCommandList();
    }

    private record StubMesh(String label, int vertexCount, int indexCount) implements RenderMesh {
        @Override public void close() { }
    }

    private record StubTexture(String label, int width, int height) implements RenderTexture {
        @Override public void close() { }
    }

    private static final class StubCommandList implements RenderCommandList {
        private boolean recording;
        @Override public void begin() { recording = true; }
        @Override public void draw(RenderMesh mesh) {
            if (!recording) throw new IllegalStateException("Command list is not recording");
        }
        @Override public void end() { recording = false; }
    }
}
