package com.github.eugene.kamenev.orient.ast

import com.github.eugene.kamenev.orient.ast.util.ASTUtil
import com.github.eugene.kamenev.orient.ast.util.EntityStructure
import com.github.eugene.kamenev.orient.document.OrientDocument
import com.github.eugene.kamenev.orient.document.OrientDocumentHelper
import com.github.eugene.kamenev.orient.graph.Edge
import com.github.eugene.kamenev.orient.graph.OrientGraphHelper
import com.github.eugene.kamenev.orient.graph.Vertex
import com.github.eugene.kamenev.orient.schema.SchemaHelper
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import com.tinkerpop.blueprints.impls.orient.OrientEdge
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import com.tinkerpop.blueprints.impls.orient.OrientVertex
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement

import static org.codehaus.groovy.ast.tools.GeneralUtils.*

/**
 * Represents Orient Entity Structure
 *
 * @author @eugenekamenev
 * @since 0.1.1
 */
@CompileStatic
class OrientStructure extends EntityStructure<OrientProperty> {

    static ClassNode GRAPH_HELPER = ClassHelper.make(OrientGraphHelper).plainNodeReference
    static ClassNode DOC_HELPER = ClassHelper.make(OrientDocumentHelper).plainNodeReference
    static ClassNode SCHEMA_HELPER = ClassHelper.make(SchemaHelper).plainNodeReference
    static ClassNode IDENTIFIABLE = ClassHelper.make(OIdentifiable).plainNodeReference
    static ClassNode VERTEX = ClassHelper.make(OrientVertex).plainNodeReference
    static ClassNode EDGE = ClassHelper.make(OrientEdge).plainNodeReference
    static ClassNode DOCUMENT = ClassHelper.make(ODocument).plainNodeReference
    static ClassNode ORIENT_GRAPH = ClassHelper.make(OrientGraph).plainNodeReference
    static ClassNode ODATABASE_TX = ClassHelper.make(ODatabaseDocumentTx).plainNodeReference

    static ClassNode ORIENT_GROOVY_DOCUMENT = ClassHelper.make(OrientDocument).plainNodeReference
    static ClassNode ORIENT_GROOVY_VERTEX = ClassHelper.make(Vertex).plainNodeReference
    static ClassNode ORIENT_GROOVY_EDGE = ClassHelper.make(Edge).plainNodeReference

    /**
     * Injected instance of
     * {@link com.tinkerpop.blueprints.impls.orient.OrientVertex},
     * {@link com.tinkerpop.blueprints.impls.orient.OrientEdge} or
     * {@link com.orientechnologies.orient.core.record.impl.ODocument}
     *
     */
    final FieldNode injectedObject

    /**
     * OrientDB class name
     */
    final String className

    OrientStructure(ClassNode entityClassNode, AnnotationNode annotationNode, String injectedFieldName) {
        super(entityClassNode, annotationNode)
        this.injectedObject = entityClassNode.fields.find {it.name == injectedFieldName}
        this.className = ASTUtil.parseValue(annotationNode.members.value, entityClassNode.nameWithoutPackage)
        this.transients << injectedFieldName
    }

    @Override
    OrientProperty mapProperty(FieldNode node) {
        new OrientProperty(entity, node)
    }

    def initTransformation() {
        createGetters()
        createSetters()
        createConstructors()
        createInitSchemaMethod()
        createInitSchemaLinksMethod()
    }

    def createConstructors() {
        // params for constructors
        def oIdentifiableParams = params(param(IDENTIFIABLE, 'oIdentifiable'))
        def selfType = params(param(injectedObject.type.plainNodeReference, 'selfType'))
        // three type of constructors
        def emptyConstructor = new BlockStatement()
        def oIdentifiableContructor = new BlockStatement()
        def selfTypeConstructor = new BlockStatement()

        if (vertex) {
            emptyConstructor.addStatement(assignS(varX(injectedObject), callGetActiveGraph('addTemporaryVertex', constX(className))))
            oIdentifiableContructor.addStatement(assignS(varX(injectedObject), callGetActiveGraph('getVertex', varX(oIdentifiableParams[0]))))
        }
        if (edge) {
            emptyConstructor.addStatement(stmt(assignX(varX(injectedObject), ctorX(EDGE))))
            oIdentifiableContructor.addStatement(assignS(varX(injectedObject), callGetActiveGraph('getEdge', varX(oIdentifiableParams[0]))))
        }
        if (document) {
            emptyConstructor.addStatement(assignS(varX(injectedObject), ctorX(DOCUMENT, constX(className))))
            oIdentifiableContructor.addStatement(assignS(varX(injectedObject), ctorX(DOCUMENT, varX(oIdentifiableParams[0]))))
        }
        selfTypeConstructor.addStatement(assignS(varX(injectedObject), varX(selfType[0])))
        // add initial values to empty constructor
        entityProperties.each {k, orientProperty ->
            if (!orientProperty.edge && orientProperty.hasInitialValue()) {
                emptyConstructor.addStatement(stmt(callThisX("set${orientProperty.nodeName.capitalize()}", orientProperty.fieldNode.initialValueExpression)))
            }
        }
        entity.addConstructor(new ConstructorNode(ACC_PUBLIC, emptyConstructor))
        entity.addConstructor(new ConstructorNode(ACC_PUBLIC, oIdentifiableParams, [] as ClassNode[], oIdentifiableContructor))
        entity.addConstructor(new ConstructorNode(ACC_PUBLIC, selfType, [] as ClassNode[], selfTypeConstructor))
    }

