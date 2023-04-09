package net.irisshaders.iris.gl.uniform.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.uniform.Uniform;
import net.irisshaders.iris.gl.uniform.UniformBuffer;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GL45C;

import java.util.LinkedHashMap;
import java.util.Map;

public class ActiveUniformBuffer implements UniformBuffer {
	private long[] address;
	private int[] id;
	private int size;
	private int offset;
	private boolean isDone;
	private LinkedHashMap<String, Uniform> uniforms = new LinkedHashMap<>();
	private int currentFence;

	public ActiveUniformBuffer() {

	}

	int frameId;

	@Override
	public void upload() {
		frameId = (frameId + 1) % SodiumClientMod.options().advanced.cpuRenderAheadLimit;
		GL45C.glBindBufferBase(GL43C.GL_UNIFORM_BUFFER, 1, id[frameId]);
		if (!isDone) {
			throw new IllegalStateException("Tried to upload a buffer that was not marked done!");
		}

		uniforms.values().forEach(uniform -> uniform.updateValue(address[frameId]));
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

		return id[0];
	}

	@Override
	public int register(Uniform uniform) {
		uniforms.put(uniform.getName(), uniform);
		offset += uniform.getByteSize();

		return offset;
	}

	private static int align(int bufferSize, int alignment) {
		return (((bufferSize - 1) + alignment) & -alignment);
	}

	public static void main(String[] args) {
		System.out.println(align(8, 16));
	}

	@Override
	public int getCurrentOffset(int byteSize, int alignment) {
		return offset = align(offset, alignment);
	}

	@Override
	public void done() {
		Iris.logger.warn("Size is " + offset);
		size = align(offset, GL45C.glGetInteger(GL45C.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT));

		isDone = true;

		id = new int[SodiumClientMod.options().advanced.cpuRenderAheadLimit + 1];

		GL45C.glGenBuffers(id);

		for (int id1 : id) {
			GlStateManager._glBindBuffer(GL43C.GL_UNIFORM_BUFFER, id1);
			IrisRenderSystem.bufferStorage(GL43C.GL_UNIFORM_BUFFER, size * ((long) SodiumClientMod.options().advanced.cpuRenderAheadLimit + 1L), GL45C.GL_MAP_WRITE_BIT | GL45C.GL_MAP_PERSISTENT_BIT | GL45C.GL_MAP_COHERENT_BIT);
		}

		address = new long[SodiumClientMod.options().advanced.cpuRenderAheadLimit + 1];

		for (int i = 0; i < SodiumClientMod.options().advanced.cpuRenderAheadLimit + 1; i++) {
			address[i] = GL45C.nglMapNamedBuffer(id[i], GL45C.GL_WRITE_ONLY);
		}
	}

	@Override
	public void delete() {
		for (int id1 : id) {
			GL45C.glUnmapNamedBuffer(id1);
			GL45C.glDeleteBuffers(id1);
		}

	}
}
