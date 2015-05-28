package com.groovy.orient.document

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotation should be used to mark entity class
 * @param value can be used to choose different orientDB class name, if none
 *
 * @author @eugenekamenev
 */
@Target([ElementType.TYPE])
@Retention(RetentionPolicy.SOURCE)
@GroovyASTTransformationClass(['com.groovy.orient.document.OrientDocumentTransformation'])
@interface OrientDocument {
    String value() default ''
}