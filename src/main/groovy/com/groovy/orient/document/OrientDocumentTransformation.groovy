package com.groovy.orient.document

import com.groovy.orient.document.util.ASTUtil
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.NamedArgumentListExpression
import org.codehaus.groovy.ast.expr.TupleExpression
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

    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        AnnotationNode annotation = (AnnotationNode) nodes[0];
        ClassNode annotatedClass = (ClassNode) nodes[1];
        String clusterName = ASTUtil.parseValue(annotation.members.value, annotatedClass.nameWithoutPackage)
        List<String> transients = ASTUtil.parseValue(annotatedClass.getField('transients')?.initialExpression, []) as List<String>
        def documentFieldNode = annotatedClass.addField('document', ACC_PRIVATE | ACC_FINAL, document, new EmptyExpression())
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
            createPropertyGetter(annotatedClass, it, mapping[it.name])
            createPropertySetter(annotatedClass, it, mapping[it.name])
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
            map[name][ASTUtil.parseValue(arg.keyExpression)] = ASTUtil.parseValue(arg.valueExpression)
        }
    }

    private static void createPropertyGetter(ClassNode clazzNode, FieldNode field, Map map) {
        def fieldName = map?.field ? map?.field : field.name
        def getterStatement = returnS(castX(field.type, callX(varX('document'), 'field', args(constX(fieldName)))))
        def method = new MethodNode("get${field.name.capitalize()}", ACC_PUBLIC, field.type, [] as Parameter[], [] as ClassNode[], getterStatement)
        clazzNode.addMethod(method)
    }

    private static void createPropertySetter(ClassNode clazzNode, FieldNode field, Map map) {
        def fieldName = map?.field ? map?.field : field.name
        def setterParam = param(field.type, field.name)
        def setterVar = varX(setterParam)
        def setterStatement = stmt(callX(varX('document'), 'field', args(constX(fieldName), setterVar)))
        def method = new MethodNode("set${field.name.capitalize()}", ACC_PUBLIC, ClassHelper.VOID_TYPE, params(setterParam), [] as ClassNode[], setterStatement)
        clazzNode.addMethod(method)
    }


}
