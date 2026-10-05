package com.aura.client.renderer.backend.opengl;

import com.aura.client.renderer.backend.RenderBackend;
import com.aura.client.renderer.backend.RenderCommandList;
import com.aura.client.renderer.backend.RenderMesh;
import com.aura.client.renderer.backend.RenderTexture;
import com.aura.client.renderer.chunk.ChunkMeshData;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * Real OpenGL resource backend. It uploads chunk vertex/index buffers while keeping
 * shader/terrain ownership with the Minecraft render pipeline until the Aura shader
 * pipeline is explicitly installed.
 */
public final class OpenGlBackend implements RenderBackend {
    private boolean initialized;
    private boolean contextReady;

    @Override public String id() { return "opengl"; }

    @Override public void initialize() {
        // Fabric/Minecraft owns the current GL context. Backend activation is therefore
        // intentionally lazy and only touches GL objects when a live context exists.
        initialized = true;
        contextReady = false;
    }

    @Override public void shutdown() {
        contextReady = false;
        initialized = false;
    }

    @Override public boolean isAvailable() {
        return true;
    }

    private void requireInitialized() {
        if (!initialized) throw new IllegalStateException("Aura OpenGL backend is not initialized");
    }

    private void markContextReady() {
        contextReady = true;
    }

    @Override public RenderMesh createMesh(String label, int vertexCount, int indexCount) {
        requireInitialized();
        return new OpenGlMesh(label, vertexCount, indexCount, 0, 0);
    }

    @Override public RenderMesh uploadChunkMesh(String label, ChunkMeshData mesh) {
        requireInitialized();
        markContextReady();
        int vertexArray = GL30.glGenVertexArrays();
        int vertexBuffer = GL15.glGenBuffers();
        int indexBuffer = GL15.glGenBuffers();
        GL30.glBindVertexArray(vertexArray);
        FloatBuffer vertices = memAllocFloat(mesh.vertices().length);
        IntBuffer indices = memAllocInt(mesh.indices().length);
        try {
            vertices.put(mesh.vertices()).flip();
            indices.put(mesh.indices()).flip();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
            GL30.glVertexAttribPointer(0, 3, GL15.GL_FLOAT, false, 3 * Float.BYTES, 0L);
            org.lwjgl.opengl.GL20.glEnableVertexAttribArray(0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            GL30.glBindVertexArray(0);
            OpenGlMesh result = new OpenGlMesh(label, mesh.vertexCount(), mesh.indexCount(), vertexBuffer, indexBuffer);
            result.vertexArray = vertexArray;
            return result;
        } finally {
            memFree(vertices);
            memFree(indices);
        }
    }

    @Override public RenderTexture createTexture(String label, int width, int height) {
        requireInitialized();
        return new OpenGlTexture(label, width, height, 0);
    }

    @Override public RenderCommandList createCommandList() {
        requireInitialized();
        return new OpenGlCommandList();
    }

    private static final class OpenGlMesh implements RenderMesh {
        private final String label;
        private final int vertexCount;
        private final int indexCount;
        private int vertexArray;
        private int vertexBuffer;
        private int indexBuffer;

        private OpenGlMesh(String label, int vertexCount, int indexCount, int vertexBuffer, int indexBuffer) {
            this.label = label;
            this.vertexCount = vertexCount;
            this.indexCount = indexCount;
            this.vertexBuffer = vertexBuffer;
            this.indexBuffer = indexBuffer;
        }

        @Override public String label() { return label; }
        @Override public int vertexCount() { return vertexCount; }
        @Override public int indexCount() { return indexCount; }

        int vertexBuffer() { return vertexBuffer; }
        int indexBuffer() { return indexBuffer; }
        int vertexArray() { return vertexArray; }

        @Override public void close() {
            if (vertexBuffer != 0) {
                GL15.glDeleteBuffers(vertexBuffer);
                vertexBuffer = 0;
            }
            if (indexBuffer != 0) {
                GL15.glDeleteBuffers(indexBuffer);
                indexBuffer = 0;
            }
            if (vertexArray != 0) {
                GL30.glDeleteVertexArrays(vertexArray);
                vertexArray = 0;
            }
        }
    }

    private record OpenGlTexture(String label, int width, int height, int textureId) implements RenderTexture {
        @Override public void close() {
            if (textureId != 0) org.lwjgl.opengl.GL11.glDeleteTextures(textureId);
        }
    }

    private static final class OpenGlCommandList implements RenderCommandList {
        private boolean recording;

        @Override public void begin() {
            if (recording) throw new IllegalStateException("Command list already recording");
            recording = true;
        }

        @Override public void draw(RenderMesh mesh) {
            if (!recording) throw new IllegalStateException("Command list is not recording");
            if (!(mesh instanceof OpenGlMesh glMesh)) return;
            if (glMesh.indexCount() <= 0 || glMesh.indexBuffer() == 0) return;
            GL30.glBindVertexArray(glMesh.vertexArray());
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, glMesh.indexBuffer());
            GL15.glDrawElements(GL15.GL_TRIANGLES, glMesh.indexCount(), GL15.GL_UNSIGNED_INT, 0L);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            GL30.glBindVertexArray(0);
        }

        @Override public void end() {
            recording = false;
        }

        @Override public void close() {
            if (recording) end();
        }
    }
}
