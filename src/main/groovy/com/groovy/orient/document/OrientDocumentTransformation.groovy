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
        def mapping = createEntityMappingMap(mappingClosure.initialExpression as ClosureExpression)
        fields.each {
            createPropertyGetter(annotatedClass, it, documentFieldNode, mapping[it.name])
            createPropertySetter(annotatedClass, it, documentFieldNode, mapping[it.name])
            ASTUtil.removeProperty(annotatedClass, it.name)
        }

        ASTUtil.removeProperty(annotatedClass, 'mapping')
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

    private static Map<String, Map> createEntityMappingMap(ClosureExpression expression) {
        def mapping = [:]
        def block = expression.code as BlockStatement
        block.statements.each {
            parseMappingExpression((it as ExpressionStatement).expression as MethodCallExpression, mapping)
        }
        mapping
    }

    private static void parseMappingExpression(MethodCallExpression methodCallExpression, Map<String, Map> map) {
        String name = ASTUtil.parseValue(methodCallExpression.method)
        def args = (methodCallExpression.arguments as TupleExpression).expressions.first() as NamedArgumentListExpression
        map[name] = [:]
        for (arg in args.mapEntryExpressions) {
            def key = ASTUtil.parseValue(arg.keyExpression)
            if (key == 'type') {
                map[name][key] = arg.valueExpression
            } else {
                map[name][key] = arg.valueExpression.text
            }
        }
    }

    private static void createPropertyGetter(ClassNode clazzNode, FieldNode field, FieldNode documentField, Map map) {
        def fieldName = map?.field ? map?.field : field.name
        def otype = map?.type as PropertyExpression
        def resultVar = varX('result', field.type)
        def resultBlock = block(declS(resultVar, new EmptyExpression()))
        if (otype) {
            if (otype.text.endsWith('LINK')) {
                resultBlock.addStatement(assignS(resultVar, ctorX(field.type, args(castX(document, callX(varX(documentField), 'field', args(constX(fieldName))))))))
            }
        } else {
            resultBlock.addStatement(assignS(varX(resultVar), castX(field.type, callX(varX(documentField), 'field', args(constX(fieldName))))))
        }
        resultBlock.addStatement(returnS(varX(resultVar)))
        def method = new MethodNode("get${field.name.capitalize()}", ACC_PUBLIC, field.type, [] as Parameter[], [] as ClassNode[], resultBlock)
        clazzNode.addMethod(method)
    }

    private static void createPropertySetter(ClassNode clazzNode, FieldNode field, FieldNode documentField, Map map) {
        def fieldName = map?.field ? map?.field : field.name
        def otype = map?.type as PropertyExpression
        def setterBlock = block()
        def setterParam = param(field.type, field.name)
        def setterVar = varX(setterParam)
        def args = args(constX(fieldName))
        if (otype) {
            if (otype.text.endsWith('LINK')) {
                args.addExpression(propX(varX(setterVar), 'document'))
            }
        } else {
            args.addExpression(setterVar)
        }
        setterBlock.addStatement stmt(callX(varX(documentField), 'field', args))
        def method = new MethodNode("set${field.name.capitalize()}", ACC_PUBLIC, ClassHelper.VOID_TYPE, params(setterParam), [] as ClassNode[], setterBlock)
        clazzNode.addMethod(method)
    }


}
