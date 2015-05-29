package com.groovy.orient

import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import groovy.transform.CompileStatic

@CompileStatic
class OrientDSL {

   /**
     * Provides simple static query execution
     *  Note that gdsl.orient database should be already attached to thread
     *
     * @param query
     * @param params
     * @return <T>
     */
    static <T> List<T> executeQuery(Class<T> clazz, String query, ... params) {
        def orientQuery = new OSQLSynchQuery<ODocument>(query)
        List<ODocument> result = (List<ODocument>) new ODocument().getDatabaseIfDefined().command(orientQuery).execute(params)
        result.collect {
            transform(clazz, it)
        }
    }

    static <T> T transform(Class<T> clazz, ODocument document) {
        clazz.newInstance(document)
    }
}
