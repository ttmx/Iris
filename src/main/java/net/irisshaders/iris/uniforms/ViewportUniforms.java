package net.irisshaders.iris.uniforms;

import net.irisshaders.iris.gl.uniform.UniformCreator;
import net.minecraft.client.Minecraft;

/**
 * Implements uniforms relating the current viewport
 *
 * @see <a href="https://github.com/IrisShaders/ShaderDoc/blob/master/uniforms.md#viewport">Uniforms: Viewport</a>
 */
public final class ViewportUniforms {
	// cannot be constructed
	private ViewportUniforms() {
	}

	/**
	 * Makes the viewport uniforms available to the given program
	 *
	 * @param uniforms the program to make the uniforms available to
	 */
	public static void addViewportUniforms(UniformCreator uniforms) {
		// TODO: What about the custom scale.composite3 property?
		// NB: It is not safe to cache the render target due to mods like Resolution Control modifying the render target field.
		uniforms
			.registerFloatUniform(true, "viewHeight", () -> Minecraft.getInstance().getMainRenderTarget().height)
			.registerFloatUniform(true, "viewWidth", () -> Minecraft.getInstance().getMainRenderTarget().width)
			.registerFloatUniform(true, "aspectRatio", ViewportUniforms::getAspectRatio);
	}

	/**
	 * @return the current viewport aspect ratio, calculated from the current Minecraft window size
	 */
	private static float getAspectRatio() {
		return ((float) Minecraft.getInstance().getMainRenderTarget().width) / ((float) Minecraft.getInstance().getMainRenderTarget().height);
	}
}
