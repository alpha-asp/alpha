package at.ac.tuwien.kr.alpha.common.interpretations;

import at.ac.tuwien.kr.alpha.common.terms.Constant;
import at.ac.tuwien.kr.alpha.common.terms.Term;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public class ExternalBindingMethodPredicate extends FixedInterpretation {
	private final Method method;

	public ExternalBindingMethodPredicate(Method method) {
		if (!method.getReturnType().equals(Set.class)) {
			throw new IllegalArgumentException("method must return Set");
		}

		this.method = method;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<List<Constant>> evaluate(List<Term> terms) {
		if (terms.size() != method.getParameterCount()) {
			throw new IllegalArgumentException(
				"Parameter count mismatch when calling " + method.getName() + ". " +
					"Expected " + method.getParameterCount() + " parameters but got " + terms.size() + "."
			);
		}

		final Class<?>[] parameterTypes = method.getParameterTypes();

		final Object[] arguments = new Object[terms.size()];

		for (int i = 0; i < arguments.length; i++) {
			if (!(terms.get(i) instanceof Constant)) {
				throw new IllegalArgumentException(
					"Expected only constants as input for " + method.getName() + ", but got " +
						"something else at position " + i + "."
				);
			}

			arguments[i] = ((Constant) terms.get(i)).getSymbol();

			final Class<?> expected = parameterTypes[i];
			final Class<?> actual = arguments[i].getClass();

			if (expected.isAssignableFrom(actual)) {
				continue;
			}

			if (expected.isPrimitive() && ClassUtils.primitiveToWrapper(expected).isAssignableFrom(actual)) {
				continue;
			}

			throw new IllegalArgumentException(
				"Parameter type mismatch when calling " + method.getName() +
					" at position " + i + ". Expected " + expected + " but got " +
					actual + "."
			);
		}

		try {
			return (Set) method.invoke(null, arguments);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}