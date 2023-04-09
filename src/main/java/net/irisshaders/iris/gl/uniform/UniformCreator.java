package net.irisshaders.iris.gl.uniform;

import kroppeb.stareval.function.FunctionReturn;
import kroppeb.stareval.function.Type;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.image.ImageLimits;
import net.irisshaders.iris.helpers.FloatSupplier;
import net.irisshaders.iris.parsing.MatrixType;
import net.irisshaders.iris.parsing.VectorType;
import net.irisshaders.iris.uniforms.custom.cached.CachedUniform;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class UniformCreator {
	private final UniformBuffer buffer;
	private Map<String, Type> uniformType = new HashMap<>();

	public UniformCreator(UniformBuffer buffer) {
		this.buffer = buffer;
	}

	public UniformCreator registerBooleanUniform(boolean updatePerFrame, String name, BooleanSupplier supplier) {
		int offset = buffer.getCurrentOffset(4, 4);

		uniformType.put(name, Type.Boolean);
		buffer.register(new Uniform(updatePerFrame, name, 4, 4) {
			@Override
			public void upload(long address) {

				MemoryUtil.memPutInt(address + offset, supplier.getAsBoolean() ? 1 : 0);
			}

			@Override
			public void writeTo(FunctionReturn functionReturn) {
				functionReturn.booleanReturn = supplier.getAsBoolean();
			}

			@Override
			public Type getType() {
				return Type.Boolean;
			}
		});

		return this;
	}

	public UniformCreator registerIntegerUniform(boolean updatePerFrame, String name, IntSupplier supplier) {
		int offset = buffer.getCurrentOffset(4, 4);

		uniformType.put(name, Type.Int);
		buffer.register(new Uniform(updatePerFrame, name, 4, 4) {
			@Override
			public void upload(long address) {


				MemoryUtil.memPutInt(address + offset, supplier.getAsInt());
			}

			@Override
			public void writeTo(FunctionReturn functionReturn) {
				functionReturn.intReturn = supplier.getAsInt();
			}

			@Override
			public Type getType() {
				return Type.Int;
			}
		});

		return this;
	}

	public UniformCreator registerFloatUniform(boolean updatePerFrame, String name, FloatSupplier supplier) {
		int offset = buffer.getCurrentOffset(4, 4);

		uniformType.put(name, Type.Float);
		buffer.register(new Uniform(updatePerFrame, name, 4, 4) {
			@Override
			public void upload(long address) {


				MemoryUtil.memPutFloat(address + offset, supplier.getAsFloat());
			}

			@Override
			public void writeTo(FunctionReturn functionReturn) {
				functionReturn.floatReturn = supplier.getAsFloat();
			}

			@Override
			public Type getType() {
				return Type.Float;
			}
		});

		return this;
	}

	public UniformCreator registerVector2Uniform(boolean updatePerFrame, String name, Supplier<Vector2f> supplier) {
		int offset = buffer.getCurrentOffset(8, 8);

		uniformType.put(name, VectorType.VEC2);
		buffer.register(new Uniform(updatePerFrame, name, 8, 8) {
			@Override
			public void upload(long address) {
				if (supplier.get() == null) return;


				MemoryUtil.memPutFloat(address + offset, supplier.get().x);
				MemoryUtil.memPutFloat(address + offset + 4, supplier.get().y);
			}

			@Override
			public void writeTo(FunctionReturn functionReturn) {
				functionReturn.objectReturn = supplier.get();
			}

			@Override
			public Type getType() {
				return VectorType.VEC2;
			}
		});

		return this;
	}

	public UniformCreator registerVector2IntegerUniform(boolean updatePerFrame, String name, Supplier<Vector2i> supplier) {
		int offset = buffer.getCurrentOffset(8, 8);

		uniformType.put(name, VectorType.I_VEC2);
		buffer.register(new Uniform(updatePerFrame, name, 8, 8) {
			@Override
			public void upload(long address) {
				if (supplier.get() == null) return;


				MemoryUtil.memPutInt(address + offset, supplier.get().x);
				MemoryUtil.memPutInt(address + offset + 4, supplier.get().y);
			}

			@Override
			public void writeTo(FunctionReturn functionReturn) {
				functionReturn.objectReturn = supplier.get();
			}

			@Override
			public Type getType() {
				return VectorType.I_VEC2;
			}
		});

		return this;
	}

	public UniformCreator registerVector3Uniform(boolean updatePerFrame, String name, Supplier<Vector3f> supplier) {
		int offset = buffer.getCurrentOffset(12, 16);

		uniformType.put(name, VectorType.VEC3);
		buffer.register(new Uniform(updatePerFrame, name, 12, 16) {
			@Override
			public void upload(long address) {
				if (supplier.get() == null) return;


				MemoryUtil.memPutFloat(address + offset, supplier.get().x);
				MemoryUtil.memPutFloat(address + offset + 4, supplier.get().y);
				MemoryUtil.memPutFloat(address + offset + 8, supplier.get().z);
			}

			@Override
			public void writeTo(FunctionReturn functionReturn) {
				functionReturn.objectReturn = supplier.get();
			}

			@Override
			public Type getType() {
				return VectorType.VEC3;
			}
		});

		return this;
	}

	public UniformCreator registerVector4Uniform(boolean updatePerFrame, String name, Supplier<Vector4f> supplier) {
		int offset = buffer.getCurrentOffset(16, 16);

		uniformType.put(name, VectorType.VEC4);
		buffer.register(new Uniform(updatePerFrame, name, 16, 16) {
			@Override
			public void upload(long address) {
				if (supplier.get() == null) return;


				MemoryUtil.memPutFloat(address + offset, supplier.get().x);
				MemoryUtil.memPutFloat(address + offset + 4, supplier.get().y);
				MemoryUtil.memPutFloat(address + offset + 8, supplier.get().z);
				MemoryUtil.memPutFloat(address + offset + 12, supplier.get().w);
			}

			@Override
			public void writeTo(FunctionReturn functionReturn) {
				functionReturn.objectReturn = supplier.get();
			}

			@Override
			public Type getType() {
				return VectorType.VEC4;
			}
		});

		return this;
	}

	public UniformCreator registerMatrix4fUniform(boolean updatePerFrame, String name, Supplier<Matrix4f> supplier) {
		int offset = buffer.getCurrentOffset(64, 16);

		uniformType.put(name, MatrixType.MAT4);
		buffer.register(new Uniform(updatePerFrame, name, 64, 16) {
			@Override
			public void upload(long address) {
				if (supplier.get() == null) return;


				supplier.get().getToAddress(address + offset);
			}

			@Override
			public void writeTo(FunctionReturn functionReturn) {
				functionReturn.objectReturn = supplier.get();
			}

			@Override
			public Type getType() {
				return MatrixType.MAT4;
			}
		});

		return this;
	}

	public Type getTypeForUniform(String name) {
		return uniformType.get(name);
	}

	public Map<String, Uniform> getUniforms() {
		return buffer.getUniforms();
	}

	public String getLayout() {
		StringBuilder builder = new StringBuilder();

		builder.append("layout (std140, binding = 1) uniform CommonUniforms {\n");
		buffer.getUniforms().keySet().forEach(uniformInformation -> builder.append(uniformType.get(uniformInformation).toString().toLowerCase(Locale.ROOT)).append(" ").append(uniformInformation).append(";").append("\n"));


		builder.append("};");

		return builder.toString();
	}

	public void done() {
		buffer.done();
	}

	public void newFrame() {
		buffer.upload();
	}
}