    /**
     * Create initSchema method for class
     * It can initialize simple class properties
     */
    def createInitSchemaMethod() {
        boolean initSchema = ASTUtil.parseValue(annotation.members.initSchema, false)
        if (initSchema) {
            def mappingMap = new MapExpression()
            entityProperties.each {k, orientProperty ->
                if (!orientProperty.specialType) {
                    def mappingEntries = []
                    mappingEntries << new MapEntryExpression(constX('clazz'), classX(orientProperty.fieldNode.type))
                    if (orientProperty.hasIndex()) {
                        mappingEntries << new MapEntryExpression(constX('index'), orientProperty.mapping.index as ConstantExpression)
                    }
                    if (mappingEntries) {
                        mappingMap.addMapEntryExpression(constX(orientProperty.mapping.field), new MapExpression(mappingEntries))
                    }
                }
            }
            def params = params(param(ODATABASE_TX, 'tx'))
            def args = args(varX(params[0]), mappingMap, constX(className))
            if (document) {
                args.addExpression(constX(null))
            }
            if (edge) {
                args.addExpression(constX('E'))
            }
            if (vertex) {
                args.addExpression(constX('V'))
            }
            entity.addMethod('initSchema', ACC_PUBLIC | ACC_STATIC,
                    ClassHelper.VOID_TYPE, params,
                    [] as ClassNode[],
                    block(stmt(callX(SCHEMA_HELPER, 'initClass', args))))
        }
    }

    /**
     * Create initSchemaLinks method for class
     * It can initialize orientdb links and other properties
     */
    def createInitSchemaLinksMethod() {
        boolean initSchema = ASTUtil.parseValue(annotation.members.initSchema, false)
        if (initSchema) {
            def mappingMap = new MapExpression()
            entityProperties.each {k, orientProperty ->
                if (orientProperty.linkedType) {
                    def mappingEntries = []
                    def annotationNode = orientProperty.collectionGenericType.annotations.find {
                        it.classNode in [ORIENT_GROOVY_VERTEX, ORIENT_GROOVY_EDGE, ORIENT_GROOVY_DOCUMENT]
                    }
                    def clazzName = ASTUtil.parseValue(annotationNode?.members?.value, orientProperty.collectionGenericType.nameWithoutPackage)
                    mappingEntries << new MapEntryExpression(constX('linkedClass'), constX(clazzName))
                    mappingEntries << new MapEntryExpression(constX('type'), orientProperty.mapping.type as Expression)
                    if (mappingEntries) {
                        mappingMap.addMapEntryExpression(constX(orientProperty.mapping.field), new MapExpression(mappingEntries))
                    }
                }
            }
            def params = params(param(ODATABASE_TX, 'tx'))
            def args = args(varX(params[0]), mappingMap, constX(className))
            if (document) {
                args.addExpression(constX(null))
            }
            if (edge) {
                args.addExpression(constX('E'))
            }
            if (vertex) {
                args.addExpression(constX('V'))
            }
            entity.addMethod('initSchemaLinks', ACC_PUBLIC | ACC_STATIC,
                    ClassHelper.VOID_TYPE, params,
                    [] as ClassNode[],
                    block(stmt(callX(SCHEMA_HELPER, 'initClassLinks', args))))
        }
    }

