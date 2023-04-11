package net.irisshaders.iris.compat.sodium.impl.vertex_format;

import me.jellysquid.mods.sodium.client.render.vertex.formats.ModelVertex;
import me.jellysquid.mods.sodium.client.render.vertex.serializers.VertexSerializer;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.entity_xhfp.EntityVertex;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.entity_xhfp.GlyphVertexExt;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp.XHFPModelVertexType;
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
			MemoryUtil.memPutInt(dst + 24, MemoryUtil.memGetInt(src + 20L));
			MemoryUtil.memPutInt(dst + 28, MemoryUtil.memGetInt(src + 24L));
			MemoryUtil.memPutInt(dst + 32, MemoryUtil.memGetInt(src + 28L));

			src += EntityVertex.STRIDE;
			dst += ModelVertex.STRIDE;
		}

	}
}
