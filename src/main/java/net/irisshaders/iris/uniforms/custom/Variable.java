package net.irisshaders.iris.uniforms.custom;

import kroppeb.stareval.expression.Expression;
import kroppeb.stareval.expression.VariableExpression;
import kroppeb.stareval.function.FunctionContext;
import kroppeb.stareval.function.FunctionReturn;

import java.util.Collection;

public class Variable implements VariableExpression {
	private final String name;
	private final Expression expression;
	private final FunctionContext context;
	private final FunctionReturn held;

	public Variable(String name, Expression expression, FunctionContext context, FunctionReturn held) {
		this.name = name;
		this.expression = expression;
		this.context = context;
		this.held = held;
	}

	@Override
	public void evaluateTo(FunctionContext context, FunctionReturn functionReturn) {
		this.expression.evaluateTo(context, functionReturn);
	};

	public String getName() {
		return name;
	}
}
