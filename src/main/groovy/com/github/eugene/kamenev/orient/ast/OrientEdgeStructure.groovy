package com.github.eugene.kamenev.orient.ast

import com.github.eugene.kamenev.orient.ast.util.ASTUtil
import com.tinkerpop.blueprints.Direction
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.Parameter

import static org.codehaus.groovy.ast.tools.GeneralUtils.*
/**
 * OrientEdgeStructure
 *
 * @author @eugenekamenev
 * @since 0.1.1
 */
@CompileStatic
class OrientEdgeStructure extends OrientStructure {
    /**
     * Edge direction class node
     */
    static final ClassNode directionNode = ClassHelper.make(Direction).plainNodeReference

    OrientEdgeStructure(ClassNode entityClassNode, AnnotationNode annotationNode, String injectedFieldName) {
        super(entityClassNode, annotationNode, injectedFieldName)
    }

    /**
     * Override to add additional step for edge transformation
     * @return
     */
    @Override
    def initTransformation() {
        super.initTransformation()
        def inNode = ASTUtil.annotationValue(annotation.members.from) as ClassNode
        def outNode = ASTUtil.annotationValue(annotation.members.to) as ClassNode
        assert inNode != null
        assert outNode != null
        def inStatement = returnS(ctorX(inNode.plainNodeReference, args(callX(varX(injectedObject), 'getVertex', args(propX(classX(directionNode), 'IN'))))))
        entity.addMethod('getIn', ACC_PUBLIC, inNode.plainNodeReference, [] as Parameter[], [] as ClassNode[], inStatement)
        def outStatement = returnS(ctorX(outNode.plainNodeReference, args(callX(varX(injectedObject), 'getVertex', args(propX(classX(directionNode), 'OUT'))))))
        entity.addMethod('getOut', ACC_PUBLIC, outNode.plainNodeReference, [] as Parameter[], [] as ClassNode[], outStatement)
    }
}
