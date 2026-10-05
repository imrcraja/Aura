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
 * OpenGL resource backend. It owns Aura-created GPU resources while Minecraft owns
 * the current GL context and vanilla shader state.
 */
public final class OpenGlBackend implements RenderBackend {
    private boolean initialized;
    private boolean contextReady;

    @Override public String id() { return "opengl"; }

    @Override public void initialize() {
        initialized = true;
        contextReady = false;
    }

    @Override public void shutdown() {
        contextReady = false;
        initialized = false;
    }

    @Override public boolean isAvailable() { return true; }

    private void requireInitialized() {
        if (!initialized) throw new IllegalStateException("Aura OpenGL backend is not initialized");
    }

    private void markContextReady() { contextReady = true; }

    @Override public RenderMesh createMesh(String label, int vertexCount, int indexCount) {
        requireInitialized();
        return new OpenGlMesh(label, vertexCount, indexCount, 0, 0, 0);
    }

    @Override public RenderMesh uploadChunkMesh(String label, ChunkMeshData mesh) {
        requireInitialized();
        if (mesh == null || mesh.vertexCount() == 0 || mesh.indexCount() == 0) {
            throw new IllegalArgumentException("chunk mesh must contain vertices and indices");
        }

        markContextReady();
        int vertexArray = 0;
        int vertexBuffer = 0;
        int indexBuffer = 0;
        FloatBuffer vertices = memAllocFloat(mesh.vertexCount() * mesh.vertexStrideFloats());
        IntBuffer indices = memAllocInt(mesh.indexCount());
        try {
            vertexArray = GL30.glGenVertexArrays();
            vertexBuffer = GL15.glGenBuffers();
            indexBuffer = GL15.glGenBuffers();

            vertices.put(mesh.vertices()).flip();
            indices.put(mesh.indices()).flip();

            GL30.glBindVertexArray(vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
            GL30.glVertexAttribPointer(0, mesh.vertexStrideFloats(), GL15.GL_FLOAT, false,
                    mesh.vertexStrideFloats() * Float.BYTES, 0L);
            org.lwjgl.opengl.GL20.glEnableVertexAttribArray(0);

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL30.glBindVertexArray(0);

            return new OpenGlMesh(label, mesh.vertexCount(), mesh.indexCount(),
                    vertexArray, vertexBuffer, indexBuffer);
        } catch (Throwable failure) {
            if (vertexBuffer != 0) GL15.glDeleteBuffers(vertexBuffer);
            if (indexBuffer != 0) GL15.glDeleteBuffers(indexBuffer);
            if (vertexArray != 0) GL30.glDeleteVertexArrays(vertexArray);
            throw failure;
        } finally {
            memFree(vertices);
            memFree(indices);
        }
    }

    @Override public RenderTexture createTexture(String label, int width, int height) {
        requireInitialized();
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("texture dimensions must be positive");
        markContextReady();
        int textureId = GL11Compat.genTexture();
        return new OpenGlTexture(label, width, height, textureId);
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

        private OpenGlMesh(String label, int vertexCount, int indexCount,
                           int vertexArray, int vertexBuffer, int indexBuffer) {
            this.label = label;
            this.vertexCount = vertexCount;
            this.indexCount = indexCount;
            this.vertexArray = vertexArray;
            this.vertexBuffer = vertexBuffer;
            this.indexBuffer = indexBuffer;
        }

        @Override public String label() { return label; }
        @Override public int vertexCount() { return vertexCount; }
        @Override public int indexCount() { return indexCount; }

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

    private static final class OpenGlTexture implements RenderTexture {
        private final String label;
        private final int width;
        private final int height;
        private int textureId;

        private OpenGlTexture(String label, int width, int height, int textureId) {
            this.label = label;
            this.width = width;
            this.height = height;
            this.textureId = textureId;
        }

        @Override public String label() { return label; }
        @Override public int width() { return width; }
        @Override public int height() { return height; }

        @Override public void close() {
            if (textureId != 0) {
                GL11Compat.deleteTexture(textureId);
                textureId = 0;
            }
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
            if (!(mesh instanceof OpenGlMesh glMesh) || glMesh.indexCount <= 0 || glMesh.indexBuffer == 0) return;
            GL30.glBindVertexArray(glMesh.vertexArray);
            GL15.glDrawElements(GL15.GL_TRIANGLES, glMesh.indexCount, GL15.GL_UNSIGNED_INT, 0L);
            GL30.glBindVertexArray(0);
        }

        @Override public void end() { recording = false; }

        @Override public void close() {
            if (recording) end();
        }
    }

    private static final class GL11Compat {
        private GL11Compat() {}
        static int genTexture() { return org.lwjgl.opengl.GL11.glGenTextures(); }
        static void deleteTexture(int id) { org.lwjgl.opengl.GL11.glDeleteTextures(id); }
    }
}
