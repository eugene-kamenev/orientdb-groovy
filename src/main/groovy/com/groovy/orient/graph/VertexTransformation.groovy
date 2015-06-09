package com.groovy.orient.graph

import com.groovy.orient.OrientGraphDSL
import com.groovy.orient.document.util.ASTUtil
import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import com.tinkerpop.blueprints.impls.orient.OrientVertex
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.DelegateASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import static org.codehaus.groovy.ast.tools.GeneralUtils.*

/**
 * @author @eugenekamenev
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class VertexTransformation extends AbstractASTTransformation {
    static final ClassNode orientBaseGraphNode = ClassHelper.make(OrientBaseGraph).plainNodeReference
    static final ClassNode orientGraphDSLNode = ClassHelper.make(OrientGraphDSL).plainNodeReference
    static final ClassNode delegateNode = ClassHelper.make(Delegate).plainNodeReference
    static final ClassNode orientVertexNode = ClassHelper.make(OrientVertex).plainNodeReference
    static final ClassNode orientGraphNode = ClassHelper.make(OrientGraph).plainNodeReference
    static final ClassNode oIdentifiableNode = ClassHelper.make(OIdentifiable).plainNodeReference
    static final ClassNode edgeAnnotationNode = ClassHelper.make(Edge).plainNodeReference
    static final List<ClassNode> collectionNodes = [ClassHelper.make(List).plainNodeReference]

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        AnnotationNode annotation = (AnnotationNode) nodes[0];
        ClassNode annotatedClass = (ClassNode) nodes[1];
        List<String> transients = ASTUtil.parseValue(annotatedClass.getField('transients')?.initialExpression, []) as List<String>
        String clusterName = ASTUtil.parseValue(annotation.members.value, annotatedClass.nameWithoutPackage)
        def vertexFieldNode = annotatedClass.addField('vertex', ACC_PUBLIC | ACC_FINAL, orientVertexNode, new EmptyExpression())
        def mappingClosure = annotatedClass.getField('mapping')
        createConstructors(annotatedClass, clusterName, vertexFieldNode)
        def mapping = [:]
        mapping = createEntityMappingMap(annotatedClass, mappingClosure?.initialExpression as ClosureExpression)
        def fields = annotatedClass.fields.findAll {
            if (it.name == 'vertex') {
                def annotationNode = new AnnotationNode(delegateNode)
                def delegateTransformation = new DelegateASTTransformation()
                delegateTransformation.visit([annotationNode, it] as ASTNode[], source)
            }
            !(it.name in transients) && !(it.static) && (it.modifiers != ACC_TRANSIENT) && (it.name != 'vertex')
        }
        fields.each {
            createPropertyGetter(annotatedClass, it, mapping[it.name] as Map)
            createPropertySetter(annotatedClass, it, mapping[it.name] as Map)
            ASTUtil.removeProperty(annotatedClass, it.name)
        }
        // clean up
        ASTUtil.removeProperty(annotatedClass, 'mapping')
        ASTUtil.removeProperty(annotatedClass, 'transients')
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

    private static void createConstructors(ClassNode classNode, String orientCluster, FieldNode thisVertex) {
        def recordIdParams = params(param(oIdentifiableNode, 'oIdentifiable'))
        def vertexParams = params(param(orientVertexNode, 'vertex1'))
        def initStatementRecordId = stmt(assignX(varX('vertex'), callX(callX(orientGraphNode, 'getActiveGraph'), 'getVertex', varX(recordIdParams[0]))))
        def initStatement = stmt(assignX(varX('vertex'), callX(callX(orientGraphNode, 'getActiveGraph'), 'addVertex', constX('class:' + orientCluster))))
        def emptyConstructor = new ConstructorNode(ACC_PUBLIC, initStatement)
        def initStatementDocument = stmt(assignX(varX(thisVertex), varX(vertexParams[0])))
        def documentConstructor = new ConstructorNode(ACC_PUBLIC, vertexParams, [] as ClassNode[], initStatementDocument)
        def recordConnstructorNode = new ConstructorNode(ACC_PUBLIC, recordIdParams, [] as ClassNode[], initStatementRecordId)
        classNode.addConstructor(emptyConstructor)
        classNode.addConstructor(documentConstructor)
        classNode.addConstructor(recordConnstructorNode)
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
                case 'type':
                    map[name][key] = arg.valueExpression
                    break;
                default:
                    map[name][key] = arg.valueExpression.text
                    break;
            }
        }
    }

    private void createPropertyGetter(ClassNode clazzNode, FieldNode field, Map mapping) {
        Statement getterStatement
        def propertyName = mapping?.field ?: field.name
        def thisVertex = clazzNode.fields.find { it.name == 'vertex' }
        if (mapping?.edge) {
            def experssion = mapping.edge as ClassExpression
            def edgeClass = experssion.type.plainNodeReference
            def edgeNodeAnnotation = edgeClass.getAnnotations(edgeAnnotationNode)[0]
            def inNode = ((ClassNode) ASTUtil.annotationValue(edgeNodeAnnotation.members.from))
            def outNode = ((ClassNode) ASTUtil.annotationValue(edgeNodeAnnotation.members.to))
            getterStatement = generateEdgeGetter(clazzNode, edgeClass, inNode, outNode, field)
        } else {
            getterStatement = returnS(castX(field.type, callX(varX(thisVertex), 'getProperty', constX(propertyName))))
        }
        def method = new MethodNode("get${field.name.capitalize()}", ACC_PUBLIC, field.type, [] as Parameter[], [] as ClassNode[], getterStatement)
        clazzNode.addMethod(method)
    }

    private Statement generateEdgeGetter(ClassNode currentClass, ClassNode edgeClass, ClassNode inNode, ClassNode outNode, FieldNode fieldNode) {
        def isCollection = fieldNode.type.plainNodeReference in collectionNodes
        def methodName = isCollection ? 'transformToVertexList' : 'transformToVertex'
        def pipeResultMethodName = isCollection ? 'toList' : 'next'
        def direction = inNode == currentClass ? 'in' : 'out'
        def currentNode = inNode == currentClass ? outNode : inNode
        def thisVertex = varX(currentClass.fields.find { it.name == 'vertex' })
        def callThisExpression = callX(callX(orientGraphDSLNode, 'pipe', args(thisVertex)), direction, args(constX(edgeClass.nameWithoutPackage)))
        def expression = callX(orientGraphDSLNode, methodName, args(classX(currentNode), callX(callThisExpression, pipeResultMethodName)))
        return returnS(castX(fieldNode.type, expression))
    }

    private void createPropertySetter(ClassNode clazzNode, FieldNode field, Map mapping) {
        String methodName
        Statement setterStatement
        def setterParam = param(field.type, field.name)
        def setterVar = varX(setterParam)
        Parameter[] methodParams = params(setterParam)
        def propertyName = mapping?.field ?: field.name
        ClassNode returnType = ClassHelper.VOID_TYPE
        def thisVertex = varX(clazzNode.fields.find { it.name == 'vertex' })
        if (mapping?.edge) {
            def edgeClassExpr = mapping.edge as ClassExpression
            def edgeNodeAnnotation = edgeClassExpr.type.plainNodeReference.getAnnotations(edgeAnnotationNode)[0]
            def isCollection = field.type.plainNodeReference in collectionNodes
            if (isCollection) {
                methodParams[0] = param(field.type.genericsTypes[0].type.plainNodeReference, field.name)
                setterVar = varX(methodParams[0])
            }
            returnType = edgeClassExpr.type.plainNodeReference
            def inNode = (ClassNode) ASTUtil.annotationValue(edgeNodeAnnotation.members.from)
            def outNode = (ClassNode) ASTUtil.annotationValue(edgeNodeAnnotation.members.to)
            methodName = "addTo${field.name.capitalize()}"
            if (outNode == clazzNode) {
                setterStatement = returnS(castX(returnType, callX(orientGraphDSLNode, 'createEdge', args(thisVertex, propX(setterVar, 'vertex'), edgeClassExpr))))
            }
            if (inNode == clazzNode) {
                setterStatement = returnS(castX(returnType, callX(orientGraphDSLNode, 'createEdge', args(propX(setterVar, 'vertex'), thisVertex, edgeClassExpr))))
            }
        } else {
        /* here we should catch mapping types like OType.LINK and others
                    if (mapping.type) {

                    }
               */
            methodName = "set${field.name.capitalize()}"
            methodParams = params(setterParam)
            setterStatement = stmt(callX(varX(thisVertex), 'setProperty', args(constX(propertyName), setterVar)))
        }
        def method = new MethodNode(methodName, ACC_PUBLIC, returnType, methodParams, [] as ClassNode[], setterStatement)
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
