package com.groovy.orient.graph

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * @author @eugenekamenev
 */
@CompileStatic
@GroovyASTTransformation(phase=CompilePhase.SEMANTIC_ANALYSIS)
class EdgeTransformation extends AbstractASTTransformation {

	@Override
	void visit(ASTNode[] nodes, SourceUnit source) {

	}
}
