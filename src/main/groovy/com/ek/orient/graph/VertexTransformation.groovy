package com.ek.orient.graph
import com.ek.orient.ast.ASTUtil
import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.metadata.schema.OType
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
 * Groovy AST Transformation for injecting vertex related features into entity class
 * @see Vertex
 * @see OrientGraphHelper
 *
 * @author @eugenekamenev
 * @since 0.1.0
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class VertexTransformation extends AbstractASTTransformation {
    /**
     * ClassNodes for usage in transformation
     */
    static final ClassNode otype = ClassHelper.make(OType).plainNodeReference
    static final ClassNode listNode = ClassHelper.make(List).plainNodeReference
    static final ClassNode setNode = ClassHelper.make(LinkedHashSet).plainNodeReference
    static final ClassNode orientGraphHelperNode = ClassHelper.make(OrientGraphHelper).plainNodeReference
    static final ClassNode delegateNode = ClassHelper.make(Delegate).plainNodeReference
    static final ClassNode orientVertexNode = ClassHelper.make(OrientVertex).plainNodeReference
    static final ClassNode orientGraphNode = ClassHelper.make(OrientGraph).plainNodeReference
    static final ClassNode oIdentifiableNode = ClassHelper.make(OIdentifiable).plainNodeReference
    static final ClassNode edgeAnnotationNode = ClassHelper.make(Edge).plainNodeReference
    static final List<ClassNode> collectionNodes = [ClassHelper.make(List).plainNodeReference]

    /**
     * Transformation starts here, visiting the class node
     * @since 0.1.0
     *
     * @param nodes
     * @param source
     */
    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        // Vertex annotation node
        AnnotationNode annotation = (AnnotationNode) nodes[0];
        // current transformation class node
        ClassNode annotatedClass = (ClassNode) nodes[1];
        // get transient fields from class to exclude them from transformation
        List<String> transients = ASTUtil.parseValue(annotatedClass.getField('transients')?.initialExpression, []) as List<String>
        // get orientdb document class name
        String clusterName = ASTUtil.parseValue(annotation.members.value, annotatedClass.nameWithoutPackage)
        // add vertex field into class
        def vertexFieldNode = annotatedClass.addField('vertex', ACC_PUBLIC | ACC_FINAL, orientVertexNode, new EmptyExpression())
        // create constructors
        createConstructors(annotatedClass, clusterName, vertexFieldNode)
        // read mapping closure, parse and create entity mapping map
        def mappingClosure = annotatedClass.getField('mapping')
        def mapping = [:]
        mapping = createEntityMappingMap(annotatedClass, mappingClosure?.initialExpression as ClosureExpression)
        // collect entity properties for transformation, exclude transients and vertex field
        def fields = annotatedClass.fields.findAll {
            if (it.name == 'vertex') {
                def annotationNode = new AnnotationNode(delegateNode)
                def delegateTransformation = new DelegateASTTransformation()
                delegateTransformation.visit([annotationNode, it] as ASTNode[], source)
            }
            !(it.name in transients) && !(it.static) && (it.modifiers != ACC_TRANSIENT) && (it.name != 'vertex')
        }
        // apply property transformation
        fields.each {
            createPropertyGetter(annotatedClass, it, mapping[it.name] as Map)
            createPropertySetter(annotatedClass, it, mapping[it.name] as Map)
            ASTUtil.removeProperty(annotatedClass, it.name)
        }
        // clean up
        ASTUtil.removeProperty(annotatedClass, 'mapping')
        ASTUtil.removeProperty(annotatedClass, 'transients')
    }

    /**
     * Parse entity mapping closure from source and return entity mapping map
     * @since 0.1.0
     *
     * @param classNode
     * @param expression
     * @return
     */
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

    /**
     * Create entity constructors
     * @since 0.1.0
     *
     * @param classNode
     * @param orientCluster
     * @param thisVertex
     */
    private static void createConstructors(ClassNode classNode, String orientCluster, FieldNode thisVertex) {
        def recordIdParams = params(param(oIdentifiableNode, 'oIdentifiable'))
        def vertexParams = params(param(orientVertexNode, 'vertex1'))
        def initStatementRecordId = stmt(assignX(varX('vertex'), callX(callX(orientGraphNode, 'getActiveGraph'), 'getVertex', varX(recordIdParams[0]))))
        def initStatement = stmt(assignX(varX('vertex'), callX(callX(orientGraphNode, 'getActiveGraph'), 'addTemporaryVertex', constX(orientCluster))))
        def emptyConstructor = new ConstructorNode(ACC_PUBLIC, initStatement)

        // @todo commented because not sure about such kind behavior
        // def initStatementDocument = ifElseS(notNullX(varX(vertexParams[0])), stmt(assignX(varX(thisVertex), varX(vertexParams[0]))), initStatement)
        def documentConstructor = new ConstructorNode(ACC_PUBLIC, vertexParams, [] as ClassNode[], initStatement)
        def recordConnstructorNode = new ConstructorNode(ACC_PUBLIC, recordIdParams, [] as ClassNode[], initStatementRecordId)
        classNode.addConstructor(emptyConstructor)
        classNode.addConstructor(documentConstructor)
        classNode.addConstructor(recordConnstructorNode)
    }

    /**
     * Parse single mapping closure expression
     * @since 0.1.0
     *
     * @param classNode
     * @param methodCallExpression
     * @param map
     */
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
                case 'params':
                    map[name][key] = arg.valueExpression
                    break;
                default:
                    map[name][key] = arg.valueExpression.text
                    break;
            }
        }
    }

    /**
     * Create entity property getter method
     * @since 0.1.0
     *
     * @param clazzNode
     * @param field
     * @param mapping
     */
    private void createPropertyGetter(ClassNode clazzNode, FieldNode field, Map mapping) {
        Statement getterStatement
        def propertyName = mapping?.field ?: field.name
        def thisVertex = clazzNode.fields.find { it.name == 'vertex' }
        def type = mapping?.type as PropertyExpression
        if (mapping?.formula) {
            def query = mapping.formula
            ClassNode queryClassNode
            def singleResult
            if (field.type.plainNodeReference in collectionNodes) {
                queryClassNode = field.type.genericsTypes[0].type.plainNodeReference
                singleResult = false
            } else {
                queryClassNode = field.type
                singleResult = true
            }
            getterStatement = returnS(callX(orientGraphHelperNode, 'executeQuery', args(classX(queryClassNode), constX(query), constX(singleResult), mapping?.params as Expression)))
        } else {
            if (mapping?.edge) {
                def expression = mapping.edge as ClassExpression
                def edgeClass = expression.type.plainNodeReference
                def edgeNodeAnnotation = edgeClass.getAnnotations(edgeAnnotationNode)[0]
                def inNode = ((ClassNode) ASTUtil.annotationValue(edgeNodeAnnotation.members.from))
                def outNode = ((ClassNode) ASTUtil.annotationValue(edgeNodeAnnotation.members.to))
                def edgeName = (String) ASTUtil.annotationValue(edgeNodeAnnotation.members.name) ?: edgeClass.nameWithoutPackage
                getterStatement = generateEdgeGetter(clazzNode, edgeClass, inNode, outNode, field, edgeName)
            } else {
                if (type) {
                    def resultBlock = new BlockStatement()
                    if (type.text.endsWith('LINK')) {
                        resultBlock.addStatement(returnS(callX(orientGraphHelperNode, 'transformVertexToEntity', args(classX(field.type), castX(oIdentifiableNode, callX(varX(thisVertex), 'getProperty', args(constX(propertyName))))))))
                    }
                    if (type.text.endsWith('LINKLIST') || type.text.endsWith('LINKSET')) {
                        def genericNode = field.type.genericsTypes[0].type.plainNodeReference
                        def getter = callX(varX(thisVertex), 'getProperty', args(constX(propertyName)))
                        resultBlock.addStatement(returnS(callX(orientGraphHelperNode, 'transformVertexCollectionToEntity', args(getter, type, classX(genericNode)))))
                    }
                    getterStatement = resultBlock
                } else {
                    getterStatement = returnS(castX(field.type, callX(varX(thisVertex), 'getProperty', constX(propertyName))))
                }
            }
        }
        def method = new MethodNode("get${field.name.capitalize()}", ACC_PUBLIC, field.type, [] as Parameter[], [] as ClassNode[], getterStatement)
        clazzNode.addMethod(method)
    }

    /**
     * Create edge getter statement
     * @since 0.1.0
     *
     * @param currentClass
     * @param edgeClass
     * @param inNode
     * @param outNode
     * @param fieldNode
     * @param edgeName
     * @return
     */
    private Statement generateEdgeGetter(ClassNode currentClass, ClassNode edgeClass, ClassNode inNode, ClassNode outNode, FieldNode fieldNode, String edgeName) {
        def isCollection = fieldNode.type.plainNodeReference in collectionNodes
        def methodName = isCollection ? 'transformVertexCollectionToEntity' : 'transformVertexToEntity'
        def pipeResultMethodName = isCollection ? 'toList' : 'next'
        def direction = inNode == currentClass ? 'in' : 'out'
        def currentNode = inNode == currentClass ? outNode : inNode
        def thisVertex = varX(currentClass.fields.find { it.name == 'vertex' })
        def callThisExpression = callX(callX(orientGraphHelperNode, 'pipe', args(thisVertex)), direction, args(constX(edgeName)))
        def arguments = args(classX(currentNode), callX(callThisExpression, pipeResultMethodName))
        def expression = callX(orientGraphHelperNode, methodName, arguments)
        return returnS(expression)
    }

    /**
     * Create entity setter property
     * @since 0.1.0
     *
     * @param clazzNode
     * @param field
     * @param mapping
     */
    private void createPropertySetter(ClassNode clazzNode, FieldNode field, Map mapping) {
        if (mapping?.formula == null) {
            return
        }
        String methodName
        Statement setterStatement
        def setterParam = param(field.type, field.name)
        def setterVar = varX(setterParam)
        Parameter[] methodParams = params(setterParam)
        def propertyName = mapping?.field ?: field.name
        def otype = mapping?.type as PropertyExpression
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
                setterStatement = returnS(castX(returnType, callX(orientGraphHelperNode, 'createEdge', args(thisVertex, propX(setterVar, 'vertex'), edgeClassExpr))))
            }
            if (inNode == clazzNode) {
                setterStatement = returnS(castX(returnType, callX(orientGraphHelperNode, 'createEdge', args(propX(setterVar, 'vertex'), thisVertex, edgeClassExpr))))
            }
        } else {
            methodName = "set${field.name.capitalize()}"
            methodParams = params(setterParam)
            def arguments = args(constX(propertyName), setterVar)
            if (otype) {
                if (otype.text.endsWith('LINK') || otype.text.endsWith('EMBEDDED')) {
                    arguments = args(constX(propertyName), propX(varX(setterVar), 'vertex'))
                }
                if (otype.text.endsWith('LINKLIST')) {
                    arguments = args(constX(propertyName), callX(orientGraphHelperNode, 'transformEntityCollectionToVertex', args(setterVar)))
                }
                if (otype.text.endsWith('LINKSET')) {
                    arguments = args(constX(propertyName), ctorX(setNode, args(callX(orientGraphHelperNode, 'transformEntityCollectionToVertex', args(setterVar)))))
                }
                arguments.addExpression(otype)
            }
            setterStatement = stmt(callX(varX(thisVertex), 'setProperty', arguments))
        }
        def method = new MethodNode(methodName, ACC_PUBLIC, returnType, methodParams, [] as ClassNode[], setterStatement)
        clazzNode.addMethod(method)
    }
}
