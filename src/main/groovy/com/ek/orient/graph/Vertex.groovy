package com.ek.orient.graph

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotation for marking classes that will wrap OrientVertex
 *
 * @author @eugenekamenev
 * @since 0.1.0
 */
@Target([ElementType.TYPE])
@Retention(RetentionPolicy.SOURCE)
@GroovyASTTransformationClass(['com.ek.orient.graph.VertexTransformation'])
@interface Vertex {
    /**
     * Vertex class name in OrientDB
     * @return
     */
    String value() default ''
}