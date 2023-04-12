package net.irisshaders.iris.compat.sodium.impl.vertex_format;

import me.jellysquid.mods.sodium.client.render.vertex.formats.ModelVertex;
import me.jellysquid.mods.sodium.client.render.vertex.serializers.VertexSerializer;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.entity_xhfp.EntityVertex;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.entity_xhfp.GlyphVertexExt;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp.XHFPModelVertexType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
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

			MemoryUtil.memPutInt(dst + 32, oct_to_vec(MemoryUtil.memGetByte(src + 28L), MemoryUtil.memGetByte(src + 29L)));

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
}
