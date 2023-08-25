package net.coderbot.iris.vertices;

import org.joml.Matrix3f;
import org.joml.Quaternionf;

public class QTangentCalculator {
	private Matrix3f tbn = new Matrix3f();
	private Quaternionf qTangent = new Quaternionf();
   	private static float bias = 1.0f / (2^(8 - 1) - 1);

	public int calculateQTangent(boolean flipUpcomingNormal, QuadView q) {
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

		// TODO: can tangents also use flipped xyz?
		if (flipUpcomingNormal) {
			x0 = q.x(3);
			y0 = q.y(3);
			z0 = q.z(3);
			x1 = q.x(2);
			y1 = q.y(2);
			z1 = q.z(2);
			x2 = q.x(1);
			y2 = q.y(1);
			z2 = q.z(1);
			x3 = q.x(0);
			y3 = q.y(0);
			z3 = q.z(0);
		} else {
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
		}

		final float dx0 = x2 - x0;
		final float dy0 = y2 - y0;
		final float dz0 = z2 - z0;
		final float dx1 = x3 - x1;
		final float dy1 = y3 - y1;
		final float dz1 = z3 - z1;

		float normX = dy0 * dz1 - dz0 * dy1;
		float normY = dz0 * dx1 - dx0 * dz1;
		float normZ = dx0 * dy1 - dy0 * dx1;

		float l = (float) Math.sqrt(normX * normX + normY * normY + normZ * normZ);

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

		qTangent.setFromNormalized(tbn.set(normX, normY, normZ, tangentx, tangenty, tangentz, bitangentx, bitangenty, bitangentz));

		qTangent.normalize();

		//Make sure QTangent is always positive
		if (qTangent.w < 0)
			qTangent.set(-qTangent.x, -qTangent.y, -qTangent.z, -qTangent.w);

		//Because '-0' sign information is lost when using integers,
		//we need to apply a "bias"; while making sure the Quaternion
		//stays normalized.
		// ** Also our shaders assume qTangent.w is never 0. **
		if (qTangent.w < bias) {
			float normFactor = (float) Math.sqrt( 1 - bias * bias );
			qTangent.w = bias;
			qTangent.x *= normFactor;
			qTangent.y *= normFactor;
			qTangent.z *= normFactor;
		}

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

		if (dot <= 0) {
			qTangent.set(-qTangent.x, -qTangent.y, -qTangent.z, -qTangent.w);
		}

		return NormI8.pack(qTangent.x, qTangent.y, qTangent.z, qTangent.w);
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
