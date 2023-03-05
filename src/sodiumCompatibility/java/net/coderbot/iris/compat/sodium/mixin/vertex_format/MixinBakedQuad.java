package net.coderbot.iris.compat.sodium.mixin.vertex_format;

import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.coderbot.iris.Iris;
import net.coderbot.iris.compat.sodium.impl.vertex_format.ExtendedQuadView;
import net.coderbot.iris.vertices.NormalHelper;
import net.coderbot.iris.vertices.TriView;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BakedQuad.class, priority = 1010)
public abstract class MixinBakedQuad implements ExtendedQuadView, ModelQuadView, TriView {
	private int tangent;
	private float midU;
	private float midV;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void init(int[] vertexData, int colorIndex, Direction face, TextureAtlasSprite sprite, boolean shade, CallbackInfo ci) {
		int normal = getNormal();
		this.tangent = NormalHelper.computeTangent(NormI8.unpackX(normal), NormI8.unpackY(normal), NormI8.unpackZ(normal), this);
		this.midU = (getTexU(0) + getTexU(1) + getTexU(2) + getTexU(3)) * 0.25f;
		this.midV = (getTexV(0) + getTexV(1) + getTexV(2) + getTexV(3)) * 0.25f;
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
