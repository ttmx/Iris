package net.irisshaders.iris.gl.uniform.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.arena.staging.MappedStagingBuffer;
import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferMapFlags;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferStorageFlags;
import me.jellysquid.mods.sodium.client.gl.buffer.GlImmutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.util.EnumBitField;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.uniform.Uniform;
import net.irisshaders.iris.gl.uniform.UniformBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.MemoryUtil;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.lwjgl.opengl.ARBDirectStateAccess.*;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL42.GL_BUFFER_UPDATE_BARRIER_BIT;
import static org.lwjgl.opengl.GL42C.glMemoryBarrier;
import static org.lwjgl.opengl.GL44.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT;

public class ActiveUniformBuffer implements UniformBuffer {
	private int frames;
	private long address;
	private GlBuffer buff;
	private int size;
	private int offset;
	private boolean isDone;
	private LinkedHashMap<String, Uniform> uniforms = new LinkedHashMap<>();
	private int currentFence;

	private final CommandList cl;
	private final StagingBuffer sb;

	public ActiveUniformBuffer() {
		RenderDevice.enterManagedCode();
		cl = RenderDevice.INSTANCE.createCommandList();
		sb = new MappedStagingBuffer(cl);
		RenderDevice.exitManagedCode();
	}

	int frameId;

	@Override
	public void upload() {
		uniforms.values().forEach(uniform -> uniform.updateValue(address));
		sb.enqueueCopy(cl, MemoryUtil.memByteBuffer(address, size), buff, 0);
		sb.flush(cl);
		sb.flip();
		GL45C.glBindBufferRange(GL43C.GL_UNIFORM_BUFFER, 1, buff.handle(),0, size);
		if (!isDone) {
			throw new IllegalStateException("Tried to upload a buffer that was not marked done!");
		}

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

		return buff.handle();
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
		size = align(offset, 16);

		isDone = true;

		frames = SodiumClientMod.options().advanced.cpuRenderAheadLimit + 1;
		buff = cl.createImmutableBuffer(size, EnumBitField.of(GlBufferStorageFlags.PERSISTENT, GlBufferStorageFlags.CLIENT_STORAGE, GlBufferStorageFlags.MAP_WRITE));
		address = MemoryUtil.nmemAlloc(size);

	}

	@Override
	public void delete() {
		MemoryUtil.nmemFree(address);
		GlStateManager._glBindBuffer(GL32C.GL_UNIFORM_BUFFER, 0);
		RenderDevice.enterManagedCode();
		cl.deleteBuffer(buff);
		cl.flush();
		cl.close();
		RenderDevice.exitManagedCode();
	}
}

















































