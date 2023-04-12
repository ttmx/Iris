package net.irisshaders.iris.compat.sodium.impl.vertex_format.entity_xhfp;

import com.mojang.blaze3d.vertex.PoseStack;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.RenderGlobal;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatRegistry;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.common.util.MatrixHelper;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp.XHFPModelVertexType;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.irisshaders.iris.vertices.NormalHelper;
import net.minecraft.util.Mth;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class EntityVertex {
	public static final VertexFormatDescription FORMAT = VertexFormatRegistry.get(IrisVertexFormats.ENTITY);
	public static final int STRIDE = IrisVertexFormats.ENTITY.getVertexSize();

	private static final int OFFSET_POSITION = 0;
	private static final int OFFSET_COLOR = 12;
	private static final int OFFSET_TEXTURE = 16;
	private static final int OFFSET_MID_TEXTURE = 38;
	private static final int OFFSET_OVERLAY = 20;
	private static final int OFFSET_LIGHT = 24;
	private static final int OFFSET_NORMAL = 28;

	private static final Vector3f lastNormal = new Vector3f();
	private static final QuadViewEntity.QuadViewEntityUnsafe quadView = new QuadViewEntity.QuadViewEntityUnsafe();

	public static void write(long ptr,
							 float x, float y, float z, int color, float u, float v, float midU, float midV, int light, int overlay, byte normalX, byte normalY, byte tangentX, byte tangentY) {
		MemoryUtil.memPutFloat(ptr + OFFSET_POSITION, x);
		MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 4, y);
		MemoryUtil.memPutFloat(ptr + OFFSET_POSITION + 8, z);

		MemoryUtil.memPutInt(ptr + OFFSET_COLOR, color);

		MemoryUtil.memPutShort(ptr + OFFSET_TEXTURE, XHFPModelVertexType.encodeBlockTexture(u));
		MemoryUtil.memPutShort(ptr + OFFSET_TEXTURE + 2, XHFPModelVertexType.encodeBlockTexture(v));

		MemoryUtil.memPutInt(ptr + OFFSET_LIGHT, light);

		MemoryUtil.memPutInt(ptr + OFFSET_OVERLAY, overlay);

		MemoryUtil.memPutByte(ptr + OFFSET_NORMAL, normalX);
		MemoryUtil.memPutByte(ptr + OFFSET_NORMAL + 1, normalY);
		MemoryUtil.memPutByte(ptr + OFFSET_NORMAL + 2, tangentX);
		MemoryUtil.memPutByte(ptr + OFFSET_NORMAL + 3, tangentY);

		MemoryUtil.memPutShort(ptr + OFFSET_MID_TEXTURE, XHFPModelVertexType.encodeBlockTexture(midU));
		MemoryUtil.memPutShort(ptr + OFFSET_MID_TEXTURE + 2, XHFPModelVertexType.encodeBlockTexture(midV));

		MemoryUtil.memPutShort(ptr + 32, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedEntity());
		MemoryUtil.memPutShort(ptr + 34, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity());
		MemoryUtil.memPutShort(ptr + 36, (short) CapturedRenderingState.INSTANCE.getCurrentRenderedItem());

	}

	public static void writeQuadVertices(VertexBufferWriter writer, PoseStack.Pose matrices, ModelQuadView quad, int light, int overlay, int color) {
		Matrix3f matNormal = matrices.normal();
		Matrix4f matPosition = matrices.pose();

		try (MemoryStack stack = RenderGlobal.VERTEX_DATA.push()) {
			long buffer = stack.nmalloc(4 * STRIDE);
			long ptr = buffer;

			// The packed normal vector
			var n = quad.getNormal();

			// The normal vector
			float nx = Norm3b.unpackX(n);
			float ny = Norm3b.unpackY(n);
			float nz = Norm3b.unpackZ(n);

			float midU = ((quad.getTexU(0) + quad.getTexU(1) + quad.getTexU(2) + quad.getTexU(3)) * 0.25f);
			float midV = ((quad.getTexV(0) + quad.getTexV(1) + quad.getTexV(2) + quad.getTexV(3)) * 0.25f);

			// The transformed normal vector
			float nxt = MatrixHelper.transformNormalX(matNormal, nx, ny, nz);
			float nyt = MatrixHelper.transformNormalY(matNormal, nx, ny, nz);
			float nzt = MatrixHelper.transformNormalZ(matNormal, nx, ny, nz);

			// The packed transformed normal vector
			var nt = Norm3b.pack(nxt, nyt, nzt);

			getTangent(nt, quad.getX(0), quad.getY(0), quad.getZ(0), quad.getTexU(0), quad.getTexV(0),
				quad.getX(1), quad.getY(1), quad.getZ(1), quad.getTexU(1), quad.getTexV(1),
				quad.getX(2), quad.getY(2), quad.getZ(2), quad.getTexU(2), quad.getTexV(2));

			for (int i = 0; i < 4; i++) {
				// The position vector
				float x = quad.getX(i);
				float y = quad.getY(i);
				float z = quad.getZ(i);

				// The transformed position vector
				float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
				float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
				float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);

				write(ptr, xt, yt, zt, color, quad.getTexU(i), quad.getTexV(i), midU, midV, light, overlay, normalConvX, normalConvY, tangentX, tangentY);
				ptr += STRIDE;
			}

			endQuad(ptr - STRIDE, nxt, nyt, nzt);

			writer.push(stack, buffer, 4, FORMAT);
		}
	}

	private static byte normalConvX, normalConvY, tangentX, tangentY;

	private static void vec_to_oct(int value)
	{
		float valueX = Norm3b.unpackX(value);
		float valueY = Norm3b.unpackY(value);
		float valueZ = Norm3b.unpackZ(value);

		float invL1Norm = 1.0f / (Math.abs(valueX) + Math.abs(valueY) + Math.abs(valueZ));
		float resX = 0.0f, resY = 0.0f;
		if (valueZ < 0.0f) {
			resX = (1.0f - Math.abs(valueY * invL1Norm)) * Mth.sign(valueX);
			resY = (1.0f - Math.abs(valueX * invL1Norm)) * Mth.sign(valueY);
		} else {
			resX = valueX * invL1Norm;
			resY = valueY * invL1Norm;
		}

		normalConvX = floatToSnorm8(resX);
		normalConvY = floatToSnorm8(resY);
	}
	private static float bias = 1.0f / (2^(8 - 1) - 1);

	private static byte floatToSnorm8( float v )
	{
		//According to D3D10 rules, the value "-1.0f" has two representations:
		//  0x1000 and 0x10001
		//This allows everyone to convert by just multiplying by 32767 instead
		//of multiplying the negative values by 32768 and 32767 for positive.
		return (byte) Math.clamp( v >= 0.0f ?
				(v * 127.0f + 0.5f) :
				(v * 127.0f - 0.5f),
			-128.0f,
			127.0f );
	}


	private static void vec_to_tangent(float valueX, float valueY, float valueZ, float valueW) {
		//encode to octahedron, result in range [-1, 1]
		float invL1Norm = 1.0f / (Math.abs(valueX) + Math.abs(valueY) + Math.abs(valueZ));
		float resX = 0.0f, resY = 0.0f;
		if (valueZ < 0.0f) {
			resX = (1.0f - Math.abs(valueY * invL1Norm)) * Mth.sign(valueX);
			resY = (1.0f - Math.abs(valueX * invL1Norm)) * Mth.sign(valueY);
		} else {
			resX = valueX * invL1Norm;
			resY = valueY * invL1Norm;
		}

		// map y to always be positive
		resY = resY * 0.5f + 0.5f;

		// add a bias so that y is never 0 (sign in the vertex shader)
		if (resY < bias)
			resY = bias;

		// Apply the sign of the binormal to y, which was computed elsewhere
		if (valueW < 0)
			resY *= -1;

		tangentX = floatToSnorm8(resX);
		tangentY = floatToSnorm8(resY);
	}


	private static int getTangent(int normal, float x0, float y0, float z0, float u0, float v0, float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2, float v2) {
		// Capture all of the relevant vertex positions

		float normalX = Norm3b.unpackX(normal);
		float normalY = Norm3b.unpackY(normal);
		float normalZ = Norm3b.unpackZ(normal);

		float edge1x = x1 - x0;
		float edge1y = y1 - y0;
		float edge1z = z1 - z0;

		float edge2x = x2 - x0;
		float edge2y = y2 - y0;
		float edge2z = z2 - z0;

		float deltaU1 = u1 - u0;
		float deltaV1 = v1 - v0;
		float deltaU2 = u2 - u0;
		float deltaV2 = v2 - v0;

		float fdenom = deltaU1 * deltaV2 - deltaU2 * deltaV1;
		float f;

		if (fdenom == 0.0) {
			f = 1.0f;
		} else {
			f = 1.0f / fdenom;
		}

		float tangentx = f * (deltaV2 * edge1x - deltaV1 * edge2x);
		float tangenty = f * (deltaV2 * edge1y - deltaV1 * edge2y);
		float tangentz = f * (deltaV2 * edge1z - deltaV1 * edge2z);
		float tcoeff = rsqrt(tangentx * tangentx + tangenty * tangenty + tangentz * tangentz);
		tangentx *= tcoeff;
		tangenty *= tcoeff;
		tangentz *= tcoeff;

		float bitangentx = f * (-deltaU2 * edge1x + deltaU1 * edge2x);
		float bitangenty = f * (-deltaU2 * edge1y + deltaU1 * edge2y);
		float bitangentz = f * (-deltaU2 * edge1z + deltaU1 * edge2z);
		float bitcoeff = rsqrt(bitangentx * bitangentx + bitangenty * bitangenty + bitangentz * bitangentz);
		bitangentx *= bitcoeff;
		bitangenty *= bitcoeff;
		bitangentz *= bitcoeff;

		// predicted bitangent = tangent × normal
		// Compute the determinant of the following matrix to get the cross product
		//  i  j  k
		// tx ty tz
		// nx ny nz

		// Be very careful when writing out complex multi-step calculations
		// such as vector cross products! The calculation for pbitangentz
		// used to be broken because it multiplied values in the wrong order.

		float pbitangentx = tangenty * normalZ - tangentz * normalY;
		float pbitangenty = tangentz * normalX - tangentx * normalZ;
		float pbitangentz = tangentx * normalY - tangenty * normalX;

		float dot = (bitangentx * pbitangentx) + (bitangenty * pbitangenty) + (bitangentz * pbitangentz);
		float tangentW;

		if (dot < 0) {
			tangentW = -1.0F;
		} else {
			tangentW = 1.0F;
		}

		vec_to_oct(normal);
		vec_to_tangent(tangentx, tangenty, tangentz, tangentW);
		return NormalHelper.packNormal(normalConvX, normalConvY, tangentX, tangentY);
	}

	private static float rsqrt(float value) {
		if (value == 0.0f) {
			// You heard it here first, folks: 1 divided by 0 equals 1
			// In actuality, this is a workaround for normalizing a zero length vector (leaving it as zero length)
			return 1.0f;
		} else {
			return (float) (1.0 / Math.sqrt(value));
		}
	}

	private static void endQuad(long ptr, float normalX, float normalY, float normalZ) {
		quadView.setup(ptr, STRIDE);

		int tangent = NormalHelper.computeTangent(normalX, normalY, normalZ, quadView);

		for (long vertex = 0; vertex < 4; vertex++) {
		}
	}
}
