package com.github.eugene.kamenev.orient.ast
import com.github.eugene.kamenev.orient.ast.util.ASTUtil
import com.github.eugene.kamenev.orient.ast.util.EntityProperty
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.codehaus.groovy.ast.expr.Expression
/**
 * OrientProperty implementation
 * Mostly the same for vertex/edge/document
 *
 * @author @eugenekamenev
 * @since 0.1.1
 */
@CompileStatic
@InheritConstructors
class OrientProperty extends EntityProperty {

    /**
     * {@inheritDoc}
     */
    void readMapping() {
        defaultMappingIterator {arg ->
            def key = ASTUtil.parseValue(arg.keyExpression)
            def val
            switch (key) {
                case ['edge', 'type', 'params', 'index']:
                    val = arg.valueExpression
                    break;
                default:
                    val = arg.valueExpression.text
                    break;
            }
            ((EntityProperty) thisObject).mapping[key] = val
        }
        this.mapping.oclass = fieldNode.type
        if (!this.mapping.field) {
            this.mapping.field = fieldNode.name
        }
    }

    /**
     * If property is mapped specially
     * @return
     */
    boolean isSpecialType() {
        link || linkedList || edge || embedded || formula || linkedSet
    }

    /**
     * Get orient type from mapping
     * @return
     */
    String getOrientType() {
        ((Expression) mapping.type)?.text
    }

    /**
     * if this node is mapped like a formula
     * @param node
     * @return boolean
     */
    boolean isFormula() {
        mapping.formula != null
    }

    /**
     * if this node is mapped like a
     * {@link com.orientechnologies.orient.core.metadata.schema.OType#LINK}
     *
     * @param node
     * @return boolean
     */
    boolean isLink() {
        orientType?.endsWith('LINK')
    }

    /**
     * if this node is mapped like a
     * {@link com.orientechnologies.orient.core.metadata.schema.OType#LINKLIST}
     *
     * @param node
     * @return boolean
     */
    boolean isLinkedList() {
        orientType?.endsWith('LINKLIST')
    }

    /**
     * if this node is mapped like a
     * {@link com.orientechnologies.orient.core.metadata.schema.OType#LINKSET}
     *
     * @param node
     * @return boolean
     */
    boolean isLinkedSet() {
        orientType?.endsWith('LINKSET')
    }

    /**
     * if this node is mapped like a
     * {@link com.orientechnologies.orient.core.metadata.schema.OType#EMBEDDED}
     *
     * @return
     */
    boolean isEmbedded() {
        orientType?.endsWith('EMBEDDED')
    }

    /**
     * if this node is mapped like a
     * {@link com.tinkerpop.blueprints.impls.orient.OrientEdge}
     *
     * @param node
     * @return boolean
     */
    boolean isEdge() {
        mapping.edge != null
    }

    /**
     * if this node has index mapping

     * @param node
     * @return boolean
     */
    boolean hasIndex() {
        mapping.index != null
    }

    /**
     * if property is simple typed
     *
     * @return boolean
     */
    boolean isSimpleTyped() {
        mapping.oclass in ASTUtil.simpleTypes
    }

    /**
     * if property is a link typed
     *
     * @return
     */
    boolean isLinkedType() {
        link || linkedList || linkedSet
    }
}
