package net.irisshaders.iris.pipeline.clearing;

import org.joml.Vector4f;

public class ClearPassInformation {
	private final Vector4f color;
	private final int width;
	private final int height;

	public ClearPassInformation(Vector4f vector4f, int width, int height) {
		this.color = vector4f;
		this.width = width;
		this.height = height;
	}

	public Vector4f getColor() {
		return color;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ClearPassInformation information)) {
			return false;
		}

		return information.color.equals(this.color) && information.height == this.height && information.width == this.width;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (color == null ? 1 : color.hashCode());
		result = prime * result + width;
		result = prime * result + height;
		return result;
	}

	@Override
	public String toString() {
		return " Color: " + color + " height " + height + " width " + width;
	}
}
