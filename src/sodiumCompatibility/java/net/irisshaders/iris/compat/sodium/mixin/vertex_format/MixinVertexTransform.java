package net.irisshaders.iris.compat.sodium.mixin.vertex_format;

import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatRegistry;
import me.jellysquid.mods.sodium.client.render.vertex.transform.CommonVertexElement;
import me.jellysquid.mods.sodium.client.render.vertex.transform.VertexTransform;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.IrisCommonVertexElements;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.entity_xhfp.EntityVertex;
import net.irisshaders.iris.compat.sodium.impl.vertex_format.terrain_xhfp.XHFPModelVertexType;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.minecraft.core.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexTransform.class)
public class MixinVertexTransform {
	private static int entityId = VertexFormatRegistry.get(IrisVertexFormats.ENTITY).id;

	@Inject(method = "transformOverlay", at = @At("HEAD"), cancellable = true, remap = false)
	private static void transformOverlayRedirect(long ptr, int count, VertexFormatDescription format, Matrix3f inverseNormalMatrix, Matrix4f inverseTextureMatrix, float textureScale, CallbackInfo ci) {
		if (format.id == entityId) {
			ci.cancel();
			transformOverlayCompressed(ptr, count, format, inverseNormalMatrix, inverseTextureMatrix, textureScale);
		}
	}
	/**
	 * @author IMS
	 * @reason Rewrite to edit midTexCoord too
	 */
	@Overwrite(remap = false)
	public static void transformSprite(long ptr, int count, VertexFormatDescription format,
									   float minU, float minV, float maxU, float maxV) {
		if (format.id == entityId) {
			useCompressed(ptr, count, format, minU, minV, maxU, maxV);
		} else {
			useUncompressed(ptr, count, format, minU, minV, maxU, maxV);
		}
	}

