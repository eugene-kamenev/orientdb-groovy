package com.github.eugene.kamenev.orient.schema.graph
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory
import spock.lang.Specification

/**
 * OrientDB Graph schema generation spec
 *
 * @author @eugenekamenev
 * @since 0.1.1
 */
class GraphSchemaTransformationTest extends Specification {

    /**
     * Orient graph factory
     */
    OrientGraphFactory factory

    /**
     * Document database instance for schema creation
     */
    ODatabaseDocumentTx db

    def setup() {
        factory = new OrientGraphFactory('memory:graphSchemaTest', 'admin', 'admin')
        db = factory.noTx.rawGraph
        if (!db.exists()) {
            db.create()
        }
    }

    def cleanup() {
        db.close()
        factory.close()
    }

    def 'test that database schema created successfully'() {
        given: 'call init schema methods'
            Person.initSchema(db)
            Pet.initSchema(db)
            Owns.initSchema(db)
            Person.initSchemaLinks(db)
            Pet.initSchemaLinks(db)
            Owns.initSchemaLinks(db)
        when: 'get db schema and classes'
            def schema = db.metadata.schema
            def personClass = schema.getClass('Person')
            def petClass = schema.getClass('Pet')
            def ownedClass = schema.getClass('Owns')
        then: 'check classes are not null'
            personClass != null
            petClass != null
            ownedClass != null
        and: 'check superclasses'
            personClass.getSuperClassesNames().contains('V')
            ownedClass.getSuperClassesNames().contains('E')
            petClass.getSuperClassesNames().contains('V')
        and: 'check created class propeties'
            personClass.propertiesMap().containsKey('first_name')
            personClass.propertiesMap().containsKey('last_name')
            ownedClass.propertiesMap().containsKey('on_date')
            petClass.propertiesMap().containsKey('pet_name')
            petClass.propertiesMap().containsKey('birthDate')
        and: 'check created indexes'
            personClass.indexes.find { it.name == 'Person.first_name' && it.type == 'NOTUNIQUE' }
            personClass.indexes.find { it.name == 'Person.last_name' && it.type == 'UNIQUE' }
            ownedClass.indexes.find { it.name == 'Owns.on_date' && it.type == 'NOTUNIQUE' }
            petClass.indexes.find { it.name == 'Pet.pet_name' && it.type == 'NOTUNIQUE' }
            petClass.indexes.find { it.name == 'Pet.birthDate' && it.type == 'NOTUNIQUE' }
    }
}
