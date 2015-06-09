package com.groovy.orient.graph

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
/**
 * @author @eugenekamenev
 */
@Target([ElementType.TYPE])
@Retention(RetentionPolicy.SOURCE)
@GroovyASTTransformationClass(['com.groovy.orient.graph.EdgeTransformation'])
@interface Edge {
    Class from() // Direction out
    Class to() // Direction in
    String value() default '' // orient name
}