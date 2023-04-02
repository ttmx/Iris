package net.coderbot.iris.compat.sodium.mixin.shadow_map.frustum;

import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import me.jellysquid.mods.sodium.client.util.frustum.FrustumAdapter;
import net.coderbot.iris.shadows.frustum.BoxCuller;
import net.coderbot.iris.shadows.frustum.fallback.BoxCullingFrustum;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BoxCullingFrustum.class)
public class MixinBoxCullingFrustum implements Frustum, FrustumAdapter {
	@Shadow(remap = false)
	@Final
	private BoxCuller boxCuller;

	// TODO: Better way to do this... Maybe we shouldn't be using a frustum for the box culling in the first place!
	@Override
	public boolean testBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		// TODO: Frustum.INSIDE
		return !this.boxCuller.isCulled(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public Frustum sodium$createFrustum() {
		return this;
	}
}
