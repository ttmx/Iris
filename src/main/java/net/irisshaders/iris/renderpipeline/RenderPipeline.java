package net.irisshaders.iris.renderpipeline;

import net.irisshaders.iris.gl.texture.MutableTexture;

public interface RenderPipeline {
	//Executes a sub command buffer
	void execute(BakedCommandBuffer commandBuffer);

	//RenderType can also filter out other configurable properties (like dimension)
	void setRenderers(Pair<Filterable, RenderShaderBinding>... renderers);

	//Raw geometry call, calls the respective set shader
	void drawGeometry(Geometry geometry);

	void copyBuffer(Buffer source, long sourceOffset, Buffer dest, long destOffset, long length);

	void composite(RenderShaderBinding binding);

	void clearBuffer(Buffer buffer);

	void clearTexture(MutableTexture texture);

	void awaitPrePhase(RenderPhase phase);

	void awaitPhase(RenderPhase phase, boolean skip);

	void awaitPrePhase(IGetPhase phase) {
		awaitPrePhase(phase.getPhase());
	}

	void awaitPhase(IGetPhase phase, boolean skip) {
		awaitPhase(phase.getPhase(), skip);
	}
}
