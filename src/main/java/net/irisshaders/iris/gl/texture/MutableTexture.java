package net.irisshaders.iris.gl.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import net.irisshaders.iris.gl.GlResource;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.shaderpack.texture.TextureFilteringData;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL45C;

public class MutableTexture extends GlResource {
	public MutableTexture(TextureType target, int sizeX, int sizeY, int sizeZ, int internalFormat, boolean mipmap, TextureFilteringData filteringData) {
		super(GlStateManager._genTexture());

		TextureUploadHelper.resetTextureUploadState();

		target.applyNew(this.getGlId(), mipmap, sizeX, sizeY, sizeZ, internalFormat);

		int texture = getGlId();

		IrisRenderSystem.texParameteri(texture, target.getGlType(), GL11C.GL_TEXTURE_WRAP_S, filteringData.shouldClamp() ? GL13C.GL_CLAMP_TO_EDGE : GL13C.GL_REPEAT);

		if (sizeY > 0) {
			IrisRenderSystem.texParameteri(texture, target.getGlType(), GL11C.GL_TEXTURE_WRAP_T, filteringData.shouldClamp() ? GL13C.GL_CLAMP_TO_EDGE : GL13C.GL_REPEAT);
		}

		if (sizeZ > 0) {
			IrisRenderSystem.texParameteri(texture, target.getGlType(), GL30C.GL_TEXTURE_WRAP_R, filteringData.shouldClamp() ? GL13C.GL_CLAMP_TO_EDGE : GL13C.GL_REPEAT);
		}

		IrisRenderSystem.texParameteri(texture, target.getGlType(), GL20C.GL_TEXTURE_MAX_LEVEL, mipmap ? 3 : 0);
		if (!mipmap) {
			IrisRenderSystem.texParameteri(texture, target.getGlType(), GL20C.GL_TEXTURE_MIN_LOD, 0);
			IrisRenderSystem.texParameteri(texture, target.getGlType(), GL20C.GL_TEXTURE_MAX_LOD, 0);
			IrisRenderSystem.texParameterf(texture, target.getGlType(), GL20C.GL_TEXTURE_LOD_BIAS, 0.0F);
			IrisRenderSystem.texParameteri(texture, target.getGlType(), GL11C.GL_TEXTURE_MIN_FILTER, filteringData.shouldBlur() ? GL11C.GL_LINEAR : GL11C.GL_NEAREST);
			IrisRenderSystem.texParameteri(texture, target.getGlType(), GL11C.GL_TEXTURE_MAG_FILTER, filteringData.shouldBlur() ? GL11C.GL_LINEAR : GL11C.GL_NEAREST);
		} else {
			IrisRenderSystem.texParameteri(texture, target.getGlType(), GL11C.GL_TEXTURE_MIN_FILTER, filteringData.shouldBlur() ? GL11C.GL_LINEAR_MIPMAP_LINEAR : GL11C.GL_NEAREST_MIPMAP_NEAREST);
			IrisRenderSystem.texParameteri(texture, target.getGlType(), GL11C.GL_TEXTURE_MAG_FILTER, filteringData.shouldBlur() ? GL11C.GL_LINEAR_MIPMAP_LINEAR : GL11C.GL_NEAREST_MIPMAP_NEAREST);
		}
	}

	@Override
	protected void destroyInternal() {
		GL45C.glDeleteTextures(getGlId());
	}

	public void generateMipmap() {
		GL45C.glGenerateTextureMipmap(getGlId());
	}
}