    /**
     * Create entity getters task
     *
     * @return
     */
    def createGetters() {
        entityProperties.each {k, orientProperty ->
            def methodBody = new BlockStatement()
            def resultVar = varX('resultVar', orientProperty.nodeType)
            Expression assignExpression = null
            methodBody.addStatement(declS(varX(resultVar), new EmptyExpression()))

            // process formula feature
            if (orientProperty.formula) {
                def queryArgs = args(classX(orientProperty.collectionGenericType),
                        constX(orientProperty.mapping.formula),
                        constX(!orientProperty.collection),
                        orientProperty.mapping?.params as Expression)
                // this can be wrong, because we should take care of type result object, not the
                // injected one.
                if (vertexOrEdge) {
                    assignExpression = callGraphHelper('executeQuery', queryArgs)
                }
                if (document) {
                    assignExpression = callDocHelper('executeQuery', queryArgs)
                }
            } else {
                if (orientProperty.edge) {
                    assignExpression = createEdgeGetter(orientProperty)
                } else {
                    assignExpression = getFromInjectedObject(orientProperty)
                }
            }
            methodBody.addStatement(assignS(varX(resultVar), assignExpression))
            methodBody.addStatement(returnS(varX(resultVar)))
            entity.addMethod(new MethodNode("get${orientProperty.fieldNode.name.capitalize()}", ACC_PUBLIC, orientProperty.nodeType, [] as Parameter[], [] as ClassNode[], methodBody))
        }
    }

    /**
     * Create entity setters task
     *
     * @return
     */
    def createSetters() {
        def methodReturnType = ClassHelper.VOID_TYPE
        entityProperties.each {k, orientProperty ->
            if (!orientProperty.formula) {
                def methodBody = new BlockStatement()
                def setterParam = param(orientProperty.nodeType, orientProperty.nodeName)
                if (orientProperty.edge) {
                    createEdgeSetter(orientProperty)
                } else {
                    methodBody.addStatement(stmt(setToInjectedObject(orientProperty, setterParam)))
                    entity.addMethod("set${orientProperty.nodeName.capitalize()}", ACC_PUBLIC, methodReturnType, params(setterParam), [] as ClassNode[], methodBody)
                }
            }
        }
    }

    /**
     * Create edge getter expression
     *
     * @param currentClass
     * @param edgeClass
     * @param inNode
     * @param outNode
     * @param orientProperty
     * @param edgeName
     * @return
     */
    Expression createEdgeGetter(OrientProperty orientProperty) {
        def edgeClassExpression = orientProperty.mapping.edge as ClassExpression
        def edgeClass = edgeClassExpression.type.plainNodeReference
        def edgeNodeAnnotation = edgeClass.getAnnotations(ORIENT_GROOVY_EDGE)[0]
        def inNode = ((ClassNode) ASTUtil.annotationValue(edgeNodeAnnotation.members.from))
        def outNode = ((ClassNode) ASTUtil.annotationValue(edgeNodeAnnotation.members.to))
        def edgeName = (String) ASTUtil.annotationValue(edgeNodeAnnotation.members.name) ?: edgeClass.nameWithoutPackage
        def isCollection = orientProperty.collection
        def methodName = isCollection ? 'transformVertexCollectionToEntity' : 'transformVertexToEntity'
        def pipeResultMethodName = isCollection ? 'toList' : 'next'
        def direction = inNode == entity ? 'in' : 'out'
        def currentNode = inNode == entity ? outNode : inNode
        def thisVertex = varX(injectedObject)
        def callThisExpression = callX(callGraphHelper('pipe', args(thisVertex)), direction, args(constX(edgeName)))
        def arguments = args(classX(currentNode), callX(callThisExpression, pipeResultMethodName))
        callGraphHelper(methodName, arguments)
    }

    /**
     * Creates edge addTo* setter method
     *
     * @param property
     */
    def createEdgeSetter(OrientProperty property) {
        Statement setterStatenent = null
        def edgeClass = property.mapping.edge as ClassExpression
        def parameter = param(property.collectionGenericType, property.nodeName)
        def setterVar = varX(parameter)
        def edgeNodeAnnotation = edgeClass.type.plainNodeReference.getAnnotations(ORIENT_GROOVY_EDGE)[0]
        def inNode = (ClassNode) ASTUtil.annotationValue(edgeNodeAnnotation.members.from)
        def outNode = (ClassNode) ASTUtil.annotationValue(edgeNodeAnnotation.members.to)
        if (outNode == entity) {
            setterStatenent = returnS(callGraphHelper('createEdge', args(varX(injectedObject), propX(setterVar, 'vertex'), edgeClass)))
        }
        if (inNode == entity) {
            setterStatenent = returnS(callGraphHelper('createEdge', args(propX(setterVar, 'vertex'), varX(injectedObject), edgeClass)))
        }
        def methodName = "addTo${property.nodeName.capitalize()}"
        entity.addMethod(new MethodNode(methodName, ACC_PUBLIC, edgeClass.type.plainNodeReference, params(parameter), [] as ClassNode[], setterStatenent))
    }

