package com.github.eugene.kamenev.orient

import com.github.eugene.kamenev.orient.document.OrientDocumentHelper
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import groovy.transform.CompileStatic
/**
 * OrientDB groovy extension methods for documents
 * @since 0.1.0
 *
 * @author @eugenekamenev
 */
@CompileStatic
class OrientDSL {



    /**
     * Get single document by @rid as entity instance
     * @since 0.1.0
     *
     * @param clazz
     * @param rid
     * @return
     */
    static <T> T get(Class<T> clazz, String rid) {
        OrientDocumentHelper.transformDocument(clazz, (ODocument) new ODocument().getDatabaseIfDefined().getRecord(new ORecordId(rid)))
    }
}
