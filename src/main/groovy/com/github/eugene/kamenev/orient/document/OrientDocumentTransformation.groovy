package com.github.eugene.kamenev.orient.document

import com.github.eugene.kamenev.orient.ast.OrientStructure
import com.orientechnologies.orient.core.record.impl.ODocument
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.DelegateASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
/**
 * Transformation for OrientDB document usage
 * @since 0.1.0
 *
 * @author @eugenekamenev
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class OrientDocumentTransformation extends AbstractASTTransformation {
    /**
     * ClassNodes for usage in transformation
     */
    static final ClassNode document = ClassHelper.make(ODocument).plainNodeReference
    static final ClassNode delegateNode = ClassHelper.make(Delegate).plainNodeReference

    /**
     * Transformation starts here, visiting the class node
     * @since 0.1.0
     *
     * @param nodes
     * @param source
     */
    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        AnnotationNode annotation = (AnnotationNode) nodes[0];
        ClassNode annotatedClass = (ClassNode) nodes[1];
        def documentFieldNode = annotatedClass.addField('document', ACC_PUBLIC | ACC_FINAL, document, new EmptyExpression())
        def annotationNode = new AnnotationNode(delegateNode)
        def delegateTransformation = new DelegateASTTransformation()
        delegateTransformation.visit([annotationNode, documentFieldNode] as ASTNode[], source)
        def orientStructure = new OrientStructure(annotatedClass, annotation, 'document')
        orientStructure.initMapping()
        orientStructure.initTransformation()
        orientStructure.clean()
    }
}
