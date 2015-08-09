package com.github.eugene.kamenev.orient.ast.util
import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.stmt.BlockStatement

/**
 * Class represents entity property
 * It contains all mapping information about property
 *
 * @author @eugenekamenev
 * @since 0.1.1
 */
@CompileStatic
abstract class EntityProperty {
    /**
     * Entity class node
     */
    ClassNode entityClassNode

    /**
     * Property field node
     */
    FieldNode fieldNode

    /**
     * Contains all information parsed from mapping closure
     */
    Map mapping = [:]

    EntityProperty(ClassNode entityClassNode, FieldNode fieldNode) {
        this.entityClassNode = entityClassNode
        this.fieldNode = fieldNode
        readMapping()
    }

    /**
     * Apply a closure for iterating through mapping parameters
     *
     * @param closure
     */
    void defaultMappingIterator(@ClosureParams(value = FromString,
            options = 'org.codehaus.groovy.ast.expr.MapEntryExpression')
                                     Closure closure) {
        def expression = entityClassNode.getField('mapping')?.initialExpression as ClosureExpression
        def methodCall = ASTUtil.findMethodCallExpression(fieldNode.name, expression?.code as BlockStatement)
        ASTUtil.eachMethodArg(methodCall, closure)
    }



    /**
     * if property is mapped as collection
     *
     * @return boolean
     */
    boolean isCollection() {
        fieldNode.type.plainNodeReference in ASTUtil.collectionNodes
    }

    /**
     * if property has initial value
     *
     * @return boolean
     */
    boolean hasInitialValue() {
        fieldNode.initialExpression != null
    }

    /**
     * Get property type node
     *
     * @return
     */
    ClassNode getNodeType() {
        fieldNode.type
    }

    /**
     * Get property name
     *
     * @return
     */
    String getNodeName() {
        fieldNode.name
    }

    /**
     * If property mapped by collection get it generic type
     * otherwise return default type
     *
     * @return
     */
    ClassNode getCollectionGenericType() {
        if (collection) {
            return nodeType.genericsTypes[0].type.plainNodeReference
        }
        nodeType
    }

    /**
     * Method is called to read expression from mapping closure
     */
    abstract void readMapping()
}
