package com.github.eugene.kamenev.orient.schema

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * AST Transformation for OrientDB schema initialization feature
 * Supports types and indexes
 *
 * @author @eugenekamenev
 * @since 0.1.1
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class SchemaTransformation extends AbstractASTTransformation {
    @Override
    void visit(ASTNode[] nodes, SourceUnit source) {
        // OrientSchema annotation node
        AnnotationNode annotation = (AnnotationNode) nodes[0];
        // Annotated class node
        ClassNode annotatedClass = (ClassNode) nodes[1];

    }
}
