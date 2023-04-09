package net.irisshaders.iris.uniforms.custom.cached;

import kroppeb.stareval.expression.Expression;
import kroppeb.stareval.expression.VariableExpression;
import kroppeb.stareval.function.FunctionContext;
import kroppeb.stareval.function.FunctionReturn;
import kroppeb.stareval.function.Type;
import net.irisshaders.iris.gl.uniform.Uniform;
import net.irisshaders.iris.parsing.VectorType;
import net.irisshaders.iris.uniforms.custom.Variable;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public abstract class CachedUniform implements VariableExpression {
	static public Variable forExpression(String name, Type type, Expression expression, FunctionContext context) {
		final FunctionReturn held = new FunctionReturn();
		return new Variable(name, expression, context, held);
	}
}
