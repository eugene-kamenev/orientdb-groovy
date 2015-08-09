package com.github.eugene.kamenev.orient.ast

import com.github.eugene.kamenev.orient.ast.util.ASTUtil
import com.github.eugene.kamenev.orient.ast.util.EntityStructure
import com.github.eugene.kamenev.orient.document.OrientDocument
import com.github.eugene.kamenev.orient.document.OrientDocumentHelper
import com.github.eugene.kamenev.orient.graph.Edge
import com.github.eugene.kamenev.orient.graph.OrientGraphHelper
import com.github.eugene.kamenev.orient.graph.Vertex
import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import com.tinkerpop.blueprints.impls.orient.OrientEdge
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import com.tinkerpop.blueprints.impls.orient.OrientVertex
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.TupleExpression
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
    static ClassNode IDENTIFIABLE = ClassHelper.make(OIdentifiable).plainNodeReference
    static ClassNode VERTEX = ClassHelper.make(OrientVertex).plainNodeReference
    static ClassNode EDGE = ClassHelper.make(OrientEdge).plainNodeReference
    static ClassNode DOCUMENT = ClassHelper.make(ODocument).plainNodeReference
    static ClassNode ORIENT_GRAPH = ClassHelper.make(OrientGraph).plainNodeReference

    static ClassNode ORIENT_DOCUMENT = ClassHelper.make(OrientDocument).plainNodeReference
    static ClassNode ORIENT_VERTEX = ClassHelper.make(Vertex).plainNodeReference
    static ClassNode ORIENT_EDGE = ClassHelper.make(Edge).plainNodeReference

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
    }

    def createConstructors() {
        // params for constructors
        def oIdentifiableParams = params(param(IDENTIFIABLE, 'oIdentifiable'))
        def selfType = params(param(injectedObject.type.plainNodeReference, 'selfType'))
        // three type of constructors
        def emptyConstructor = new BlockStatement()
        def oIdentifiableContructor = new BlockStatement()
        def selfTypeConstructor = new BlockStatement()

        if (injectedObject.type == VERTEX) {
            emptyConstructor.addStatement(assignS(varX(injectedObject), callGetActiveGraph('addTemporaryVertex', constX(className))))
            oIdentifiableContructor.addStatement(assignS(varX(injectedObject), callGetActiveGraph('getVertex', varX(oIdentifiableParams[0]))))
        }
        if (injectedObject.type == EDGE) {
            emptyConstructor.addStatement(stmt(assignX(varX(injectedObject), ctorX(EDGE))))
            oIdentifiableContructor.addStatement(assignS(varX(injectedObject), callGetActiveGraph('getEdge', varX(oIdentifiableParams[0]))))
        }
        if (injectedObject.type == DOCUMENT) {
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

    def createGetters() {
        entityProperties.each {k, orientProperty ->
            println "processing getter for $k"
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
                if (injectedObject.type == VERTEX || injectedObject.type == EDGE) {
                    assignExpression = callGraphHelper('executeQuery', queryArgs)
                }
                if (injectedObject.type == DOCUMENT) {
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
     * @since 0.1.1
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
        def edgeNodeAnnotation = edgeClass.getAnnotations(ORIENT_EDGE)[0]
        def inNode = ((ClassNode) ASTUtil.annotationValue(edgeNodeAnnotation.members.from))
        def outNode = ((ClassNode) ASTUtil.annotationValue(edgeNodeAnnotation.members.to))
        def edgeName = (String) ASTUtil.annotationValue(edgeNodeAnnotation.members.name) ?: edgeClass.nameWithoutPackage
        def isCollection = orientProperty.isCollection()
        def methodName = isCollection ? 'transformVertexCollectionToEntity' : 'transformVertexToEntity'
        def pipeResultMethodName = isCollection ? 'toList' : 'next'
        def direction = inNode == entity ? 'in' : 'out'
        def currentNode = inNode == entity ? outNode : inNode
        def thisVertex = varX(injectedObject)
        def callThisExpression = callX(callGraphHelper('pipe', args(thisVertex)), direction, args(constX(edgeName)))
        def arguments = args(classX(currentNode), callX(callThisExpression, pipeResultMethodName))
        callGraphHelper(methodName, arguments)
    }

    def createEdgeSetter(OrientProperty property) {
        Statement setterStatenent = null
        def edgeClass = property.mapping.edge as ClassExpression
        def parameter = param(property.collectionGenericType, property.nodeName)
        def setterVar = varX(parameter)
        def edgeNodeAnnotation = edgeClass.type.plainNodeReference.getAnnotations(ORIENT_EDGE)[0]
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

    Expression setToInjectedObject(OrientProperty orientProperty, Parameter parameter) {
        ArgumentListExpression arguments = args(constX(orientProperty.mapping.field), varX(parameter))
        def isTyped = orientProperty.link || orientProperty.linkedList || orientProperty.linkedSet || orientProperty.embedded
        if (injectedObject.type == VERTEX || injectedObject.type == EDGE) {
            if (isTyped) {
                def methodName = orientProperty.collection ? 'transformEntityCollectionToVertex' : 'getVertexFromEntity'
                arguments = args(constX(orientProperty.mapping.field), callGraphHelper(methodName, varX(parameter)))
            }
            if (orientProperty.orientType) {
                arguments.addExpression(orientProperty.mapping.type as Expression)
            }
            return callX(varX(injectedObject), 'setProperty', arguments)
        }
        if (injectedObject.type == DOCUMENT) {
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
     * @param orientProperty
     * @return
     */
    Expression getFromInjectedObject(OrientProperty orientProperty) {
        def isTyped = orientProperty.link || orientProperty.linkedList || orientProperty.linkedSet || orientProperty.embedded
        if (injectedObject.type == VERTEX || injectedObject.type == EDGE) {
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
        if (injectedObject.type == DOCUMENT) {
            def getterExpression = callX(varX(injectedObject), 'field', args(constX(orientProperty.mapping.field)))
            if (isTyped) {
                def methodName = orientProperty.collection ?  'transformDocumentCollection' : 'transformDocument'
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

    static Expression callDocHelper(String methodName, Expression args) {
        callX(DOC_HELPER, methodName, args)
    }

    static Expression callGraphHelper(String methodName, Expression args) {
        callX(GRAPH_HELPER, methodName, args)
    }

    static Expression callOrientGraph(String methodName, Expression args) {
        callX(ORIENT_GRAPH, methodName, args)
    }

    static Expression callGetActiveGraph(String methodName, Expression args) {
        callX(callOrientGraph('getActiveGraph', new TupleExpression()), methodName, args)
    }

    /**
     * Removes all mapped properties from entity class node
     * @return
     */
    def removeAllMappedProperties() {
        allFields.each {
            ASTUtil.removeProperty(entity, it.name)
        }
    }


}
