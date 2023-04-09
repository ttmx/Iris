package net.irisshaders.iris.uniforms.custom;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import kroppeb.stareval.element.ExpressionElement;
import kroppeb.stareval.expression.Expression;
import kroppeb.stareval.expression.VariableExpression;
import kroppeb.stareval.function.FunctionContext;
import kroppeb.stareval.function.FunctionReturn;
import kroppeb.stareval.function.Type;
import kroppeb.stareval.parser.Parser;
import kroppeb.stareval.resolver.ExpressionResolver;
import net.irisshaders.iris.Iris;

import net.irisshaders.iris.gl.uniform.Uniform;
import net.irisshaders.iris.gl.uniform.UniformBuffer;
import net.irisshaders.iris.gl.uniform.UniformCreator;
import net.irisshaders.iris.parsing.IrisFunctions;
import net.irisshaders.iris.parsing.IrisOptions;
import net.irisshaders.iris.parsing.VectorType;
import net.irisshaders.iris.uniforms.custom.cached.CachedUniform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class CustomUniforms implements FunctionContext {
	private final Map<String, Variable> variables = new Object2ObjectLinkedOpenHashMap<>();
	private final Map<String, Expression> variablesExpressions = new Object2ObjectLinkedOpenHashMap<>();
	private final List<Uniform> uniforms = new ArrayList<>();
	private final List<String> uniformOrder;
	private final Map<Object, Object2IntMap<Uniform>> locationMap = new Object2ObjectOpenHashMap<>();
	private final Map<String, List<Variable>> dependsOn;
	private final Map<String, List<Variable>> requiredBy;
	private final UniformCreator creator;

	private CustomUniforms(UniformCreator creator, Map<String, Builder.Variable> variables) {
		this.creator = creator;

		ExpressionResolver resolver = new ExpressionResolver(
			IrisFunctions.functions,
			(name) -> {
				Type type = creator.getTypeForUniform(name);
				if (type != null)
					return type;
				Builder.Variable variable = variables.get(name);
				if (variable != null)
					return variable.type;
				return null;
			},
			true);

		for (Builder.Variable variable : variables.values()) {
			try {
				Expression expression = resolver.resolveExpression(variable.type, variable.expression);
				Variable cachedUniform = CachedUniform
					.forExpression(variable.name, variable.type, expression, this);
				this.addVariable(expression, cachedUniform);
				if (variable.uniform) {
					Uniform.convertVariable(creator, variable.type, cachedUniform, this);
				}
				//Iris.logger.info("Was able to resolve uniform " + variable.name + " = " + variable.expression);
			} catch (Exception e) {
				Iris.logger
					.warn("Failed to resolve uniform " + variable.name + ", reason: " + e
						.getMessage() + " ( = " + variable.expression + ")", e);
			}
		}

		{
			// toposort

			this.dependsOn = new Object2ObjectOpenHashMap<>();
			this.requiredBy = new Object2ObjectOpenHashMap<>();
			Object2IntMap<String> dependsOnCount = new Object2IntOpenHashMap<>();

			for (String input : creator.getUniforms().keySet()) {
				requiredBy.put(input, new ObjectArrayList<>());
			}

			for (Variable input : this.variables.values()) {
				requiredBy.put(input.getName(), new ObjectArrayList<>());
			}

			FunctionReturn functionReturn = new FunctionReturn();
			Set<VariableExpression> requires = new ObjectOpenHashSet<>();
			Set<Variable> brokenUniforms = new ObjectOpenHashSet<>();

			for (Map.Entry<String, Expression> entry : this.variablesExpressions.entrySet()) {
				requires.clear();

				entry.getValue().listVariables(requires);
				if (requires.isEmpty()) {
					continue;
				}

				Variable uniform = this.variables.get(entry.getKey());

				List<Variable> dependencies = new ArrayList<>();
				for (VariableExpression v : requires) {
					Expression evaluated = v.partialEval(this, functionReturn);
					if (evaluated instanceof Variable) {
						dependencies.add(((Variable) evaluated));
					} else {
						// we are depending on a broken uniform
						brokenUniforms.add(uniform);
					}
				}

				if (dependencies.isEmpty()) {
					// can be empty if we rely on broken uniforms
					continue;
				}

				dependsOn.put(uniform.getName(), dependencies);
				dependsOnCount.put(uniform.getName(), dependencies.size());

				for (Variable dependency : dependencies) {
					requiredBy.get(dependency.getName()).add(uniform);
				}
			}

			// actual toposort:
			List<String> ordered = new ObjectArrayList<>();
			List<String> free = new ObjectArrayList<>();

			// init
			for (String entry : requiredBy.keySet()) {
				if (!dependsOnCount.containsKey(entry)) {
					free.add(entry);
				}
			}

			while (!free.isEmpty()) {
				String pop = free.remove(free.size() - 1);
				if (!brokenUniforms.contains(pop)) {
					// only add those that aren't broken
					ordered.add(pop);
				} else {
					// mark all those that rely on use as broken too
					brokenUniforms.addAll(requiredBy.get(pop));
				}
				for (Variable dependent : requiredBy.get(pop)) {
					int count = dependsOnCount.mergeInt(dependent.getName(), -1, Integer::sum);
					assert count >= 0;
					if (count == 0) {
						free.add(dependent.getName());
						dependsOnCount.removeInt(dependent.getName());
					}
				}
			}

			if (!brokenUniforms.isEmpty()) {
				Iris.logger.warn(
					"The following uniforms won't work, either because they are broken, or reference a broken uniform: \n" +
						brokenUniforms.stream().map(Variable::getName).collect(Collectors.joining(", ")));
			}

			if (!dependsOnCount.isEmpty()) {
				throw new IllegalStateException("Circular reference detected between: " +
					dependsOnCount.object2IntEntrySet()
						.stream()
						.map(entry -> entry.getKey() + " (" + entry.getIntValue() + ")")
						.collect(Collectors.joining(", "))
				);
			}

			this.uniformOrder = ordered;
		}
	}

	private void addVariable(Expression expression, Variable uniform) throws Exception {
		String name = uniform.getName();
		if (this.variables.containsKey(name))
			throw new Exception("Duplicated variable: " + name);
		if (this.creator.getTypeForUniform(name) != null)
			throw new Exception("Variable shadows build in uniform: " + name);

		this.variables.put(name, uniform);
		this.variablesExpressions.put(name, expression);
	}

	@Override
	public boolean hasVariable(String name) {
		return this.creator.getTypeForUniform(name) != null || this.variables.containsKey(name);
	}

	@Override
	public Expression getVariable(String name) {
		// TODO: Make the simplify just return these ones
		final Uniform inputUniform = this.creator.getUniforms().get(name);
		if (inputUniform != null)
			return (VariableExpression) (context, functionReturn) -> {
				inputUniform.writeTo(functionReturn);
			};
		final Variable customUniform = this.variables.get(name);
		if (customUniform != null)
			return customUniform;
		throw new RuntimeException("Unknown variable: " + name);
	}

	public static class Builder {
		final private static Map<String, Type> types = new ImmutableMap.Builder<String, Type>()
			.put("bool", Type.Boolean)
			.put("float", Type.Float)
			.put("int", Type.Int)
			.put("vec2", VectorType.VEC2)
			.put("vec3", VectorType.VEC3)
			.put("vec4", VectorType.VEC4)
			.build();
		Map<String, Variable> variables = new Object2ObjectLinkedOpenHashMap<>();

		public void addVariable(String type, String name, String expression, boolean isUniform) {
			if (variables.containsKey(name)) {
				Iris.logger.warn("Ignoring duplicated custom uniform name: " + name);
				return;
			}

			Type parsedType = types.get(type);
			if (parsedType == null) {
				Iris.logger.warn("Ignoring invalid uniform type: " + type + " of " + name);
				return;
			}

			try {
				ExpressionElement ast = Parser.parse(expression, IrisOptions.options);
				variables.put(name, new Variable(parsedType, name, ast, isUniform));
			} catch (Exception e) {
				Iris.logger.warn("Failed to parse custom variable/uniform", e);
			}
		}

		public final CustomUniforms build(
			UniformCreator creator
		) {
			return new CustomUniforms(creator, this.variables);
		}

		private static class Variable {
			final public Type type;
			final public String name;
			final public ExpressionElement expression;
			final public boolean uniform;

			public Variable(Type type, String name, ExpressionElement expression, boolean uniform) {
				this.type = type;
				this.name = name;
				this.expression = expression;
				this.uniform = uniform;
			}
		}


	}
}
