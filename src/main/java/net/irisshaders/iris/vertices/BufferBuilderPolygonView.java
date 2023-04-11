package net.irisshaders.iris.vertices;

import java.nio.ByteBuffer;

public class BufferBuilderPolygonView implements QuadView {
	private ByteBuffer buffer;
	private int writePointer;
	private int stride = 48;
	private int vertexAmount;
	private boolean entity;

	public void setup(ByteBuffer buffer, int writePointer, int stride, int vertexAmount, boolean entity) {
		this.buffer = buffer;
		this.writePointer = writePointer;
		this.stride = stride;
		this.vertexAmount = vertexAmount;
		this.entity = entity;
	}

	@Override
	public float x(int index) {
		return buffer.getFloat(writePointer - stride * (vertexAmount - index));
	}

	@Override
	public float y(int index) {
		return buffer.getFloat(writePointer + 4 - stride * (vertexAmount - index));
	}

	@Override
	public float z(int index) {
		return buffer.getFloat(writePointer + 8 - stride * (vertexAmount - index));
	}

	@Override
	public float u(int index) {
		if (entity) {
			return decodeBlockTexture(buffer.getShort(writePointer + 16 - stride * (vertexAmount - index)));
		}
		return buffer.getFloat(writePointer + 16 - stride * (vertexAmount - index));
	}

	@Override
	public float v(int index) {
		if (entity) {
			return decodeBlockTexture(buffer.getShort(writePointer + 18 - stride * (vertexAmount - index)));
		}
		return buffer.getFloat(writePointer + 20 - stride * (vertexAmount - index));
	}

	private static final float TEXTURE_SCALE = (1.0f / 65536);

	private static float decodeBlockTexture(short raw) {
		return (raw & 0xFFFF) * TEXTURE_SCALE;
	}
}
