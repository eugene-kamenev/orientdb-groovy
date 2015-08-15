package com.github.eugene.kamenev.orient.graph
import com.github.eugene.kamenev.orient.ast.OrientStructure
import com.tinkerpop.blueprints.impls.orient.OrientVertex
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
    static final ClassNode delegateNode = ClassHelper.make(Delegate).plainNodeReference
    static final ClassNode orientVertexNode = ClassHelper.make(OrientVertex).plainNodeReference

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
        def vertexFieldNode = annotatedClass.addField('vertex', ACC_PUBLIC | ACC_FINAL, orientVertexNode, new EmptyExpression())
        OrientStructure orientStructure = new OrientStructure(annotatedClass, annotation, 'vertex')
        def annotationNode = new AnnotationNode(delegateNode)
        def delegateTransformation = new DelegateASTTransformation()
        delegateTransformation.visit([annotationNode, vertexFieldNode] as ASTNode[], source)
        orientStructure.initMapping()
        orientStructure.initTransformation()
        orientStructure.clean()
    }
}
