package net.coderbot.iris.compat.sodium.mixin.shadow_map.frustum;

import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import me.jellysquid.mods.sodium.client.util.frustum.FrustumAdapter;
import net.coderbot.iris.shadows.frustum.advanced.AdvancedShadowCullingFrustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AdvancedShadowCullingFrustum.class)
public abstract class MixinAdvancedShadowCullingFrustum implements Frustum, FrustumAdapter {
	@Shadow(remap = false)
	public abstract int isVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);

	@Override
	public boolean testBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		// TODO: Visibility.INSIDE
		return isVisible(minX, minY, minZ, maxX, maxY, maxZ) != 0;
	}

	@Override
	public Frustum sodium$createFrustum() {
		return this;
	}
}
