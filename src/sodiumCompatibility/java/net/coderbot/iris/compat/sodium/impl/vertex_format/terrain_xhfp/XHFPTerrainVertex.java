package net.coderbot.iris.compat.sodium.impl.vertex_format.terrain_xhfp;

import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.coderbot.iris.compat.sodium.impl.block_context.BlockContextHolder;
import net.coderbot.iris.compat.sodium.impl.block_context.ContextAwareVertexWriter;
import org.joml.Vector3f;
import net.coderbot.iris.vertices.ExtendedDataHelper;
import net.coderbot.iris.vertices.NormalHelper;
import org.lwjgl.system.MemoryUtil;

import static net.coderbot.iris.compat.sodium.impl.vertex_format.terrain_xhfp.XHFPModelVertexType.STRIDE;

public class XHFPTerrainVertex implements ChunkVertexEncoder, ContextAwareVertexWriter {
	private BlockContextHolder contextHolder;

	private float midU;
	private float midV;
	private int normal;
	private int tangent;

	@Override
	public void iris$setContextHolder(BlockContextHolder holder) {
		this.contextHolder = holder;
	}

	@Override
	public void iris$setQuadInfo(int normal, int tangent, float midTexCoordX, float midTexCoordY) {
		this.normal = normal;
		this.tangent = tangent;
		this.midU = midTexCoordX;
		this.midV = midTexCoordY;
	}

	@Override
	public long write(long ptr, Material material,
					  Vertex vertex, int chunkId) {
		MemoryUtil.memPutShort(ptr + 0, XHFPModelVertexType.encodePosition(vertex.x));
		MemoryUtil.memPutShort(ptr + 2, XHFPModelVertexType.encodePosition(vertex.y));
		MemoryUtil.memPutShort(ptr + 4, XHFPModelVertexType.encodePosition(vertex.z));
		MemoryUtil.memPutByte(ptr + 6, material.bits());
		MemoryUtil.memPutByte(ptr + 7, (byte) chunkId);

		MemoryUtil.memPutInt(ptr + 8, vertex.color);

		MemoryUtil.memPutShort(ptr + 12, XHFPModelVertexType.encodeBlockTexture(vertex.u));
		MemoryUtil.memPutShort(ptr + 14, XHFPModelVertexType.encodeBlockTexture(vertex.v));

		MemoryUtil.memPutInt(ptr + 16, vertex.light);
		MemoryUtil.memPutFloat(ptr + 20, midU);
		MemoryUtil.memPutFloat(ptr + 24, midV);

		MemoryUtil.memPutInt(ptr + 28, tangent);
		MemoryUtil.memPutInt(ptr + 32, normal);

		MemoryUtil.memPutShort(ptr + 36, contextHolder.blockId);
		MemoryUtil.memPutShort(ptr + 38, contextHolder.renderType);
		MemoryUtil.memPutInt(ptr + 40, ExtendedDataHelper.computeMidBlock(vertex.x, vertex.y, vertex.z, contextHolder.localPosX, contextHolder.localPosY, contextHolder.localPosZ));

		return ptr + STRIDE;
	}
}
