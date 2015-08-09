package com.github.eugene.kamenev.orient.ast.util

import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement

/**
 * Groovy ASTUtils
 *
 * @author @eugenekamenev
 * @since 0.1.0
 */
@CompileStatic
class ASTUtil {

    static List<ClassNode> collectionNodes = [List, Set, Map, Collection].collect {
        ClassHelper.make(it).plainNodeReference
    }

    static List<ClassNode> simpleTypes = [Integer, Double, Float, String, Boolean, Long, BigDecimal, Byte, Character, Short, Date].collect {
        ClassHelper.make(it).plainNodeReference
    }

    /**
     * Completely remove property from class
     * @since 0.1.0
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
     * @since 0.1.0
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
            return (T) constant.expressions?.collect {(it as ConstantExpression).value}
        }
        if (constant instanceof ConstantExpression) {
            return (T) constant.value
        }
        defaultValue
    }

    /**
     * Parse value from expression
     * @since 0.1.0
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
            return (T) expression.expressions?.collect {(it as ConstantExpression).value}
        }
        if (expression instanceof ListExpression) {
            return (T) expression.expressions?.collect {parseValue(it, null)}
        }
        if (expression instanceof VariableExpression) {
            return (T) expression.name
        }
        if (expression instanceof ConstantExpression) {
            return (T) expression.value
        }
        defaultValue
    }

    public
    static MethodCallExpression findMethodCallExpression(String methodName, BlockStatement code) {
        if (code) {
            for (statement in code.statements) {
                def expr = ((statement as ExpressionStatement).expression as MethodCallExpression)
                if (parseValue(expr.method) == methodName) {
                    return expr
                }
            }
        }
        return null
    }

    public static void eachMethodArg(MethodCallExpression methodCall,
                                     @ClosureParams(value = FromString, options = ['org.codehaus.groovy.ast.expr.MapEntryExpression'])
                                             Closure closure) {
        if (methodCall) {
            def args = (methodCall.arguments as TupleExpression).expressions.first() as NamedArgumentListExpression
            for (arg in args.mapEntryExpressions) {
                closure.call(arg)
            }
        }
    }
}