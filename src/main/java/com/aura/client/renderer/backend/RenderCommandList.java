package com.aura.client.renderer.backend;

public interface RenderCommandList extends AutoCloseable {
    void begin();
    void draw(RenderMesh mesh);

    /**
     * Draws a mesh with a world-space chunk offset. Backends that do not need an
     * explicit offset may safely fall back to draw(mesh).
     */
    default void draw(RenderMesh mesh, double offsetX, double offsetY, double offsetZ) {
        draw(mesh);
    }

    void end();

    @Override
    default void close() {
        end();
    }
}
