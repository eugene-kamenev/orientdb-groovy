package com.github.eugene.kamenev.orient.schema
import com.github.eugene.kamenev.orient.graph.Person
import com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import spock.lang.Shared
import spock.lang.Specification

class SchemaTransformationTest extends Specification {
    @Shared
    OPartitionedDatabasePoolFactory factory

    @Shared
    ODatabaseDocumentTx db

    def setup() {
        factory = new OPartitionedDatabasePoolFactory()
        db = factory.get('memory:test', 'admin', 'admin').acquire()
        if (!db.exists()) {
            db.create()
        } else {
            db.drop()
            db = factory.get('memory:test', 'admin', 'admin').acquire()
            db.create()
        }
    }

    def cleanup() {
        db.close()
        factory.close()
    }



    def 'test'() {
        given: 'orient graph instance'
        Person.init(db)
        expect: 'few entities'
        true == true

    }
}
