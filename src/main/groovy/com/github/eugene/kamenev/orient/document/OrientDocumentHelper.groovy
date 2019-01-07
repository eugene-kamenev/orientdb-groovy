package com.github.eugene.kamenev.orient.document

import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
/**
 * OrientDB document helper methods
 *
 * @author @eugenekamenev
 * @since 0.1.1
 */
@CompileStatic
class OrientDocumentHelper {

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
    static <T> Object executeQuery(Class<T> clazz, String query, boolean singleResult, ... params) {
        def orientQuery = new OSQLSynchQuery<ODocument>(query)
        ODatabaseDocumentInternal db = ODatabaseRecordThreadLocal.instance().get()
        List<ODocument> result = (List<ODocument>) db.command(orientQuery).execute(params)
        if (singleResult) {
            return transformDocument(clazz, result[0])
        }
        return transformDocumentCollection(clazz, result, OType.LINKLIST)
    }

    /**
     * Dynamic mehtod for getting document instance from entity
     * @since 0.1.1
     *
     * @param object
     * @return ODocument instance
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    static ODocument transformEntity(object) {
        object.document
    }

    /**
     * Transforms collection of entities into collection of documents
     * @since 0.1.1
     *
     * @param entities
     * @return
     */
    static List<ODocument> transformEntityCollection(Iterable<?> entities) {
        entities.collect {
            transformEntity(it)
        }
    }

    /**
     * Transforms collection of documents into collection of entities
     * @since 0.1.1
     *
     * @param clazz
     * @param type
     * @param documents
     * @return
     */
    static <T> Iterable<T> transformDocumentCollection(Class<T> clazz, def documents, OType type = null) {
        def collection = ((Iterable)documents).collect {
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
     * Transform document into entity instance
     * @since 0.1.1
     *
     * @param clazz
     * @param document
     * @return
     */
    static <T> T transformDocument(Class<T> clazz, Object document) {
        if (!document) {
            return null
        }
        return clazz.newInstance(document)
    }

    /**
     * Get class size in OrientDB
     * @since 0.1.2
     *
     * @param className
     * @return
     */
    static Long count(String className) {
        return new ODocument().databaseIfDefined.countClass(className)
    }

    /**
     * Iterate through entity collection
     * @since 0.1.2
     *
     * @param clazz
     * @param className
     * @return
     */
    static <T> Iterable<T> iterate(Class clazz, String className) {
        ODatabaseDocumentInternal db = ODatabaseRecordThreadLocal.instance().get()
        return new DocumentIterator<T>(db.browseClass(className), clazz)
    }

}
