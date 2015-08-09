package com.github.eugene.kamenev.orient.graph

import com.github.eugene.kamenev.orient.ast.util.ASTUtil
import com.tinkerpop.blueprints.Direction
import com.tinkerpop.blueprints.impls.orient.OrientEdge
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.DelegateASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import static org.codehaus.groovy.ast.tools.GeneralUtils.*

/**
 * OrientDB entity transformation for @see Edge annotation
 *
 * @author @eugenekamenev
 * @since 0.1.0
 *
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class EdgeTransformation extends AbstractASTTransformation {
    static final ClassNode orientEdgeClassNode = ClassHelper.make(OrientEdge).plainNodeReference
    static final ClassNode delegateNode = ClassHelper.make(Delegate).plainNodeReference
    static final ClassNode directionNode = ClassHelper.make(Direction).plainNodeReference

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        AnnotationNode annotation = (AnnotationNode) nodes[0];
        ClassNode annotatedClass = (ClassNode) nodes[1];
        def inNode = ASTUtil.annotationValue(annotation.members.from) as ClassNode
        def outNode = ASTUtil.annotationValue(annotation.members.to) as ClassNode
        def edgeFieldNode = annotatedClass.addField('edge', ACC_PUBLIC | ACC_FINAL, orientEdgeClassNode, new EmptyExpression())
        List<String> transients = ASTUtil.parseValue(annotatedClass.getField('transients')?.initialExpression, []) as List<String>
        def mappingClosure = annotatedClass.getField('mapping')
        def mapping = [:]
        if (mappingClosure) {
            mapping = createEntityMappingMap(annotatedClass, mappingClosure?.initialExpression as ClosureExpression)
        }
        createConstructrors(annotatedClass, edgeFieldNode)
        createInOut(annotatedClass, edgeFieldNode, inNode, outNode)
        FieldNode edgeField
        def fields = annotatedClass.fields.findAll {
            if (it.name == 'edge') {
                edgeField = it
                def annotationNode = new AnnotationNode(delegateNode)
                def delegateTransformation = new DelegateASTTransformation()
                delegateTransformation.visit([annotationNode, it] as ASTNode[], source)
            }
            !(it.name in transients) && !(it.static) && (it.modifiers != ACC_TRANSIENT) && !(it.name == 'edge')
        }
        fields.each {
            createPropertyGetter(annotatedClass, it, edgeField, mapping)
            createPropertySetter(annotatedClass, it, edgeField, mapping)
            ASTUtil.removeProperty(annotatedClass, it.name)
        }
        // clean up
        ASTUtil.removeProperty(annotatedClass, 'mapping')
        ASTUtil.removeProperty(annotatedClass, 'transients')
    }

    private static void createInOut(ClassNode clazzNode, FieldNode edgeNode, ClassNode inNode, ClassNode outNode) {
        def inStatement = returnS(ctorX(inNode.plainNodeReference, args(callX(varX(edgeNode), 'getVertex', args(propX(classX(directionNode), 'IN'))))))
        def inMethod = clazzNode.addMethod('getIn', ACC_PUBLIC, inNode.plainNodeReference, [] as Parameter[], [] as ClassNode[], inStatement)
        def outStatement = returnS(ctorX(outNode.plainNodeReference, args(callX(varX(edgeNode), 'getVertex', args(propX(classX(directionNode), 'OUT'))))))
        def outMethod = clazzNode.addMethod('getOut', ACC_PUBLIC, outNode.plainNodeReference, [] as Parameter[], [] as ClassNode[], outStatement)
    }

    private static void createConstructrors(ClassNode clazzNode, FieldNode edgeFieldNode) {
        def edgeParams = params(param(orientEdgeClassNode, 'edge1'))
        def initStatement = stmt(assignX(varX(edgeFieldNode), ctorX(orientEdgeClassNode)))
        def emptyConstructor = new ConstructorNode(ACC_PUBLIC, initStatement)
        def initStatementEdge = stmt(assignX(varX(edgeFieldNode), varX(edgeParams[0])))
        def edgeConstructor = new ConstructorNode(ACC_PUBLIC, edgeParams, [] as ClassNode[], initStatementEdge)
        clazzNode.addConstructor(emptyConstructor)
        clazzNode.addConstructor(edgeConstructor)
    }

    private static Map<String, Map> createEntityMappingMap(ClassNode classNode, ClosureExpression expression) {
        def mapping = [:]
        if (!expression) {
            return mapping
        }
        def block = expression.code as BlockStatement
        block.statements.each {
            parseMappingExpression(classNode, (it as ExpressionStatement).expression as MethodCallExpression, mapping)
        }
        mapping
    }

    private
    static void parseMappingExpression(ClassNode classNode, MethodCallExpression methodCallExpression, Map<String, Map> map) {
        String name = ASTUtil.parseValue(methodCallExpression.method)
        def args = (methodCallExpression.arguments as TupleExpression).expressions.first() as NamedArgumentListExpression
        map[name] = [:]
        for (arg in args.mapEntryExpressions) {
            def key = ASTUtil.parseValue(arg.keyExpression)
            switch (key) {
                case 'edge':
                    map[name][key] = arg.valueExpression
                    break;
                default:
                    map[name][key] = arg.valueExpression.text
                    break;
            }
        }
    }

    private void createPropertyGetter(ClassNode clazzNode, FieldNode field, FieldNode edgeField, Map mapping) {
        def getterStatement = returnS(castX(field.type, callX(varX(edgeField), 'getProperty', constX(field.name))))
        def method = new MethodNode("get${field.name.capitalize()}", ACC_PUBLIC, field.type, [] as Parameter[], [] as ClassNode[], getterStatement)
        clazzNode.addMethod(method)
    }

    private void createPropertySetter(ClassNode clazzNode, FieldNode field, FieldNode edgeField, Map mapping) {
        def setterParam = param(field.type, field.name)
        def setterVar = varX(setterParam)
        def setterStatement = stmt(callX(varX(edgeField), 'setProperty', args(constX(field.name), setterVar)))
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
