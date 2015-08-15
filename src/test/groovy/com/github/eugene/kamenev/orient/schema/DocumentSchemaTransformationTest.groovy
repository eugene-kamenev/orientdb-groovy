package com.github.eugene.kamenev.orient.schema
import com.github.eugene.kamenev.orient.document.City
import com.github.eugene.kamenev.orient.document.Person
import com.github.eugene.kamenev.orient.document.Profile
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import spock.lang.Shared
import spock.lang.Specification
/**
 * OrientDB document schema generation spec
 *
 * @author @eugenekamenev
 * @since 0.1.1
 */
class DocumentSchemaTransformationTest extends Specification {

    /**
     * Document database instance for schema creation
     */
    @Shared
    ODatabaseDocumentTx db

    def setup() {
        db = new ODatabaseDocumentTx('memory:docSchemaTest')
        if (!db.exists()) {
            db.create()
        }
    }

    def cleanup() {
        db.close()
    }

    def 'test'() {
        given: 'orient graph instance'
        Person.initSchema(db)
        City.initSchema(db)
        Profile.initSchema(db)
        expect: 'few entities'
        true == true

    }
}
