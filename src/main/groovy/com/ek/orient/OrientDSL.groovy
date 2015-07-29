package com.ek.orient

import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

/**
 * OrientDB groovy extension methods for documents
 * @since 0.1.0
 *
 * @author @eugenekamenev
 */
@CompileStatic
class OrientDSL {

    /**
     * Provides simple static query execution
     * Note that orient database connection
     * should be already attached to thread
     * @since 0.1.0
     *
     * @param query
     * @param params
     * @return <T>
     */
    static <T> List<T> executeQuery(Class<T> clazz, String query, ... params) {
        def orientQuery = new OSQLSynchQuery<ODocument>(query)
        List<ODocument> result = (List<ODocument>) new ODocument().getDatabaseIfDefined().command(orientQuery).execute(params)
        (List<T>) transformDocumentCollection(clazz  , OType.LINKLIST, result)
    }

    /**
     * Get single document by @rid as entity instance
     * @since 0.1.0
     *
     * @param clazz
     * @param rid
     * @return
     */
    static <T> T get(Class<T> clazz, String rid) {
        transformDocument(clazz, (ODocument) new ODocument().getDatabaseIfDefined().getRecord(new ORecordId(rid)))
    }

    /**
     * Transform document into entity instance
     * @since 0.1.0
     *
     * @param clazz
     * @param document
     * @return
     */
    static <T> T transformDocument(Class<T> clazz, Object document) {
        clazz.newInstance(document)
    }

    /**
     * Transforms collection of documents into collection of entities
     * @since 0.1.0
     *
     * @param clazz
     * @param type
     * @param documents
     * @return
     */
    static <T> Iterable<T> transformDocumentCollection(Class<T> clazz, OType type, Iterable<?> documents) {
        def collection = documents.collect {
            transformDocument(clazz, it)
        }
        switch (type) {
            case OType.LINKSET:
                return new LinkedHashSet<T>(collection)
                break;
        }
        return collection
    }

    /**
     * Dynamic mehtod for getting document instance from entity
     * @since 0.1.0
     *
     * @param object
     * @return
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    static ODocument transformEntity(object) {
        object.document
    }

    /**
     * Transforms collection of entities into collection of documents
     * @since 0.1.0
     *
     * @param entities
     * @return
     */
    static List<ODocument> transformEntityCollection(Iterable<?> entities) {
        entities.collect {
            transformEntity(it)
        }
    }
}
