package net.coderbot.iris.compat.sodium.impl.vertex_format.terrain_xhfp;

import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorU8;
import net.coderbot.iris.compat.sodium.impl.block_context.BlockContextHolder;
import net.coderbot.iris.compat.sodium.impl.block_context.ContextAwareVertexWriter;
import net.coderbot.iris.vertices.OctahedralCalculator;
import net.coderbot.iris.vertices.QuadView;
import org.joml.Vector3f;
import net.coderbot.iris.vertices.ExtendedDataHelper;
import org.lwjgl.system.MemoryUtil;

import static net.coderbot.iris.compat.sodium.impl.vertex_format.terrain_xhfp.XHFPModelVertexType.STRIDE;
import static net.coderbot.iris.compat.sodium.impl.vertex_format.terrain_xhfp.XHFPModelVertexType.encodeColor;
import static net.coderbot.iris.compat.sodium.impl.vertex_format.terrain_xhfp.XHFPModelVertexType.encodeDrawParameters;
import static net.coderbot.iris.compat.sodium.impl.vertex_format.terrain_xhfp.XHFPModelVertexType.encodeLight;
import static net.coderbot.iris.compat.sodium.impl.vertex_format.terrain_xhfp.XHFPModelVertexType.encodePosition;
import static net.coderbot.iris.compat.sodium.impl.vertex_format.terrain_xhfp.XHFPModelVertexType.encodeTexture;

public class XHFPTerrainVertex implements ChunkVertexEncoder, ContextAwareVertexWriter, QuadView {
	private final Vector3f normal = new Vector3f();

	private BlockContextHolder contextHolder;

	private int vertexCount;
	private float uSum;
	private float vSum;
	private boolean flipUpcomingNormal;
	private Vertex[] vertices = new Vertex[4];

	// TODO: FIX

	/*@Override
	public void copyQuadAndFlipNormal() {
		ensureCapacity(4);

		MemoryUtil.memCopy(this.writePointer - STRIDE * 4, this.writePointer, STRIDE * 4);

		// Now flip vertex normals
		int packedNormal = MemoryUtil.memGetInt(this.writePointer + 32);
		int inverted = NormalHelper.invertPackedNormal(packedNormal);

		MemoryUtil.memPutInt(this.writePointer + 32, inverted);
		MemoryUtil.memPutInt(this.writePointer + 32 + STRIDE, inverted);
		MemoryUtil.memPutInt(this.writePointer + 32 + STRIDE * 2, inverted);
		MemoryUtil.memPutInt(this.writePointer + 32 + STRIDE * 3, inverted);

		// We just wrote 4 vertices, advance by 4
		for (int i = 0; i < 4; i++) {
			this.advance();
		}

		// Ensure vertices are flushed
		this.flush();
	}*/

	private long writePointer;
	private OctahedralCalculator encoder = new OctahedralCalculator();

	@Override
	public void iris$setContextHolder(BlockContextHolder holder) {
		this.contextHolder = holder;
	}

	@Override
	public void flipUpcomingQuadNormal() {
		flipUpcomingNormal = true;
	}
	static float bias = 1.0f / (2^(8 - 1) - 1);

