package net.coderbot.iris.samplers;

import net.coderbot.iris.gl.IrisRenderSystem;
import net.coderbot.iris.gl.sampler.GlSampler;

import java.util.function.IntSupplier;

public class TextureSamplerState {
	private final IntSupplier textureSupplier;
	private final GlSampler defaultSampler;
	private final int unit;
	private final int target;
	private final String[] names;
	private int lastBoundId;

	public TextureSamplerState(String[] names, int target, int unit, IntSupplier textureSupplier, GlSampler defaultSampler) {
		this.names = names;
		this.target = target;
		this.unit = unit;
		this.textureSupplier = textureSupplier;
		this.defaultSampler = defaultSampler;
	}

	public void bindWithoutSampler() {
		int texture = textureSupplier.getAsInt();
		IrisRenderSystem.bindTextureToUnit(target, unit, texture);
		lastBoundId = texture;
	}

	public void resetSampler() {
		IrisRenderSystem.bindSamplerToUnit(unit, defaultSampler == null ? 0 : defaultSampler.getId());
	}

	public void bindWithSampler() {
		bindWithoutSampler();
		resetSampler();
	}
}
