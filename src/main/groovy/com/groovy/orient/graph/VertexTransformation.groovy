package com.groovy.orient.graph

import com.syncleus.ferma.AbstractVertexFrame
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import static org.codehaus.groovy.ast.tools.GeneralUtils.*

/**
 * @author @eugenekamenev
 */
@CompileStatic
@GroovyASTTransformation(phase=CompilePhase.SEMANTIC_ANALYSIS)
class VertexTransformation extends AbstractASTTransformation {

	static final ClassNode vertexNode = ClassHelper.make(AbstractVertexFrame).plainNodeReference

	@Override
	void visit(ASTNode[] nodes, SourceUnit source) {
		AnnotationNode annotation = (AnnotationNode) nodes[0];
		ClassNode annotatedClass = (ClassNode) nodes[1];
		annotatedClass.setSuperClass(vertexNode)
		annotatedClass.fields.findAll {
			createPropertyGetter(annotatedClass, it)
			createPropertySetter(annotatedClass, it)
		}/*.each {
			removeProperty(annotatedClass, it.name)
		}*/
		def mapping = annotatedClass.getField('mapping')
		removeProperty(annotatedClass, 'mapping')
	}

	private void createPropertyGetter(ClassNode clazzNode, FieldNode field) {
		def getterStatement = returnS(castX(field.type, callThisX('getProperty', constX(field.name))))
		def method = new MethodNode("get${field.name.capitalize()}", ACC_PUBLIC, field.type, [] as Parameter[], [] as ClassNode[], getterStatement)
		clazzNode.addMethod(method)
	}

	private void createPropertySetter(ClassNode clazzNode, FieldNode field) {
		def setterParam = param(field.type, field.name)
		def setterVar = varX(setterParam)
		def setterStatement = stmt(callThisX('setProperty', args(constX(field.name), setterVar)))
		def method = new MethodNode("set${field.name.capitalize()}", ACC_PUBLIC, ClassHelper.VOID_TYPE, params(setterParam), [] as ClassNode[], setterStatement)
		clazzNode.addMethod(method)
	}

	private void removeProperty(ClassNode classNode, String propertyName) {
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
}
