package com.groovy.orient.document

import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
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

    /**
        * Provides simple static query execution
        *  Note that orient database should be already attached to thread
        *
        * @param query
        * @param params
        * @return
        */
    static def executeQuery(String query, ... params) {
        def orientQuery = new OSQLSynchQuery<ODocument>(query)
        new ODocument().getDatabaseIfDefined().command(orientQuery).execute(params)
    }

    /**
        *  Just wrapping ODocument with our wrapper class
        * @param document
        * @param tClass
        * @return
        */
    static <T> T transform(ODocument document, Class<T> tClass) {
        tClass.newInstance(document)
    }

    /**
        * Wrapping a collection of documents with wrapper class
        * @param documents
        * @param tClass
        * @return
        */
    static <T, C extends Iterable<ODocument>> List<T> transformCollection(C documents, Class<T> tClass) {
        documents.collect { ODocument document ->
            transform(document, tClass)
        }
    }
}