	@Override
	public long write(long ptr,
					  Material material, Vertex vertex, int sectionIndex) {
		uSum += vertex.u;
		vSum += vertex.v;

		vertices[vertexCount] = vertex;

		this.writePointer = ptr;

		vertexCount++;

		MemoryUtil.memPutInt(ptr + 0, (encodePosition(vertex.x) << 0) | (encodePosition(vertex.y) << 16));
		MemoryUtil.memPutInt(ptr + 4, (encodePosition(vertex.z) << 0) | (encodeDrawParameters(material, sectionIndex) << 16));
		MemoryUtil.memPutInt(ptr + 8, (encodeColor(vertex.color) << 0) | (encodeLight(vertex.light) << 24));
		MemoryUtil.memPutInt(ptr + 12, (encodeTexture(vertex.u) << 0) | (encodeTexture(vertex.v) << 16));

		MemoryUtil.memPutShort(ptr + 24, contextHolder.blockId);
		MemoryUtil.memPutShort(ptr + 26, contextHolder.renderType);
		var brightness = ColorU8.byteToNormalizedFloat(ColorABGR.unpackAlpha(vertex.color));

		MemoryUtil.memPutInt(ptr + 28, contextHolder.ignoreMidBlock ? 0 : ExtendedDataHelper.computeMidBlock(vertex.x, vertex.y, vertex.z, contextHolder.localPosX, contextHolder.localPosY, contextHolder.localPosZ, brightness));

		if (vertexCount == 4) {
			vertexCount = 0;

			// FIXME
			// The following logic is incorrect because OpenGL denormalizes shorts by dividing by 65535. The atlas is
			// based on power-of-two values and so a normalization factor that is not a power of two causes the values
			// used in the shader to be off by enough to cause visual errors. These are most noticeable on 1.18 with POM
			// on block edges.
			//
			// The only reliable way that this can be fixed is to apply the same shader transformations to midTexCoord
			// as Sodium does to the regular texture coordinates - dividing them by the correct power-of-two value inside
			// of the shader instead of letting OpenGL value normalization do the division. However, this requires
			// fragile patching that is not yet possible.
			//
			// As a temporary solution, the normalized shorts have been replaced with regular floats, but this takes up
			// an extra 4 	bytes per vertex.

			// NB: Be careful with the math here! A previous bug was caused by midU going negative as a short, which
			// was sign-extended into midTexCoord, causing midV to have garbage (likely NaN data). If you're touching
			// this code, be aware of that, and don't introduce those kinds of bugs!
			//
			// Also note that OpenGL takes shorts in the range of [0, 65535] and transforms them linearly to [0.0, 1.0],
			// so multiply by 65535, not 65536.
			//
			// TODO: Does this introduce precision issues? Do we need to fall back to floats here? This might break
			// with high resolution texture packs.
//			int midU = (int)(65535.0F * Math.min(uSum * 0.25f, 1.0f)) & 0xFFFF;
//			int midV = (int)(65535.0F * Math.min(vSum * 0.25f, 1.0f)) & 0xFFFF;
//			int midTexCoord = (midV << 16) | midU;

			uSum *= 0.25f;
			vSum *= 0.25f;

			int midU = XHFPModelVertexType.encodeTexture(uSum);
			int midV = XHFPModelVertexType.encodeTexture(vSum);

			MemoryUtil.memPutInt(ptr + 16, (midU << 0) | (midV << 16));
			MemoryUtil.memPutInt(ptr + 16 - STRIDE, (midU << 0) | (midV << 16));
			MemoryUtil.memPutInt(ptr + 16 - STRIDE * 2, (midU << 0) | (midV << 16));
			MemoryUtil.memPutInt(ptr + 16 - STRIDE * 3, (midU << 0) | (midV << 16));

			uSum = 0;
			vSum = 0;

			// normal computation
			// Implementation based on the algorithm found here:
			// https://github.com/IrisShaders/ShaderDoc/blob/master/vertex-format-extensions.md#surface-normal-vector

			int normTangent = encoder.calculateOctahedralEncode(flipUpcomingNormal, this);

			MemoryUtil.memPutInt(ptr + 20, normTangent);
			MemoryUtil.memPutInt(ptr + 20 - STRIDE, normTangent);
			MemoryUtil.memPutInt(ptr + 20 - STRIDE * 2, normTangent);
			MemoryUtil.memPutInt(ptr + 20 - STRIDE * 3, normTangent);

			flipUpcomingNormal = false;
		}

		return ptr + STRIDE;
	}

	@Override
	public float x(int index) {
		return vertices[index].x;
	}

	@Override
	public float y(int index) {
		return vertices[index].y;
	}

	@Override
	public float z(int index) {
		return vertices[index].z;
	}

	@Override
	public float u(int index) {
		return vertices[index].u;
	}

	@Override
	public float v(int index) {
		return vertices[index].v;
	}
}
