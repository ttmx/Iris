package net.coderbot.batchedentityrendering.impl;

import com.mojang.blaze3d.vertex.VertexFormat;

public interface BufferBuilderExt {
	void splitStrip();
	VertexFormat getFormat();
}
