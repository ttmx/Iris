package net.irisshaders.iris.compat.sodium.impl.vertex_format;

import me.jellysquid.mods.sodium.client.render.vertex.formats.ModelVertex;
import me.jellysquid.mods.sodium.client.render.vertex.serializers.VertexSerializer;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.entity_xhfp.EntityVertex;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.entity_xhfp.GlyphVertexExt;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp.XHFPModelVertexType;
import net.irisshaders.iris.vertices.NormalHelper;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

public class EntityToModelVertexSerializer implements VertexSerializer {
	@Override
	public void serialize(long src, long dst, int vertexCount) {
		for (int vertexIndex = 0; vertexIndex < vertexCount; ++vertexIndex) {
			MemoryUtil.memPutFloat(dst, MemoryUtil.memGetFloat(src));
			MemoryUtil.memPutFloat(dst + 4, MemoryUtil.memGetFloat(src + 4L));
			MemoryUtil.memPutFloat(dst + 8, MemoryUtil.memGetFloat(src + 8L));
			MemoryUtil.memPutInt(dst + 12, MemoryUtil.memGetInt(src + 12L));
			MemoryUtil.memPutFloat(dst + 16, XHFPModelVertexType.decodeBlockTexture(MemoryUtil.memGetShort(src + 16L)));
			MemoryUtil.memPutFloat(dst + 20, XHFPModelVertexType.decodeBlockTexture(MemoryUtil.memGetShort(src + 18L)));
			MemoryUtil.memPutInt(dst + 24, OverlayTexture.pack(MemoryUtil.memGetByte(src + 20L) & 0xFF, MemoryUtil.memGetByte(src + 21L) & 0xFF));
			MemoryUtil.memPutInt(dst + 28, OverlayTexture.pack(MemoryUtil.memGetByte(src + 22L) & 0xFF, MemoryUtil.memGetByte(src + 23L) & 0xFF));

			MemoryUtil.memPutInt(dst + 32, EntityVertex.lastNormalHeld);

			src += EntityVertex.STRIDE;
			dst += ModelVertex.STRIDE;
		}

	}

	int oct_to_vec(byte x, byte y) {
		float x2 = ((float) (x & 0xFF)) / 255f;
		float y2 = ((float) (y & 0xFF)) / 255f;

		float z = 1.0f - Math.abs(x2) - Math.abs(y2);
		float t = Math.max(-z, 0.0f);
		float a = (float) (t * -(Math.floor((Mth.sign(x2) + Mth.sign(y2)) / 2f)));
		x2 += a;
		y2 += a;
		return Norm3b.pack(x2, y2, z);
	}

	public static int oct_to_float322x3(byte x, byte y) {
		float fx = (float) x / (float) Byte.MAX_VALUE;
		float fy = (float) y / (float) Byte.MAX_VALUE;
		Vector2f e = new Vector2f(fx, fy);
		Vector3f v = new Vector3f(e.x(), e.y(), 1.0f - Math.abs(e.x()) - Math.abs(e.y()));
		if (v.z < 0) {
			float signX = Math.signum(v.x());
			float signY = Math.signum(v.y());
			float absX = Math.abs(v.x());
			float absY = Math.abs(v.y());
			v.x = absY * signX;
			v.y = absX * signY;
		}
		return NormalHelper.packNormal(v.normalize(), 0.0f);
	}

	public static Vector3f oct_to_float32x3(byte x, byte y) {
		Vector2f v = new Vector2f(x / Byte.MAX_VALUE, y / Byte.MAX_VALUE);
		float oneMinusX = 1.0f - Math.abs(v.x());
		float oneMinusY = 1.0f - Math.abs(v.y());
		Vector3f n;
		if (v.x() >= 0) {
			if (v.y() >= 0) {
				float a = oneMinusY * (v.x() * v.x());
				float b = oneMinusX * (v.y() * v.y());
				float c = v.x();
				n = new Vector3f(a, b, c);
			} else {
				float a = oneMinusX * (v.y() * v.y());
				float b = oneMinusY * (v.x() * v.x());
				float c = -v.y();
				n = new Vector3f(a, b, c);
			}
		} else {
			if (v.y() >= 0) {
				float a = oneMinusX * (v.y() * v.y());
				float b = oneMinusY * (v.x() * v.x());
				float c = v.y();
				n = new Vector3f(a, b, c);
			} else {
				float a = oneMinusY * (v.x() * v.x());
				float b = oneMinusX * (v.y() * v.y());
				float c = -v.x();
				n = new Vector3f(a, b, c);
			}
		}
		if (n.lengthSquared() == 0) {
			return new Vector3f(0, 0, 1);
		}
		return n.normalize();
	}
}
