package com.aura.client.renderer.backend;

public interface RenderResource extends AutoCloseable {
    String label();
    @Override void close();
}
