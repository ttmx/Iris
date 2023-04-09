package net.irisshaders.iris.gl.uniform.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.uniform.Uniform;
import net.irisshaders.iris.gl.uniform.UniformBuffer;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GL45C;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ActiveUniformBuffer implements UniformBuffer {
	private long address;
	private int id;
	private int size;
	private int offset;
	private boolean isDone;
	private LinkedHashMap<String, Uniform> uniforms = new LinkedHashMap<>();

	public ActiveUniformBuffer() {

	}

	@Override
	public void upload() {
		if (isDone) {
			throw new IllegalStateException("Tried to add a uniform to a buffer that was marked done!");
		}

		uniforms.values().forEach(uniform -> uniform.updateValue(address));
	}

	@Override
	public Map<String, Uniform> getUniforms() {
		return uniforms;
	}

	@Override
	public void bind(int bindingPoint) {

	}

	@Override
	public void unbind(int bindingPoint) {

	}

	@Override
	public int getSize() {
		if (!isDone) {
			throw new IllegalStateException("Tried to access a size of a buffer that was not marked done!");
		}

		return size;
	}

	@Override
	public int getId() {
		if (!isDone) {
			throw new IllegalStateException("Tried to get the ID of a buffer that was not marked done!");
		}

		return id;
	}

	@Override
	public int register(Uniform uniform) {
		uniforms.put(uniform.getName(), uniform);
		offset += uniform.getByteSize();
		return offset;
	}

	private int align(int bufferSize, int alignment) {
		return (((bufferSize - 1) + alignment) & -alignment);
	}

	@Override
	public int getCurrentOffset(int alignment) {
		return align(offset, alignment);
	}

	@Override
	public void done() {
		size = offset;

		isDone = true;

		id = GlStateManager._glGenBuffers();

		GlStateManager._glBindBuffer(GL43C.GL_UNIFORM_BUFFER, id);
		IrisRenderSystem.bufferStorage(GL43C.GL_UNIFORM_BUFFER, size, GL45C.GL_MAP_WRITE_BIT | GL45C.GL_MAP_PERSISTENT_BIT | GL45C.GL_MAP_COHERENT_BIT);

		address = GL45C.nglMapNamedBuffer(id, GL45C.GL_WRITE_ONLY);
	}

	@Override
	public void delete() {
		GL45C.glUnmapNamedBuffer(id);

		GL45C.glDeleteBuffers(id);
	}
}
