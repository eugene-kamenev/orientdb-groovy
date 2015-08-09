package com.github.eugene.kamenev.orient.schema

/**
 * Annotation for marking class that will init OrientDB schema
 *
 * @author @eugenekamenev
 * @since 0.1.1
 */
@interface OrientSchema {
    /**
     * Array of entity classes
     * @return
     */
    Class[] classes() default []
}