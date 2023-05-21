package net.coderbot.iris.pipeline;

import net.coderbot.iris.rendertarget.RenderTarget;
import net.coderbot.iris.vendored.joml.Vector4f;

import java.util.Objects;

public class ClearPassInformation {
	private final Vector4f color;
	private final RenderTarget target;
	private final int width;
	private final int height;

	public ClearPassInformation(Vector4f vector4f, RenderTarget target, int width, int height) {
		this.color = vector4f;
		this.target = target;
		this.width = width;
		this.height = height;
	}

	public Vector4f getColor() {
		return color;
	}

	public RenderTarget getTarget() {
		return target;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ClearPassInformation)) {
			return false;
		}

		ClearPassInformation information = (ClearPassInformation) obj;

		return information.color.equals(this.color) && information.target.equals(this.target) && information.height == this.height && information.width == this.width;
	}

	@Override
	public int hashCode() {
		return Objects.hash(color, target, height, width);
	}
}
