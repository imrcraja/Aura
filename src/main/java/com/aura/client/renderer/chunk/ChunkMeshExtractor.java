package com.aura.client.renderer.chunk;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ChunkMeshExtractor {
    private ChunkMeshExtractor() {}

    public static ChunkMeshData positions(float[] positions, int[] indices) {
        return new ChunkMeshData(positions, indices);
    }

    public static ChunkMeshData fromBuiltBuffer(BufferBuilder.BuiltBuffer builtBuffer) {
        if (builtBuffer == null || builtBuffer.isEmpty()) return null;
        BufferBuilder.DrawParameters parameters = builtBuffer.getParameters();
        VertexFormat format = parameters.format();
        int stride = format.getVertexSizeByte();
        int vertexCount = parameters.vertexCount();
        if (stride <= 0 || vertexCount <= 0) return null;

        int positionOffset = -1, colorOffset = -1, uv0Offset = -1, lightOffset = -1, normalOffset = -1;
        int offset = 0;
        for (VertexFormatElement element : format.getElements()) {
            switch (element.getType()) {
                case POSITION -> {
                    if (element.getComponentType() != VertexFormatElement.ComponentType.FLOAT || element.getComponentCount() != 3) return null;
                    positionOffset = offset;
                }
                case COLOR -> { if (element.getComponentCount() == 4) colorOffset = offset; }
                case UV -> {
                    if (element.getComponentCount() == 2) {
                        if (element.getUvIndex() == 0) uv0Offset = offset;
                        else if (element.getUvIndex() == 2) lightOffset = offset;
                    }
                }
                case NORMAL -> { if (element.getComponentCount() == 3) normalOffset = offset; }
                default -> {}
            }
            offset += element.getByteLength();
        }
        if (positionOffset < 0 || positionOffset + 12 > stride) return null;

        ByteBuffer source = builtBuffer.getVertexBuffer().duplicate().order(ByteOrder.nativeOrder());
        long required = (long) vertexCount * stride;
        if (required > source.remaining() || required > Integer.MAX_VALUE) return null;

        float[] vertices = new float[vertexCount * ChunkMeshData.FLOAT_STRIDE];
        int base = source.position();
        for (int v = 0; v < vertexCount; v++) {
            int so = base + v * stride, t = v * ChunkMeshData.FLOAT_STRIDE;
            vertices[t] = source.getFloat(so + positionOffset);
            vertices[t + 1] = source.getFloat(so + positionOffset + 4);
            vertices[t + 2] = source.getFloat(so + positionOffset + 8);

            vertices[t + 3] = vertices[t + 4] = vertices[t + 5] = vertices[t + 6] = 1.0f;
            if (colorOffset >= 0) {
                vertices[t + 3] = (source.get(so + colorOffset) & 0xFF) / 255.0f;
                vertices[t + 4] = (source.get(so + colorOffset + 1) & 0xFF) / 255.0f;
                vertices[t + 5] = (source.get(so + colorOffset + 2) & 0xFF) / 255.0f;
                vertices[t + 6] = (source.get(so + colorOffset + 3) & 0xFF) / 255.0f;
            }
            if (uv0Offset >= 0) {
                vertices[t + 7] = source.getShort(so + uv0Offset) / 32768.0f;
                vertices[t + 8] = source.getShort(so + uv0Offset + 2) / 32768.0f;
            }
            if (lightOffset >= 0) {
                vertices[t + 9] = (source.getShort(so + lightOffset) & 0xFFFF) / 65535.0f;
                vertices[t + 10] = (source.getShort(so + lightOffset + 2) & 0xFFFF) / 65535.0f;
            }
            vertices[t + 11] = vertices[t + 12] = 0.0f;
            vertices[t + 13] = 1.0f;
            if (normalOffset >= 0) {
                vertices[t + 11] = source.get(so + normalOffset) / 127.0f;
                vertices[t + 12] = source.get(so + normalOffset + 1) / 127.0f;
                vertices[t + 13] = source.get(so + normalOffset + 2) / 127.0f;
            }
        }

        int[] indices = triangleIndices(parameters.mode(), vertexCount);
        return indices.length == 0 ? null : new ChunkMeshData(vertices, indices, true);
    }

    private static int[] triangleIndices(VertexFormat.DrawMode mode, int vertexCount) {
        return switch (mode) {
            case TRIANGLES -> sequentialTriangles(vertexCount);
            case QUADS -> quadsToTriangles(vertexCount);
            case TRIANGLE_STRIP -> triangleStrip(vertexCount);
            case TRIANGLE_FAN -> triangleFan(vertexCount);
            default -> new int[0];
        };
    }

    private static int[] sequentialTriangles(int n) {
        int count = n - n % 3;
        int[] out = new int[count];
        for (int i = 0; i < count; i++) out[i] = i;
        return out;
    }

    private static int[] quadsToTriangles(int n) {
        int[] out = new int[(n / 4) * 6];
        int p = 0;
        for (int q = 0; q < n / 4; q++) {
            int b = q * 4;
            out[p++] = b; out[p++] = b + 1; out[p++] = b + 2;
            out[p++] = b; out[p++] = b + 2; out[p++] = b + 3;
        }
        return out;
    }

    private static int[] triangleStrip(int n) {
        if (n < 3) return new int[0];
        int[] out = new int[(n - 2) * 3];
        int p = 0;
        for (int i = 0; i < n - 2; i++) {
            if ((i & 1) == 0) { out[p++] = i; out[p++] = i + 1; out[p++] = i + 2; }
            else { out[p++] = i + 1; out[p++] = i; out[p++] = i + 2; }
        }
        return out;
    }

    private static int[] triangleFan(int n) {
        if (n < 3) return new int[0];
        int[] out = new int[(n - 2) * 3];
        int p = 0;
        for (int i = 1; i < n - 1; i++) { out[p++] = 0; out[p++] = i; out[p++] = i + 1; }
        return out;
    }
}
