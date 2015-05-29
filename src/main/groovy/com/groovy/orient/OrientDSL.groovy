package com.groovy.orient

import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

/**
 * @author @eugenekamenev
 */
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
        (List<T>) transformDocumentCollection(clazz, OType.LINKLIST, result)
    }

    static <T> T transformDocument(Class<T> clazz, ODocument document) {
        clazz.newInstance(document)
    }

    static <T> Iterable<T> transformDocumentCollection(Class<T> clazz, OType type, Iterable<?> documents) {
        def collection = documents.collect {
            transformDocument(clazz, (ODocument) it)
        }
        switch (type) {
            case OType.LINKSET:
                return new LinkedHashSet<T>(collection);
                break;
        }
        return collection
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    static ODocument transformEntity(object) {
        object.document
    }

    static List<ODocument> transformEntityCollection(Iterable<?> entities) {
        entities.collect {
            transformEntity(it)
        }
    }
}
