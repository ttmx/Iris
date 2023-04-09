package net.irisshaders.iris.uniforms;

import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.gl.uniform.UniformCreator;
import net.irisshaders.iris.pipeline.FogMode;
import org.joml.Vector4f;


/**
 * Internal Iris uniforms that are not directly accessible by shaders.
 */
public class IrisInternalUniforms {
	private IrisInternalUniforms() {
		// no construction
	}

	public static void addFogUniforms(UniformCreator uniforms, FogMode fogMode) {
		/*uniforms
			// TODO: Update frequency of continuous?
			.uniform4f(true, "iris_FogColor", () -> {
				float[] fogColor = RenderSystem.getShaderFogColor();
				return new Vector4f(fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
			});

		uniforms.registerFloatUniform(true, "iris_FogStart", RenderSystem::getShaderFogStart)
			.registerFloatUniform(true, "iris_FogEnd", RenderSystem::getShaderFogEnd);

		uniforms.registerFloatUniform("iris_FogDensity", () -> {
			// ensure that the minimum value is 0.0
			return Math.max(0.0F, CapturedRenderingState.INSTANCE.getFogDensity());
		}, notifier -> {
		});

		uniforms.registerFloatUniform("iris_currentAlphaTest", CapturedRenderingState.INSTANCE::getCurrentAlphaTest, notifier -> {
		});

		// Optifine compatibility
		uniforms.registerFloatUniform("alphaTestRef", CapturedRenderingState.INSTANCE::getCurrentAlphaTest, notifier -> {
		});*/

	}
}