    /**
     * Create setter expression for property
     *
     * @param orientProperty
     * @param parameter
     * @return
     */
    Expression setToInjectedObject(OrientProperty orientProperty, Parameter parameter) {
        ArgumentListExpression arguments = args(constX(orientProperty.mapping.field), varX(parameter))
        def isTyped = orientProperty.link || orientProperty.linkedList || orientProperty.linkedSet || orientProperty.embedded
        if (vertexOrEdge) {
            if (isTyped) {
                def methodName = orientProperty.collection ? 'transformEntityCollectionToVertex' : 'getVertexFromEntity'
                arguments = args(constX(orientProperty.mapping.field), callGraphHelper(methodName, varX(parameter)))
            }
            if (orientProperty.orientType) {
                arguments.addExpression(orientProperty.mapping.type as Expression)
            }
            return callX(varX(injectedObject), 'setProperty', arguments)
        }
        if (document) {
            if (isTyped) {
                def methodName = orientProperty.collection ? 'transformEntityCollection' : 'transformEntity'
                arguments = args(constX(orientProperty.mapping.field), callDocHelper(methodName, varX(parameter)))
            }
            if (orientProperty.orientType) {
                arguments.addExpression(orientProperty.mapping.type as Expression)
            }
            return callX(varX(injectedObject), 'field', arguments)
        }
        null
    }

    /**
     * Create getter expression
     *
     * @param orientProperty
     * @return
     */
    Expression getFromInjectedObject(OrientProperty orientProperty) {
        def isTyped = orientProperty.link || orientProperty.linkedList || orientProperty.linkedSet || orientProperty.embedded
        if (vertexOrEdge) {
            def getterExpression = callX(varX(injectedObject), 'getProperty', args(constX(orientProperty.mapping.field)))
            if (isTyped) {
                def methodName = orientProperty.collection ? 'transformVertexCollectionToEntity' : 'transformVertexToEntity'
                ArgumentListExpression arguments = args(classX(orientProperty.collectionGenericType), getterExpression)
                if (orientProperty.collection) {
                    arguments.addExpression(orientProperty.orientType ?
                            (orientProperty.mapping.type as Expression) : propX(classX(ClassHelper.make(OType)), 'LINKLIST'))
                }
                return callGraphHelper(methodName, arguments)
            }
            return getterExpression
        }
        if (document) {
            def getterExpression = callX(varX(injectedObject), 'field', args(constX(orientProperty.mapping.field)))
            if (isTyped) {
                def methodName = orientProperty.collection ? 'transformDocumentCollection' : 'transformDocument'
                ArgumentListExpression arguments = args(classX(orientProperty.collectionGenericType), getterExpression)
                if (orientProperty.collection) {
                    arguments.addExpression(orientProperty.orientType ?
                            (orientProperty.mapping.type as Expression) : propX(classX(ClassHelper.make(OType)), 'LINKLIST'))
                }
                return callDocHelper(methodName, arguments)
            }
            return getterExpression
        }
        null
    }

    static Expression callSchemaHelper(String methodName, Expression args) {
        callX(SCHEMA_HELPER, methodName, args)
    }

    /**
     * Create call expression on {@link OrientDocumentHelper}
     *
     * @param methodName
     * @param args
     * @return
     */
    static Expression callDocHelper(String methodName, Expression args) {
        callX(DOC_HELPER, methodName, args)
    }

    /**
     * Create call expression on {@link OrientGraphHelper}
     *
     * @param methodName
     * @param args
     * @return
     */
    static Expression callGraphHelper(String methodName, Expression args) {
        callX(GRAPH_HELPER, methodName, args)
    }

    /**
     * Create call expression on {@link OrientGraph}
     *
     * @param methodName
     * @param args
     * @return
     */
    static Expression callOrientGraph(String methodName, Expression args) {
        callX(ORIENT_GRAPH, methodName, args)
    }

    /**
     * Create call expression on {@link OrientGraph#getActiveGraph()}
     *
     * @param methodName
     * @param args
     * @return
     */
    static Expression callGetActiveGraph(String methodName, Expression args) {
        callX(callOrientGraph('getActiveGraph', new TupleExpression()), methodName, args)
    }

    /**
     * if injected object is vertex
     * @return
     */
    boolean isVertex() {
        injectedObject.type == VERTEX
    }

    /**
     * if injected object is edge
     * @return
     */
    boolean isEdge() {
        injectedObject.type == EDGE
    }

    /**
     * if injected object is document
     * @return
     */
    boolean isDocument() {
        injectedObject.type == DOCUMENT
    }

    /**
     * if injected object is edge
     * @return
     */
    boolean isVertexOrEdge() {
        vertex || edge
    }

    /**
     * Clean node from 'static mapping' and 'static transient' fields
     */
    def clean() {
        ASTUtil.removeProperty(entity, 'mapping')
        ASTUtil.removeProperty(entity, 'transients')
        allFields.each {
            ASTUtil.removeProperty(entity, it.name)
        }
    }
}
