package com.aura.client.renderer.chunk;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Decodes vanilla 1.20.1 chunk vertex buffers into Aura's canonical position-only mesh.
 *
 * This bridge preserves the vanilla draw mode by expanding supported triangle primitives
 * into an indexed triangle list. It never changes or cancels vanilla rendering.
 */
public final class ChunkMeshExtractor {
    private ChunkMeshExtractor() {}

    public static ChunkMeshData positions(float[] positions, int[] indices) {
        if (positions == null || indices == null) {
            throw new IllegalArgumentException("mesh arrays must be non-null");
        }
        if ((positions.length % 3) != 0) {
            throw new IllegalArgumentException("positions must contain XYZ triples");
        }
        return new ChunkMeshData(positions.clone(), indices.clone());
    }

    public static ChunkMeshData fromBuiltBuffer(BufferBuilder.BuiltBuffer builtBuffer) {
        if (builtBuffer == null || builtBuffer.isEmpty()) return null;

        BufferBuilder.DrawParameters parameters = builtBuffer.getParameters();
        VertexFormat format = parameters.format();
        int stride = format.getVertexSizeByte();
        int vertexCount = parameters.vertexCount();
        if (stride <= 0 || vertexCount <= 0) return null;

        int positionOffset = 0;
        boolean foundPosition = false;
        for (VertexFormatElement element : format.getElements()) {
            if (element.isPosition()) {
                if (element.getComponentType() != VertexFormatElement.ComponentType.FLOAT
                        || element.getComponentCount() != 3) {
                    return null;
                }
                foundPosition = true;
                break;
            }
            positionOffset += element.getByteLength();
        }
        if (!foundPosition || positionOffset + 3 * Float.BYTES > stride) return null;

        ByteBuffer source = builtBuffer.getVertexBuffer().duplicate().order(ByteOrder.nativeOrder());
        long requiredLong = (long) vertexCount * stride;
        if (requiredLong > source.remaining() || requiredLong > Integer.MAX_VALUE) return null;

        float[] positions = new float[vertexCount * 3];
        int base = source.position();
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            int offset = base + vertex * stride + positionOffset;
            positions[vertex * 3] = source.getFloat(offset);
            positions[vertex * 3 + 1] = source.getFloat(offset + Float.BYTES);
            positions[vertex * 3 + 2] = source.getFloat(offset + Float.BYTES * 2);
        }

        int[] indices = triangleIndices(parameters.mode(), vertexCount);
        if (indices.length == 0) return null;
        return positions(positions, indices);
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

    private static int[] sequentialTriangles(int vertexCount) {
        int count = vertexCount - (vertexCount % 3);
        int[] indices = new int[count];
        for (int i = 0; i < count; i++) indices[i] = i;
        return indices;
    }

    private static int[] quadsToTriangles(int vertexCount) {
        int quadCount = vertexCount / 4;
        int[] indices = new int[quadCount * 6];
        int out = 0;
        for (int quad = 0; quad < quadCount; quad++) {
            int base = quad * 4;
            indices[out++] = base;
            indices[out++] = base + 1;
            indices[out++] = base + 2;
            indices[out++] = base;
            indices[out++] = base + 2;
            indices[out++] = base + 3;
        }
        return indices;
    }

    private static int[] triangleStrip(int vertexCount) {
        if (vertexCount < 3) return new int[0];
        int[] indices = new int[(vertexCount - 2) * 3];
        int out = 0;
        for (int i = 0; i < vertexCount - 2; i++) {
            if ((i & 1) == 0) {
                indices[out++] = i;
                indices[out++] = i + 1;
                indices[out++] = i + 2;
            } else {
                indices[out++] = i + 1;
                indices[out++] = i;
                indices[out++] = i + 2;
            }
        }
        return indices;
    }

    private static int[] triangleFan(int vertexCount) {
        if (vertexCount < 3) return new int[0];
        int[] indices = new int[(vertexCount - 2) * 3];
        int out = 0;
        for (int i = 1; i < vertexCount - 1; i++) {
            indices[out++] = 0;
            indices[out++] = i;
            indices[out++] = i + 1;
        }
        return indices;
    }
}
