package com.groovy.orient.document

import com.groovy.orient.util.ASTUtil
import com.orientechnologies.orient.core.record.impl.ODocument
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import static org.codehaus.groovy.ast.tools.GeneralUtils.*

/**
 * Transformation for OrientDB document usage
 *
 * @author @eugenekamenev
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class OrientDocumentTransformation extends AbstractASTTransformation {

    static final ClassNode document = ClassHelper.make(ODocument).plainNodeReference

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        AnnotationNode annotation = (AnnotationNode) nodes[0];
        ClassNode annotatedClass = (ClassNode) nodes[1];
        String clusterName = ASTUtil.parseValue(annotation.members.value, annotatedClass.nameWithoutPackage)
        List<String> transients = ASTUtil.parseValue(annotatedClass.getField('transients')?.initialExpression, []) as List<String>
        def fields = annotatedClass.fields.findAll {
            !(it.name in transients) && !(it.static) && (it.modifiers != ACC_TRANSIENT) && (it.name != 'document')
        }
        fields.each {
            createPropertyGetter(annotatedClass, it)
            createPropertySetter(annotatedClass, it)
            ASTUtil.removeProperty(annotatedClass, it.name)
        }
        createConstructors(annotatedClass, clusterName)
        def mapping = annotatedClass.getField('mapping')
        println mapping.initialExpression
        ASTUtil.removeProperty(annotatedClass, 'mapping')
    }

    private void createConstructors(ClassNode classNode, String orientCluster) {
        def initStatement = stmt(callThisX('setDocument', ctorX(document, constX(orientCluster))))
        def emptyConstructor = new ConstructorNode(ACC_PUBLIC, initStatement)
        def params = params(param(document, 'document'))
        def initStatementDocument = stmt(callThisX('setDocument', varX(params[0])))
        def documentConstructor = new ConstructorNode(ACC_PUBLIC, params, [] as ClassNode[], initStatementDocument)
        classNode.addConstructor(emptyConstructor)
        classNode.addConstructor(documentConstructor)
    }

    private void createPropertyGetter(ClassNode clazzNode, FieldNode field) {
        def getterStatement = returnS(castX(field.type, callX(callThisX('getDocument'), 'field', args(constX(field.name)))))
        def method = new MethodNode("get${field.name.capitalize()}", ACC_PUBLIC, field.type, [] as Parameter[], [] as ClassNode[], getterStatement)
        clazzNode.addMethod(method)
    }

    private void createPropertySetter(ClassNode clazzNode, FieldNode field) {
        def setterParam = param(field.type, field.name)
        def setterVar = varX(setterParam)
        def setterStatement = stmt(callX(callThisX('getDocument'), 'field', args(constX(field.name), setterVar)))
        def method = new MethodNode("set${field.name.capitalize()}", ACC_PUBLIC, ClassHelper.VOID_TYPE, params(setterParam), [] as ClassNode[], setterStatement)
        clazzNode.addMethod(method)
    }


}
