package net.coderbot.iris.compat.sodium.mixin.vertex_format;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadWinding;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import net.coderbot.iris.compat.sodium.impl.block_context.ContextAwareVertexWriter;
import net.coderbot.iris.compat.sodium.impl.vertex_format.ExtendedQuadView;
import net.coderbot.iris.vertices.NormalHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FluidRenderer.class)
public class MixinFluidRenderer {
	@Inject(method = "writeQuad", at = @At("HEAD"))
	private void setQuadInfo(ChunkModelBuilder builder, ChunkRenderBounds.Builder bounds, Material material, BlockPos offset, ModelQuadView quad, ModelQuadFacing facing, boolean flip, CallbackInfo ci) {
		ExtendedQuadView quadExt = ((ExtendedQuadView) quad);
		int normal = quad.getNormal();
		if (flip) {
			normal = NormalHelper.invertPackedNormal(normal);
		}
		((ContextAwareVertexWriter) builder.getVertexBuffer(facing)).iris$setQuadInfo(normal, quadExt.getTangent(), quadExt.getMidU(), quadExt.getMidV());
	}
}
