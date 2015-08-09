package com.github.eugene.kamenev.orient.graph

import com.github.eugene.kamenev.orient.ast.util.OrientEdgeStructure
import com.tinkerpop.blueprints.Direction
import com.tinkerpop.blueprints.impls.orient.OrientEdge
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
 * OrientDB entity transformation for @see Edge annotation
 *
 * @author @eugenekamenev
 * @since 0.1.0
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

        def edgeFieldNode = annotatedClass.addField('edge', ACC_PUBLIC | ACC_FINAL, orientEdgeClassNode, new EmptyExpression())
        def annotationNode = new AnnotationNode(delegateNode)
        def delegateTransformation = new DelegateASTTransformation()
        delegateTransformation.visit([annotationNode, edgeFieldNode] as ASTNode[], source)
        def edgeStructure = new OrientEdgeStructure(annotatedClass, annotation, 'edge')
        edgeStructure.initMapping()
        edgeStructure.initTransformation()
        edgeStructure.clean()
    }
}
