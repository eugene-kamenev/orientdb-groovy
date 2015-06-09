package com.groovy.orient.document.util

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.ArrayExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.VariableExpression

/**
 * Simple, but useful AST Utils
 * @author @eugenekamenev
 */
@CompileStatic
class ASTUtil {

	/**
	 * Completely remove property from class
	 *
	 * @param classNode
	 * @param propertyName
	 */
	public static void removeProperty(ClassNode classNode, String propertyName) {
		for (int i = 0; i < classNode.fields.size(); i++) {
			if (classNode.fields[i].name == propertyName) {
				classNode.fields.remove(i)
				break
			}
		}
		for (int i = 0; i < classNode.properties.size(); i++) {
			if (classNode.properties[i].name == propertyName) {
				classNode.properties.remove(i)
				break
			}
		}
	}

	/**
	 * Get annotation value
	 *
	 * @param constant
	 * @param defaultValue
	 * @return
	 */
	public static <T> T annotationValue(constant, T defaultValue = null) {
		if (!constant) {
			return (T) defaultValue
		}
		if (constant instanceof ClassExpression) {
			return (T) constant.type
		}
		if (constant instanceof ArrayExpression) {
			return (T) constant.expressions?.collect { (it as ConstantExpression).value }
		}
		if (constant instanceof ConstantExpression) {
			return (T) constant.value
		}
		return defaultValue
	}

	/**
	 *  Parse value from expression
	 *
	 * @param expression
	 * @param defaultValue
	 * @return
	 */
	public static <T> T parseValue(expression, T defaultValue = null) {
		if (!expression) {
			return (T) defaultValue
		}
		if (expression instanceof ArrayExpression) {
			return (T) expression.expressions?.collect { (it as ConstantExpression).value }
		}
		if (expression instanceof ListExpression) {
			return (T) expression.expressions?.collect { parseValue(it, null) }
		}
		if (expression instanceof VariableExpression) {
			return (T) expression.name
		}
		if (expression instanceof ConstantExpression) {
			return (T) expression.value
		}
	}
}