package net.coderbot.iris.compat.sodium.mixin.vertex_format;

import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuad;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadViewMutable;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.coderbot.iris.compat.sodium.impl.vertex_format.ExtendedQuadView;
import net.coderbot.iris.vertices.NormalHelper;
import net.coderbot.iris.vertices.TriView;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModelQuad.class, priority = 1010)
public abstract class MixinModelQuad implements ExtendedQuadView, ModelQuadViewMutable, TriView {
	@Shadow
	public abstract float getTexU(int idx);

	@Shadow
	public abstract int getNormal();

	private int tangent;
	private float midU;
	private float midV;

	@Inject(method = "setNormal", at = @At("HEAD"))
	private void setTangent(int normal, CallbackInfo ci) {
		this.tangent = NormalHelper.computeTangent(NormI8.unpackX(normal), NormI8.unpackY(normal), NormI8.unpackZ(normal), this);
	}
	@Override
	public int getTangent() {
		return tangent;
	}

	@Override
	public float getMidU() {
		return midU;
	}

	@Override
	public float getMidV() {
		return midV;
	}

	@Inject(method = "setTexU", at = @At("TAIL"))
	private void setMidTexU(int idx, float u, CallbackInfo ci) {
		if (idx == 3)  {
			// Very basic assumption, this might not always be the case!!!
			midU = (getTexU(0) + getTexU(1) + getTexU(2) + u) * 0.25f;
		}
	}

	@Inject(method = "setTexV", at = @At("TAIL"))
	private void setMidTexV(int idx, float v, CallbackInfo ci) {
		if (idx == 3)  {
			int normal = getNormal();
			this.tangent = NormalHelper.computeTangent(NormI8.unpackX(normal), NormI8.unpackY(normal), NormI8.unpackZ(normal), this);
			// Very basic assumption, this might not always be the case!!!
			midV = (getTexV(0) + getTexV(1) + getTexV(2) + v) * 0.25f;
		}
	}

	@Override
	public float x(int i) {
		return getX(i);
	}

	@Override
	public float y(int i) {
		return getY(i);
	}

	@Override
	public float z(int i) {
		return getZ(i);
	}

	@Override
	public float u(int i) {
		return getTexU(i);
	}

	@Override
	public float v(int i) {
		return getTexV(i);
	}
}
