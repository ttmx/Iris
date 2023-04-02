package net.coderbot.iris.compat.sodium.mixin.shadow_map.frustum;

import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import me.jellysquid.mods.sodium.client.util.frustum.FrustumAdapter;
import net.coderbot.iris.shadows.frustum.fallback.NonCullingFrustum;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(NonCullingFrustum.class)
public class MixinNonCullingFrustum implements Frustum, FrustumAdapter {

	@Override
	public Frustum sodium$createFrustum() {
		return this;
	}

	@Override
	public boolean testBox(double v, double v1, double v2, double v3, double v4, double v5) {
		return true;
	}
}
