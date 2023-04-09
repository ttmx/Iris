package net.irisshaders.iris.gl.uniform;

import java.util.List;
import java.util.Map;

public interface UniformBuffer {

	/**
	 * Uploads the data in this buffer to the GPU.
	 */
	void upload();

	Map<String, Uniform> getUniforms();

	/**
	 * Binds this buffer to the given binding point.
	 */
	void bind(int bindingPoint);

	/**
	 * Unbinds this buffer from the given binding point.
	 */
	void unbind(int bindingPoint);

	/**
	 * Returns the size of this buffer in bytes.
	 */
	int getSize();

	/**
	 * Returns the ID of this buffer.
	 */
	int getId();

	/**
	 * Registers a uniform with this buffer.
	 * @return The offset of the uniform in this buffer.
	 */
	int register(Uniform uniform);

	/**
	 * Returns the current offset of this buffer, aligned to the byte size given.
	 */
	int getCurrentOffset(int alignment);

	/**
	 * Marks this buffer as done, and prevents any further uniforms from being registered.
	 */
	void done();

	void delete();
}
