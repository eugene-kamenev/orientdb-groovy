package com.groovy.orient.document
import com.orientechnologies.orient.core.record.impl.ODocument
import groovy.transform.CompileStatic

/**
 * Every entity class should implement this trait
 *
 * @author @eugenekamenev
 */
@CompileStatic
trait AsDocument {

   /**
      * Simple delegation to document instance
      */
    @Delegate
    ODocument document
}
