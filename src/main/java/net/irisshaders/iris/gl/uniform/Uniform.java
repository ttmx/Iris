package net.irisshaders.iris.gl.uniform;

import kroppeb.stareval.function.FunctionReturn;
import kroppeb.stareval.function.Type;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.parsing.VectorType;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.irisshaders.iris.uniforms.custom.Variable;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public abstract class Uniform {

	private final boolean updatePerFrame;
	private final String name;
	private final int byteSize;
	private final int alignment;
	private boolean hasUpdated;

	public Uniform(boolean updatePerFrame, String name, int byteSize, int alignment) {
		this.updatePerFrame = updatePerFrame;
		this.name = name;
		this.byteSize = byteSize;
		this.alignment = alignment;
	}

	public static void convertVariable(UniformCreator creator, Type type, Variable cachedUniform, CustomUniforms customUniforms) {
		FunctionReturn functionReturn = new FunctionReturn();
		if (type.equals(Type.Boolean)) {
			creator.registerBooleanUniform(true, cachedUniform.getName(), () -> {
				cachedUniform.evaluateTo(customUniforms, functionReturn);
				return functionReturn.booleanReturn;
			});
		} else if (type.equals(Type.Int)) {
			creator.registerIntegerUniform(true, cachedUniform.getName(), () -> {
				cachedUniform.evaluateTo(customUniforms, functionReturn);
				return functionReturn.intReturn;
			});
		} else if (type.equals(Type.Float)) {
			creator.registerFloatUniform(true, cachedUniform.getName(), () -> {
				cachedUniform.evaluateTo(customUniforms, functionReturn);
				return functionReturn.floatReturn;
			});
		} else if (type.equals(VectorType.VEC2)) {
			creator.registerVector2Uniform(true, cachedUniform.getName(), () -> {
				cachedUniform.evaluateTo(customUniforms, functionReturn);
				return (Vector2f) functionReturn.objectReturn;
			});
		} else if (type.equals(VectorType.VEC3)) {
			creator.registerVector3Uniform(true, cachedUniform.getName(), () -> {
				cachedUniform.evaluateTo(customUniforms, functionReturn);
				return (Vector3f) functionReturn.objectReturn;
			});
		} else if (type.equals(VectorType.VEC4)) {
			creator.registerVector4Uniform(true, cachedUniform.getName(), () -> {
				cachedUniform.evaluateTo(customUniforms, functionReturn);
				return (Vector4f) functionReturn.objectReturn;
			});
		}
	}

	public String getName() {
		return name;
	}

	public int getByteSize() {
		return byteSize;
	}

	public int getAlignment() {
		return alignment;
	}

	public void updateValue(long address) {
		//if (!updatePerFrame && hasUpdated) return;

		hasUpdated = true;

		upload(address);
	}

	public abstract void upload(long address);

	public abstract void writeTo(FunctionReturn functionReturn);

	public abstract Type getType();
}
