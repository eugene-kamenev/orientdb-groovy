package com.groovy.orient.document

import com.groovy.orient.OrientDSL
import com.groovy.orient.document.util.ASTUtil
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
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
 * Transformation for OrientDB document usage
 *
 * @author @eugenekamenev
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class OrientDocumentTransformation extends AbstractASTTransformation {

    static final ClassNode document = ClassHelper.make(ODocument).plainNodeReference
    static final ClassNode otype = ClassHelper.make(OType).plainNodeReference
    static final ClassNode delegateNode = ClassHelper.make(Delegate).plainNodeReference
    static final ClassNode orientDSL = ClassHelper.make(OrientDSL).plainNodeReference
    static final ClassNode listNode = ClassHelper.make(List).plainNodeReference
    static final ClassNode setNode = ClassHelper.make(LinkedHashSet).plainNodeReference

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        AnnotationNode annotation = (AnnotationNode) nodes[0];
        ClassNode annotatedClass = (ClassNode) nodes[1];
        String clusterName = ASTUtil.parseValue(annotation.members.value, annotatedClass.nameWithoutPackage)
        List<String> transients = ASTUtil.parseValue(annotatedClass.getField('transients')?.initialExpression, []) as List<String>
        def documentFieldNode = annotatedClass.addField('document', ACC_PUBLIC | ACC_FINAL, document, new EmptyExpression())
        createConstructors(annotatedClass, clusterName, documentFieldNode)
        def fields = annotatedClass.fields.findAll {
            if (it.name == 'document') {
                def annotationNode = new AnnotationNode(delegateNode)
                def delegateTransformation = new DelegateASTTransformation()
                delegateTransformation.visit([annotationNode, it] as ASTNode[], source)
            }
            !(it.name in transients) && !(it.static) && (it.modifiers != ACC_TRANSIENT) && (it.name != 'document')
        }
        def mappingClosure = annotatedClass.getField('mapping')
        def mapping = createEntityMappingMap(annotatedClass, mappingClosure.initialExpression as ClosureExpression)
        fields.each {
            createPropertyGetter(annotatedClass, it, documentFieldNode, mapping[it.name])
            createPropertySetter(annotatedClass, it, documentFieldNode, mapping[it.name])
            ASTUtil.removeProperty(annotatedClass, it.name)
        }
        // clean up
        ASTUtil.removeProperty(annotatedClass, 'mapping')
        ASTUtil.removeProperty(annotatedClass, 'transients')
    }

    private void createConstructors(ClassNode classNode, String orientCluster, FieldNode thisDocument) {
        def initStatement = stmt(assignX(varX('document'), ctorX(document, constX(orientCluster))))
        def emptyConstructor = new ConstructorNode(ACC_PUBLIC, initStatement)
        def params = params(param(document, 'document1'))
        def initStatementDocument = stmt(assignX(varX(thisDocument), varX(params[0])))
        def documentConstructor = new ConstructorNode(ACC_PUBLIC, params, [] as ClassNode[], initStatementDocument)
        classNode.addConstructor(emptyConstructor)
        classNode.addConstructor(documentConstructor)
    }

    private static Map<String, Map> createEntityMappingMap(ClassNode classNode, ClosureExpression expression) {
        def mapping = [:]
        def block = expression.code as BlockStatement
        block.statements.each {
            parseMappingExpression(classNode, (it as ExpressionStatement).expression as MethodCallExpression, mapping)
        }
        mapping
    }

    private static modifyConstructorForEagerFetch(ClassNode classNode, String eagerField) {
        classNode.getDeclaredConstructors().each {
            def parameters = it.parameters
            if (parameters) {
                def firstParam = parameters.first()
                def expression = it.code as ExpressionStatement
                def newBlock = block(expression)
                newBlock.addStatement(stmt(callX(varX(firstParam), 'field', args(constX(eagerField)))))
                it.code = newBlock
            }
        }
    }

    private
    static void parseMappingExpression(ClassNode classNode, MethodCallExpression methodCallExpression, Map<String, Map> map) {
        String name = ASTUtil.parseValue(methodCallExpression.method)
        def args = (methodCallExpression.arguments as TupleExpression).expressions.first() as NamedArgumentListExpression
        map[name] = [:]
        for (arg in args.mapEntryExpressions) {
            def key = ASTUtil.parseValue(arg.keyExpression)
            switch (key) {
                case 'type':
                    map[name][key] = arg.valueExpression
                    break;
                case 'fetch':
                    if (arg.valueExpression.text == 'eager') {
                        modifyConstructorForEagerFetch(classNode, name)
                    }
                    map[name][key] = arg.valueExpression.text
                    break;
                default:
                    map[name][key] = arg.valueExpression.text
                    break;
            }
        }
    }

    private static void createPropertyGetter(ClassNode clazzNode, FieldNode field, FieldNode documentField, Map map) {
        def fieldName = map?.field ?: field.name
        def otype = map?.type as PropertyExpression
        def resultVar = varX('result', field.type)
        def resultBlock = block(declS(resultVar, new EmptyExpression()))
        if (otype) {
            if (otype.text.endsWith('LINK') || otype.text.endsWith('EMBEDDED')) {
                resultBlock.addStatement(assignS(resultVar, ctorX(field.type, args(castX(document, callX(varX(documentField), 'field', args(constX(fieldName))))))))
            }
            if (otype.text.endsWith('LINKLIST')) {
                def genericNode = field.type.genericsTypes[0].type.plainNodeReference
                def getter = castX(listNode, callX(varX(documentField), 'field', args(constX(fieldName))), true)
                resultBlock.addStatement(assignS(resultVar, callX(orientDSL, 'transformDocumentCollection', args(classX(genericNode), otype, getter))))
            }
            if (otype.text.endsWith('LINKSET')) {
                def genericNode = field.type.genericsTypes[0].type.plainNodeReference
                def getter = castX(setNode, callX(varX(documentField), 'field', args(constX(fieldName))), true)
                resultBlock.addStatement(assignS(resultVar, callX(orientDSL, 'transformDocumentCollection', args(classX(genericNode), otype, getter))))
            }
        } else {
            resultBlock.addStatement(assignS(varX(resultVar), castX(field.type, callX(varX(documentField), 'field', args(constX(fieldName))))))
        }
        resultBlock.addStatement(returnS(varX(resultVar)))
        def method = new MethodNode("get${field.name.capitalize()}", ACC_PUBLIC, field.type, [] as Parameter[], [] as ClassNode[], resultBlock)
        clazzNode.addMethod(method)
    }

    private static void createPropertySetter(ClassNode clazzNode, FieldNode field, FieldNode documentField, Map map) {
        def fieldName = map?.field ?: field.name
        def otype = map?.type as PropertyExpression
        def setterBlock = block()
        def setterParam = param(field.type, field.name)
        def setterVar = varX(setterParam)
        def arguments = args(constX(fieldName))
        if (otype) {
            if (otype.text.endsWith('LINK') || otype.text.endsWith('EMBEDDED')) {
                arguments.addExpression(propX(varX(setterVar), 'document'))
            }
            if (otype.text.endsWith('LINKLIST')) {
                arguments.addExpression(callX(orientDSL, 'transformEntityCollection', args(setterVar)))
            }
            if (otype.text.endsWith('LINKSET')) {
                arguments.addExpression(ctorX(setNode, args(callX(orientDSL, 'transformEntityCollection', args(setterVar)))))
            }
            arguments.addExpression(otype)
        } else {
            arguments.addExpression(setterVar)
        }
        setterBlock.addStatement stmt(callX(varX(documentField), 'field', arguments))
        def method = new MethodNode("set${field.name.capitalize()}", ACC_PUBLIC, ClassHelper.VOID_TYPE, params(setterParam), [] as ClassNode[], setterBlock)
        clazzNode.addMethod(method)
    }


}
