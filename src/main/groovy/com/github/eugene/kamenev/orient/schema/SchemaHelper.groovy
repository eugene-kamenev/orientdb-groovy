package com.github.eugene.kamenev.orient.schema

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.metadata.schema.OProperty
import com.orientechnologies.orient.core.metadata.schema.OSchema
import com.orientechnologies.orient.core.metadata.schema.OType
import groovy.transform.CompileStatic

/**
 * OrientDB Schema creation helper methods
 *
 * @author @eugenekamenev
 * @since 0.1.1
 */
@CompileStatic
class SchemaHelper {
    /**
     *
     * @param tx
     * @param mapping
     * @param className
     * @param classType
     */
    static void initClass(ODatabaseDocumentTx tx, Map mapping, String className, String classType) {
        def schema = tx.getMetadata().getSchema()
        def oclass = getOrCreateClass(schema, className)
        if (classType) {
            oclass.setSuperClasses([schema.getClass(classType)])
        }
        createSimpleProperties(oclass, mapping)
    }

    static OClass getOrCreateClass(OSchema schema, String className) {
        def oclass = schema.getClass(className)
        if (!oclass) {
            oclass = schema.createClass(className)
        }
        oclass
    }

    static OProperty getOrCreateProperty(OClass oClass, String property, Class type) {
        if (property != '@rid') {
            def prop = oClass.getProperty(property)
            if (!prop) {
                prop = oClass.createProperty(property, OType.getTypeByClass(type))
            }
            return prop
        }
        null
    }

    static void createSimpleProperties(OClass oClass, Map mapping) {
        mapping.each {prop, args ->
            createPropertyFromMap(oClass, (Map) args, (String) prop)
        }
    }

    static void createPropertyFromMap(OClass oClass, Map<String, ?> mapping, String propertyName) {
        def index = mapping.index
        def clazz = (Class) mapping.clazz
        if (index instanceof String) {
            index = getIndexTypeFromString(index)
        }
        def property = getOrCreateProperty(oClass, propertyName, clazz)
        if (index) {
            def classIndex = oClass.getInvolvedIndexes(propertyName)[0]
            if (!classIndex) {
                property.createIndex((OClass.INDEX_TYPE) index)
            }
        }
    }

    static OClass.INDEX_TYPE getIndexTypeFromString(String indexType) {
        switch (indexType) {
            case 'dictionary': return OClass.INDEX_TYPE.DICTIONARY; break;
            case 'hashUnique': return OClass.INDEX_TYPE.UNIQUE_HASH_INDEX; break;
            case 'hashNotUnique': return OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX; break;
            case 'notUnique': return OClass.INDEX_TYPE.NOTUNIQUE; break;
            case 'fulltext': return OClass.INDEX_TYPE.FULLTEXT; break;
            case 'fulltextHash': return OClass.INDEX_TYPE.FULLTEXT_HASH_INDEX; break;
            case 'unique': return OClass.INDEX_TYPE.UNIQUE; break;
            case 'dictionaryHash': return OClass.INDEX_TYPE.DICTIONARY_HASH_INDEX; break;
            case 'spatial': return OClass.INDEX_TYPE.SPATIAL; break;
            default: return OClass.INDEX_TYPE.UNIQUE; break;
        }
    }
}
