package net.irisshaders.iris.uniforms;

import net.irisshaders.iris.gl.uniform.UniformCreator;
import net.irisshaders.iris.renderers.ShadowRenderer;
import net.irisshaders.iris.shaderpack.PackDirectives;
import net.irisshaders.iris.shadows.ShadowMatrices;
import org.joml.Matrix4f;

import java.util.function.Supplier;

public final class MatrixUniforms {
	private MatrixUniforms() {
	}

	public static void addMatrixUniforms(UniformCreator uniforms, PackDirectives directives) {
		addMatrix(uniforms, "ModelView", CapturedRenderingState.INSTANCE::getGbufferModelView,CapturedRenderingState.INSTANCE::getGbufferModelViewLastFrame);
		// TODO: In some cases, gbufferProjectionInverse takes on a value much different than OptiFine...
		// We need to audit Mojang's linear algebra.
		addMatrix(uniforms, "Projection", CapturedRenderingState.INSTANCE::getGbufferProjection, CapturedRenderingState.INSTANCE::getGbufferProjectionLastFrame);
		addShadowMatrix(uniforms, "ModelView", () ->
			new Matrix4f(ShadowRenderer.createShadowModelView(directives.getSunPathRotation(), directives.getShadowDirectives().getIntervalSize()).last().pose()));
		addShadowMatrix(uniforms, "Projection", () -> ShadowMatrices.createOrthoMatrix(directives.getShadowDirectives().getDistance()));
	}

	private static void addMatrix(UniformCreator uniforms, String name, Supplier<Matrix4f> supplier, Supplier<Matrix4f> lastFrame) {
		uniforms
			.registerMatrix4fUniform(true, "gbuffer" + name, supplier)
			.registerMatrix4fUniform(true, "gbuffer" + name + "Inverse", new Inverted(supplier))
			.registerMatrix4fUniform(true, "gbufferPrevious" + name, lastFrame);
	}

	private static void addShadowMatrix(UniformCreator uniforms, String name, Supplier<Matrix4f> supplier) {
		uniforms
			.registerMatrix4fUniform(true, "shadow" + name, supplier)
			.registerMatrix4fUniform(true, "shadow" + name + "Inverse", new Inverted(supplier));
	}

	private static class Inverted implements Supplier<Matrix4f> {
		private final Supplier<Matrix4f> parent;

		Inverted(Supplier<Matrix4f> parent) {
			this.parent = parent;
		}

		@Override
		public Matrix4f get() {
			// PERF: Don't copy + allocate this matrix every time?
			Matrix4f copy = new Matrix4f(parent.get());

			copy.invert();

			return copy;
		}
	}

	private static class Previous implements Supplier<Matrix4f> {
		private final Supplier<Matrix4f> parent;
		private Matrix4f previous;

		Previous(Supplier<Matrix4f> parent) {
			this.parent = parent;
			this.previous = new Matrix4f();
		}

		@Override
		public Matrix4f get() {
			// PERF: Don't copy + allocate these matrices every time?
			Matrix4f copy = new Matrix4f(parent.get());
			Matrix4f previous = new Matrix4f(this.previous);

			this.previous = copy;

			return previous;
		}
	}
}
