package net.coderbot.iris.samplers;

import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.coderbot.iris.features.FeatureFlags;
import net.coderbot.iris.gl.sampler.GlSampler;
import net.coderbot.iris.gl.texture.TextureAccess;
import net.coderbot.iris.gl.texture.TextureType;
import net.coderbot.iris.pipeline.CustomTextureManager;
import net.coderbot.iris.pipeline.WorldRenderingPipeline;
import net.coderbot.iris.rendertarget.RenderTargets;
import net.coderbot.iris.shaderpack.PackDirectives;
import net.coderbot.iris.shaderpack.ProgramSet;
import net.coderbot.iris.shadows.ShadowRenderTargets;
import org.lwjgl.opengl.GL43C;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

public class IrisSamplersNew {
	private static GlSampler SHADOW_SAMPLER_NEAREST;
	private static GlSampler SHADOW_SAMPLER_LINEAR;
	private static GlSampler LINEAR_MIPMAP;
	private static GlSampler NEAREST_MIPMAP;
	private final Object2ObjectMap<String, TextureSamplerState> textureState = new Object2ObjectOpenHashMap<>();
	private final Int2ObjectMap<TextureSamplerState> textures = new Int2ObjectOpenHashMap<>();
	private final Object2IntMap<String> bindings = new Object2IntOpenHashMap<>();

	public IrisSamplersNew(RenderTargets renderTargets, ShadowRenderTargets shadowRenderTargets, CustomTextureManager customTextureManager, WorldRenderingPipeline worldRenderingPipeline, ProgramSet programSet) {
		this.register(3, customTextureManager.getNoiseTexture().getType(), customTextureManager.getNoiseTexture().getTextureId(), null, "noisetex");
		this.register(4, TextureType.TEXTURE_2D, worldRenderingPipeline::getCurrentNormalTexture, null, "normals");
		this.register(5, TextureType.TEXTURE_2D, worldRenderingPipeline::getCurrentSpecularTexture, null, "specular");
		this.register(6, TextureType.TEXTURE_2D, () -> shadowRenderTargets.getDepthTexture().getTextureId(), programSet.getPack().hasFeature(FeatureFlags.SEPARATE_HARDWARE_SAMPLERS) ? null : (shadowRenderTargets.isHardwareFiltered(0) ? shadowRenderTargets.isLinearFiltered(0) ? SHADOW_SAMPLER_LINEAR : SHADOW_SAMPLER_NEAREST : null), "shadowtex0", "shadow");
		this.register(7, TextureType.TEXTURE_2D, () -> shadowRenderTargets.getDepthTextureNoTranslucents().getTextureId(), programSet.getPack().hasFeature(FeatureFlags.SEPARATE_HARDWARE_SAMPLERS) ? null : (shadowRenderTargets.isHardwareFiltered(1) ? shadowRenderTargets.isLinearFiltered(1) ? SHADOW_SAMPLER_LINEAR : SHADOW_SAMPLER_NEAREST : null), "shadowtex1");
		this.register(8, TextureType.TEXTURE_2D, renderTargets::getDepthTexture, null, "depthtex0");
		this.register(9, TextureType.TEXTURE_2D, () -> renderTargets.getDepthTextureNoTranslucents().getTextureId(), null, "depthtex1");
		this.register(10, TextureType.TEXTURE_2D, () -> renderTargets.getDepthTextureNoHand().getTextureId(), null, "depthtex2");

		int unit = 11;

		for (int i = 0; i < shadowRenderTargets.getRenderTargetCount(); i++) {
			int finalI = i;
			this.register(unit, TextureType.TEXTURE_2D, () -> shadowRenderTargets.doesTargetExist(finalI) ? shadowRenderTargets.get(finalI).getMainTexture() : 0, null, "shadowcolor" + i + "main");
			this.register(unit + 1, TextureType.TEXTURE_2D, () -> shadowRenderTargets.doesTargetExist(finalI) ? shadowRenderTargets.get(finalI).getAltTexture() : 0, null, "shadowcolor" + i + "alt");

			unit += 2;
		}

		for (int i = 0; i < renderTargets.getRenderTargetCount(); i++) {
			int finalI = i;
			this.register(unit, TextureType.TEXTURE_2D, () -> renderTargets.get(finalI).getMainTexture(), null, "colortex" + i + "main");
			this.register(unit + 1, TextureType.TEXTURE_2D, () -> renderTargets.get(finalI).getAltTexture(), null, "colortex" + i + "alt");

			unit += 2;
		}

		for (Map.Entry<String, TextureAccess> entry : customTextureManager.getIrisCustomTextures().entrySet()) {
			String name = entry.getKey();
			TextureAccess tex = entry.getValue();
			this.register(unit, tex.getType(), tex.getTextureId(), null, name);

			unit++;
		}

		rebindTextures();
	}

	public static void initRenderer() {
		SHADOW_SAMPLER_NEAREST = new GlSampler(false, false, true, true);
		SHADOW_SAMPLER_LINEAR = new GlSampler(true, false, true, true);
		LINEAR_MIPMAP = new GlSampler(true, true, false, false);
		NEAREST_MIPMAP = new GlSampler(false, true, false, false);
	}

	/**
	 * Sets up the uniforms for textures in a given program. The program will be bound.
	 */
	public void setupUniforms(int program) {
		GlStateManager._glUseProgram(program);
		bindings.forEach((name, unit) -> {
			GlStateManager._glUniform1i(GlStateManager._glGetUniformLocation(program, name), unit);
		});
	}

	/**
	 * Registers a texture. Multiple names can be applied to any given texture, but the names and unit **cannot** be changed.
	 * @param unit The texture unit that can be bound to. Maximum is {@code GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS} (minimum 96, maximum 196).
	 * @param type The texture type (1D, 2D, 3D, or rectangle)
	 * @param texture The texture ID. This can be changed at runtime as it is a supplier.
	 * @param sampler The default sampler. This can be overriden with a different sampler by the render function if needed.
	 * @param names The names to register the texture to.
	 */
	private void register(int unit, TextureType type, IntSupplier texture, GlSampler sampler, String... names) {
		unit += 10;
		TextureSamplerState state = new TextureSamplerState(names, type.getGlType(), unit, texture, sampler);
		for (String name : names) {
			textureState.put(name, state);
			bindings.put(name, unit);
		}
		textures.put(unit, state);
	}

	public void rebindTextures() {
		textures.forEach((unit, texture) -> {
			texture.bindWithSampler();
		});
	}
}
