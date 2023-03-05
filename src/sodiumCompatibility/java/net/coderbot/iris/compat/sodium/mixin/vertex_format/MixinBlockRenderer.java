package net.coderbot.iris.compat.sodium.mixin.vertex_format;

import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import net.coderbot.iris.compat.sodium.impl.block_context.ContextAwareVertexWriter;
import net.coderbot.iris.compat.sodium.impl.vertex_format.ExtendedQuadView;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderer.class)
public class MixinBlockRenderer {
	@Inject(method = "writeGeometry", at = @At("HEAD"))
	private void setQuadInfo(BlockRenderContext ctx, ChunkModelBuilder builder, Vec3 offset, Material material, BakedQuadView quad, int[] colors, float[] brightness, int[] lightmap, ChunkRenderBounds.Builder bounds, CallbackInfo ci) {
		ExtendedQuadView quadExt = ((ExtendedQuadView) quad);
		((ContextAwareVertexWriter) builder.getVertexBuffer(quad.getNormalFace())).iris$setQuadInfo(quad.getNormal(), quadExt.getTangent(), quadExt.getMidU(), quadExt.getMidV());
	}
}
