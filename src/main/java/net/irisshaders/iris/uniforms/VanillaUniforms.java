package net.irisshaders.iris.uniforms;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.gl.uniform.UniformCreator;
import org.joml.Vector2f;

public class VanillaUniforms {
	public static void addVanillaUniforms(UniformCreator uniforms) {
		Vector2f cachedScreenSize = new Vector2f();
		// listener -> {} dictates we want this to run on every shader update, not just on a new frame. These are dynamic.
		uniforms.registerFloatUniform(true, "iris_LineWidth", RenderSystem::getShaderLineWidth);
		uniforms.registerVector2Uniform(true, "iris_ScreenSize", () -> cachedScreenSize.set(GlStateManager.Viewport.width(), GlStateManager.Viewport.height()));
	}
}
