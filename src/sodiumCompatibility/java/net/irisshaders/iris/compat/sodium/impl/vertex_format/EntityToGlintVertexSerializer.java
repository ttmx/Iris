package net.irisshaders.iris.compat.sodium.impl.vertex_format;

import me.jellysquid.mods.sodium.client.render.vertex.formats.ModelVertex;
import me.jellysquid.mods.sodium.client.render.vertex.serializers.VertexSerializer;
import me.jellysquid.mods.sodium.client.render.vertex.transform.VertexTransform;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.entity_xhfp.EntityVertex;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp.XHFPModelVertexType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.lwjgl.system.MemoryUtil;

public class EntityToGlintVertexSerializer implements VertexSerializer {
	@Override
	public void serialize(long src, long dst, int vertexCount) {
		for (int vertexIndex = 0; vertexIndex < vertexCount; ++vertexIndex) {
			MemoryUtil.memPutFloat(dst, MemoryUtil.memGetFloat(src));
			MemoryUtil.memPutFloat(dst + 4, MemoryUtil.memGetFloat(src + 4));
			MemoryUtil.memPutFloat(dst + 8, MemoryUtil.memGetFloat(src + 8));
			MemoryUtil.memPutFloat(dst + 12, XHFPModelVertexType.decodeBlockTexture(MemoryUtil.memGetShort(src + 16L)));
			MemoryUtil.memPutFloat(dst + 16, XHFPModelVertexType.decodeBlockTexture(MemoryUtil.memGetShort(src + 18L)));

			src += EntityVertex.STRIDE;
			dst += 20;
		}

	}
}
