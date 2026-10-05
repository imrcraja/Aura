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

public final class OpenGlBackend implements RenderBackend {
    private boolean initialized;
    private boolean contextReady;

    @Override public String id() { return "opengl"; }
    @Override public void initialize() { initialized = true; contextReady = false; }
    @Override public void shutdown() { contextReady = false; initialized = false; }
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
        if (mesh == null || mesh.vertexCount() == 0 || mesh.indexCount() == 0) throw new IllegalArgumentException("chunk mesh must contain vertices and indices");
        markContextReady();

        int vao = 0, vbo = 0, ebo = 0;
        FloatBuffer vertices = memAllocFloat(mesh.vertexCount() * mesh.vertexStrideFloats());
        IntBuffer indices = memAllocInt(mesh.indexCount());
        try {
            vao = GL30.glGenVertexArrays();
            vbo = GL15.glGenBuffers();
            ebo = GL15.glGenBuffers();
            vertices.put(mesh.vertices()).flip();
            indices.put(mesh.indices()).flip();

            GL30.glBindVertexArray(vao);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);

            int stride = mesh.vertexStrideFloats() * Float.BYTES;
            org.lwjgl.opengl.GL20.glVertexAttribPointer(0, 3, GL15.GL_FLOAT, false, stride, 0L);
            org.lwjgl.opengl.GL20.glEnableVertexAttribArray(0);
            org.lwjgl.opengl.GL20.glVertexAttribPointer(1, 4, GL15.GL_FLOAT, false, stride, 3L * Float.BYTES);
            org.lwjgl.opengl.GL20.glEnableVertexAttribArray(1);
            org.lwjgl.opengl.GL20.glVertexAttribPointer(2, 2, GL15.GL_FLOAT, false, stride, 7L * Float.BYTES);
            org.lwjgl.opengl.GL20.glEnableVertexAttribArray(2);
            org.lwjgl.opengl.GL20.glVertexAttribPointer(3, 2, GL15.GL_FLOAT, false, stride, 9L * Float.BYTES);
            org.lwjgl.opengl.GL20.glEnableVertexAttribArray(3);
            org.lwjgl.opengl.GL20.glVertexAttribPointer(4, 3, GL15.GL_FLOAT, false, stride, 11L * Float.BYTES);
            org.lwjgl.opengl.GL20.glEnableVertexAttribArray(4);

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL30.glBindVertexArray(0);

            return new OpenGlMesh(label, mesh.vertexCount(), mesh.indexCount(), vao, vbo, ebo);
        } catch (Throwable failure) {
            if (vbo != 0) GL15.glDeleteBuffers(vbo);
            if (ebo != 0) GL15.glDeleteBuffers(ebo);
            if (vao != 0) GL30.glDeleteVertexArrays(vao);
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
        return new OpenGlTexture(label, width, height, org.lwjgl.opengl.GL11.glGenTextures());
    }

    @Override public RenderCommandList createCommandList() {
        requireInitialized();
        return new OpenGlCommandList();
    }

    private static final class OpenGlMesh implements RenderMesh {
        private final String label;
        private final int vertexCount, indexCount;
        private int vao, vbo, ebo;

        private OpenGlMesh(String label, int vertexCount, int indexCount, int vao, int vbo, int ebo) {
            this.label = label; this.vertexCount = vertexCount; this.indexCount = indexCount;
            this.vao = vao; this.vbo = vbo; this.ebo = ebo;
        }

        @Override public String label() { return label; }
        @Override public int vertexCount() { return vertexCount; }
        @Override public int indexCount() { return indexCount; }
        @Override public long estimatedBytes() {
            return (long) vertexCount * ChunkMeshData.FLOAT_STRIDE * Float.BYTES + (long) indexCount * Integer.BYTES;
        }

        @Override public void close() {
            if (vbo != 0) { GL15.glDeleteBuffers(vbo); vbo = 0; }
            if (ebo != 0) { GL15.glDeleteBuffers(ebo); ebo = 0; }
            if (vao != 0) { GL30.glDeleteVertexArrays(vao); vao = 0; }
        }
    }

    private static final class OpenGlTexture implements RenderTexture {
        private final String label;
        private final int width, height;
        private int textureId;

        private OpenGlTexture(String label, int width, int height, int textureId) {
            this.label = label; this.width = width; this.height = height; this.textureId = textureId;
        }

        @Override public String label() { return label; }
        @Override public int width() { return width; }
        @Override public int height() { return height; }
        @Override public void close() {
            if (textureId != 0) { org.lwjgl.opengl.GL11.glDeleteTextures(textureId); textureId = 0; }
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
            if (!(mesh instanceof OpenGlMesh glMesh) || glMesh.indexCount <= 0 || glMesh.vao == 0) return;
            GL30.glBindVertexArray(glMesh.vao);
            GL15.glDrawElements(GL15.GL_TRIANGLES, glMesh.indexCount, GL15.GL_UNSIGNED_INT, 0L);
            GL30.glBindVertexArray(0);
        }
        @Override public void end() { recording = false; }
        @Override public void close() { if (recording) end(); }
    }
}
