package net.coderbot.iris.vertices;

import net.minecraft.util.Mth;
import org.joml.Math;

public class OctahedralCalculator {
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

	public static int sign(float pDouble0) {
		return pDouble0 >= 0.0f ? 1 : -1;
	}

	public int calculateOctahedralEncode(boolean flipUpcomingNormal, QuadView q) {
		final float x0;
		final float y0;
		final float z0;
		final float x1;
		final float y1;
		final float z1;
		final float x2;
		final float y2;
		final float z2;
		final float x3;
		final float y3;
		final float z3;

		x0 = q.x(0);
		y0 = q.y(0);
		z0 = q.z(0);
		x1 = q.x(1);
		y1 = q.y(1);
		z1 = q.z(1);
		x2 = q.x(2);
		y2 = q.y(2);
		z2 = q.z(2);
		x3 = q.x(3);
		y3 = q.y(3);
		z3 = q.z(3);

		final float dx0 = x2 - x0;
		final float dy0 = y2 - y0;
		final float dz0 = z2 - z0;
		final float dx1 = x3 - x1;
		final float dy1 = y3 - y1;
		final float dz1 = z3 - z1;

		float normX = dy0 * dz1 - dz0 * dy1;
		float normY = dz0 * dx1 - dx0 * dz1;
		float normZ = dx0 * dy1 - dy0 * dx1;

		float l = Math.sqrt(normX * normX + normY * normY + normZ * normZ);

		if (l != 0) {
			normX /= l;
			normY /= l;
			normZ /= l;
		}

		float edge1x = x1 - x0;
		float edge1y = y1 - y0;
		float edge1z = z1 - z0;

		float edge2x = x2 - x0;
		float edge2y = y2 - y0;
		float edge2z = z2 - z0;

		float u0 = q.u(0);
		float v0 = q.v(0);

		float u1 = q.u(1);
		float v1 = q.v(1);

		float u2 = q.u(2);
		float v2 = q.v(2);

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

		float pbitangentx = tangenty * normZ - tangentz * normY;
		float pbitangenty = tangentz * normX - tangentx * normZ;
		float pbitangentz = tangentx * normY - tangenty * normX;

		float dot = (bitangentx * pbitangentx) + (bitangenty * pbitangenty) + (bitangentz * pbitangentz);
		float tangentW;

		if (dot < 0) {
			tangentW = -1.0F;
		} else {
			tangentW = 1.0F;
		}

		float invL1Norm = 1.0f / (Math.abs(normX) + Math.abs(normY) + Math.abs(normZ));
		float resX = 0.0f, resY = 0.0f;
		if (normZ < 0.0f) {
			resX = (1.0f - Math.abs(normY * invL1Norm)) * sign(normX);
			resY = (1.0f - Math.abs(normX * invL1Norm)) * sign(normY);
		} else {
			resX = normX * invL1Norm;
			resY = normY * invL1Norm;
		}

		byte normalConvX = floatToSnorm8(resX);
		byte normalConvY = floatToSnorm8(resY);

		//encode to octahedron, result in range [-1, 1]
		float invL1Tang = 1.0f / (Math.abs(tangentx) + Math.abs(tangenty) + Math.abs(tangentz));
		float tanX = 0.0f, tanY = 0.0f;
		if (tangentz < 0.0f) {
			tanX = (1.0f - Math.abs(tangenty * invL1Tang)) * sign(tangentx);
			tanY = (1.0f - Math.abs(tangentx * invL1Tang)) * sign(tangenty);
		} else {
			tanX = tangentx * invL1Tang;
			tanY = tangenty * invL1Tang;
		}

		// map y to always be positive
		tanY = tanY * 0.5f + 0.5f;

		// add a bias so that y is never 0 (sign in the vertex shader)
		if (tanY < bias)
			tanY = bias;

		// Apply the sign of the binormal to y, which was computed elsewhere
		if (tangentW < 0)
			tanY *= -1;

		byte convTangX = floatToSnorm8(tanX);
		byte convTangY = floatToSnorm8(tanY);

		return convTangY << 24 | convTangX << 16 | normalConvY << 8 | normalConvX << 0;
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
}
