package net.coderbot.iris.compat.sodium.mixin.vertex_format;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.RegionChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import net.coderbot.iris.compat.sodium.impl.IrisChunkShaderBindingPoints;
import net.coderbot.iris.compat.sodium.impl.vertex_format.IrisChunkMeshAttributes;
import net.coderbot.iris.shadows.ShadowRenderingState;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RegionChunkRenderer.class)
public abstract class MixinRegionChunkRenderer extends ShaderChunkRenderer {
	public MixinRegionChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
		super(device, vertexType);
	}

	@ModifyArg(method = "createRegionTessellation", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/gl/tessellation/TessellationBinding;forVertexBuffer(Lme/jellysquid/mods/sodium/client/gl/buffer/GlBuffer;[Lme/jellysquid/mods/sodium/client/gl/attribute/GlVertexAttributeBinding;)Lme/jellysquid/mods/sodium/client/gl/tessellation/TessellationBinding;"), index = 1)
	private GlVertexAttributeBinding[] iris$onInit(GlVertexAttributeBinding[] attributes) {
		if (!BlockRenderingSettings.INSTANCE.shouldUseExtendedVertexFormat()) {
			return attributes;
		}

		GlVertexFormat<ChunkMeshAttribute> vertexFormat = vertexType.getVertexFormat();

		attributes = ArrayUtils.addAll(attributes,
				new GlVertexAttributeBinding(IrisChunkShaderBindingPoints.MID_BLOCK,
						vertexFormat.getAttribute(IrisChunkMeshAttributes.MID_BLOCK)),
				new GlVertexAttributeBinding(IrisChunkShaderBindingPoints.BLOCK_ID,
						vertexFormat.getAttribute(IrisChunkMeshAttributes.BLOCK_ID)),
				new GlVertexAttributeBinding(IrisChunkShaderBindingPoints.MID_TEX_COORD,
						vertexFormat.getAttribute(IrisChunkMeshAttributes.MID_TEX_COORD)),
				new GlVertexAttributeBinding(IrisChunkShaderBindingPoints.TANGENT,
						vertexFormat.getAttribute(IrisChunkMeshAttributes.TANGENT)),
				new GlVertexAttributeBinding(IrisChunkShaderBindingPoints.NORMAL,
						vertexFormat.getAttribute(IrisChunkMeshAttributes.NORMAL))
		);
		return attributes;
	}

	@Inject(method = "getForwardFacingPlanes", at = @At("HEAD"), cancellable = true)
	private static void iris$disableBlockCulling(ChunkCameraContext camera, int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
		if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
			cir.setReturnValue(125);
		}
	}
}
