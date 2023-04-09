package net.irisshaders.iris.uniforms;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.gl.state.StateUpdateNotifiers;
import net.irisshaders.iris.gl.uniform.UniformCreator;
import net.irisshaders.iris.pipeline.FogMode;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

public class FogUniforms {
	private FogUniforms() {
		// no construction
	}

	public static void addFogUniforms(UniformCreator uniforms, FogMode fogMode) {
	/*	if (fogMode == FogMode.OFF) {
			uniforms.registerIntegerUniform(false, "fogMode", () -> 0);
			uniforms.registerIntegerUniform(false, "fogShape", () -> -1);
		} else if (fogMode == FogMode.PER_VERTEX || fogMode == FogMode.PER_FRAGMENT) {
			uniforms.registerIntegerUniform("fogMode", () -> {
				float fogDensity = CapturedRenderingState.INSTANCE.getFogDensity();

				if (fogDensity < 0.0F) {
					return GL11.GL_LINEAR;
				} else {
					return GL11.GL_EXP2;
				}
			}, listener -> {
			});

			// To keep a stable interface, 0 is defined as spherical while 1 is defined as cylindrical, even if Mojang's index changes.
			uniforms.registerIntegerUniform(true, "fogShape", () -> RenderSystem.getShaderFogShape() == FogShape.CYLINDER ? 1 : 0);
		}

		uniforms.registerFloatUniform("fogDensity", () -> {
			// ensure that the minimum value is 0.0
			return Math.max(0.0F, CapturedRenderingState.INSTANCE.getFogDensity());
		}, notifier -> {
		});

		uniforms.registerFloatUniform("fogStart", RenderSystem::getShaderFogStart, listener -> {
			StateUpdateNotifiers.fogStartNotifier.setListener(listener);
		});

		uniforms.registerFloatUniform("fogEnd", RenderSystem::getShaderFogEnd, listener -> {
			StateUpdateNotifiers.fogEndNotifier.setListener(listener);
		});

		uniforms
			// TODO: Update frequency of continuous?
			.uniform3f(true, "fogColor", () -> {
				float[] fogColor = RenderSystem.getShaderFogColor();
				return new Vector3f(fogColor[0], fogColor[1], fogColor[2]);
			});*/
	}
}
