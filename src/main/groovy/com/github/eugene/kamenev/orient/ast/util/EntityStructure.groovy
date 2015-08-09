package com.github.eugene.kamenev.orient.ast.util

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.objectweb.asm.Opcodes

/**
 * This class can read needed information from entity class node and apply modifications on it
 *
 * @author @eugenekamenev
 * @since 0.1.1
 */
@CompileStatic
abstract class EntityStructure<T extends EntityProperty> implements Opcodes {

    /**
     * Entity class node
     */
    final ClassNode entity

    /**
     * Annotation on entity class node
     */
    final AnnotationNode annotation

    /**
     * Transient fields list
     */
    List transients = []

    /**
     * Full property schema
     */
    Map<String, T> entityProperties = [:]

    /**
     * All entity fields
     */
    List<FieldNode> allFields = []

    /**
     * Constructs entity structure instance
     *
     * @param entity
     * @param annotationNode
     */
    EntityStructure(ClassNode entity, AnnotationNode annotation) {
        this.entity = entity
        this.annotation = annotation
    }

    /**
     * Initialization for parsing entity class node
     */
    def initMapping() {
        def readTransients = ASTUtil.parseValue(entity.getField('transients')?.initialExpression, []) as List<String>
        if (readTransients) {
            transients.addAll(readTransients)
        }
        allFields = entity.fields.findAll {
            !(it.name in transients) && !(it.static) && (it.modifiers != ACC_TRANSIENT)
        }
        allFields.each {
            entityProperties << [(it.name): mapProperty(it)]
        }
        this
    }

    /**
     * Property mapping instance
     * @param node
     * @return
     */
    abstract T mapProperty(FieldNode node)

    /**
     * Clean node from 'static mapping' and 'static transient' fields
     */
    def clean() {
        ASTUtil.removeProperty(entity, 'mapping')
        ASTUtil.removeProperty(entity, 'transients')
        allFields.each {
            ASTUtil.removeProperty(entity, it.name)
        }
    }
}
