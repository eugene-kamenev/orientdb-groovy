package com.ek.orient.document

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotation should be used to mark entity class
 *
 * @author @eugenekamenev
 * @since 0.1.0
 */
@Target([ElementType.TYPE])
@Retention(RetentionPolicy.SOURCE)
@GroovyASTTransformationClass(['com.ek.orient.document.OrientDocumentTransformation'])
@interface OrientDocument {

    /**
     * Specifies OrientDB document class name
     * @since 0.1.0
     */
    String value() default ''
}