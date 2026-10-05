package com.aura.client.renderer.backend;

public interface RenderCommandList extends AutoCloseable {
    void begin();
    void draw(RenderMesh mesh);
    void end();

    @Override
    default void close() {
        end();
    }
}