	/**
	 * Transforms the overlay UVs element of each vertex to create a perspective-mapped effect.
	 *
	 * @param ptr    The buffer of vertices to transform
	 * @param count  The number of vertices to transform
	 * @param format The format of the vertices
	 * @param inverseNormalMatrix The inverted normal matrix
	 * @param inverseTextureMatrix The inverted texture matrix
	 * @param textureScale The amount which the overlay texture should be adjusted
	 */
	private static void transformOverlayCompressed(long ptr, int count, VertexFormatDescription format,
												  Matrix3f inverseNormalMatrix, Matrix4f inverseTextureMatrix, float textureScale) {
		var offsetPosition = format.getOffset(CommonVertexElement.POSITION);
		var offsetColor = format.getOffset(CommonVertexElement.COLOR);
		var offsetTexture = format.getOffset(IrisCommonVertexElements.TEXTURE_COMPRESSED);

		int color = ColorABGR.pack(1.0f, 1.0f, 1.0f, 1.0f);

		var normal = new Vector3f(Float.NaN);
		var position = new Vector4f(Float.NaN);

		for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
			position.x = MemoryUtil.memGetFloat(ptr + offsetPosition + 0);
			position.y = MemoryUtil.memGetFloat(ptr + offsetPosition + 4);
			position.z = MemoryUtil.memGetFloat(ptr + offsetPosition + 8);
			position.w = 1.0f;

			int packedNormal = EntityVertex.lastNormalHeld;
			normal.x = Norm3b.unpackX(packedNormal);
			normal.y = Norm3b.unpackY(packedNormal);
			normal.z = Norm3b.unpackZ(packedNormal);

			Vector3f transformedNormal = inverseNormalMatrix.transform(normal);
			Direction direction = Direction.getNearest(transformedNormal.x(), transformedNormal.y(), transformedNormal.z());

			Vector4f transformedTexture = inverseTextureMatrix.transform(position);
			transformedTexture.rotateY(3.1415927F);
			transformedTexture.rotateX(-1.5707964F);
			transformedTexture.rotate(direction.getRotation());

			float textureU = -transformedTexture.x() * textureScale;
			float textureV = -transformedTexture.y() * textureScale;

			MemoryUtil.memPutInt(ptr + offsetColor, color);

			MemoryUtil.memPutShort(ptr + offsetTexture + 0, XHFPModelVertexType.encodeBlockTexture(textureU));
			MemoryUtil.memPutShort(ptr + offsetTexture + 2, XHFPModelVertexType.encodeBlockTexture(textureV));

			ptr += format.stride;
		}
	}

	private static void useCompressed(long ptr, int count, VertexFormatDescription format, float minU, float minV, float maxU, float maxV) {
		long stride = format.stride;
		long offsetUV = format.getOffset(IrisCommonVertexElements.TEXTURE_COMPRESSED);

		boolean hasMidTexCoord = false;
		long offsetMidTexCoord = 0;

		if (format.getElements().contains(IrisVertexFormats.MID_TEXTURE_ELEMENT_COMPRESSED)) {
			hasMidTexCoord = true;
			offsetMidTexCoord = format.getOffset(IrisCommonVertexElements.MID_TEX_COMPRESSED);
		}
		// The width/height of the sprite
		float w = maxU - minU;
		float h = maxV - minV;

		for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
			// The coordinate relative to the sprite bounds
			float u = XHFPModelVertexType.decodeBlockTexture(MemoryUtil.memGetShort(ptr + offsetUV));
			float v = XHFPModelVertexType.decodeBlockTexture(MemoryUtil.memGetShort(ptr + offsetUV + 2));

			// The coordinate absolute to the sprite sheet
			float ut = minU + (w * u);
			float vt = minV + (h * v);

			MemoryUtil.memPutShort(ptr + offsetUV, XHFPModelVertexType.encodeBlockTexture(ut));
			MemoryUtil.memPutShort(ptr + offsetUV + 2, XHFPModelVertexType.encodeBlockTexture(vt));

			if (hasMidTexCoord) {
				float midU = XHFPModelVertexType.decodeBlockTexture(MemoryUtil.memGetShort(ptr + offsetMidTexCoord));
				float midV = XHFPModelVertexType.decodeBlockTexture(MemoryUtil.memGetShort(ptr + offsetMidTexCoord + 2));

				// The coordinate absolute to the sprite sheet
				float midut = minU + (w * midU);
				float midvt = minV + (h * midV);

				MemoryUtil.memPutShort(ptr + offsetMidTexCoord, XHFPModelVertexType.encodeBlockTexture(midut));
				MemoryUtil.memPutShort(ptr + offsetMidTexCoord + 2, XHFPModelVertexType.encodeBlockTexture(midvt));
			}

			ptr += stride;
		}
	}

	private static void useUncompressed(long ptr, int count, VertexFormatDescription format, float minU, float minV, float maxU, float maxV) {
		long stride = format.stride;
		long offsetUV = format.getOffset(CommonVertexElement.TEXTURE);

		boolean hasMidTexCoord = false;
		long offsetMidTexCoord = 0;

		if (format.getElements().contains(IrisVertexFormats.MID_TEXTURE_ELEMENT)) {
			hasMidTexCoord = true;
			offsetMidTexCoord = format.getOffset(IrisCommonVertexElements.MID_TEX_COORD);
		}
		// The width/height of the sprite
		float w = maxU - minU;
		float h = maxV - minV;

		for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
			// The coordinate relative to the sprite bounds
			float u = MemoryUtil.memGetFloat(ptr + offsetUV);
			float v = MemoryUtil.memGetFloat(ptr + offsetUV + 4);

			// The coordinate absolute to the sprite sheet
			float ut = minU + (w * u);
			float vt = minV + (h * v);

			MemoryUtil.memPutFloat(ptr + offsetUV, ut);
			MemoryUtil.memPutFloat(ptr + offsetUV + 4, vt);

			if (hasMidTexCoord) {
				float midU = MemoryUtil.memGetFloat(ptr + offsetMidTexCoord);
				float midV = MemoryUtil.memGetFloat(ptr + offsetMidTexCoord + 4);

				// The coordinate absolute to the sprite sheet
				float midut = minU + (w * midU);
				float midvt = minV + (h * midV);

				MemoryUtil.memPutFloat(ptr + offsetMidTexCoord, midut);
				MemoryUtil.memPutFloat(ptr + offsetMidTexCoord + 4, midvt);
			}

			ptr += stride;
		}


	}
}
