package com.github.eugene.kamenev.orient.schema.document

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.metadata.schema.OType
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
        db = new ODatabaseDocumentTx('memory:documentSchemaTest')
        if (!db.exists()) {
            db.create()
        }
    }

    def cleanup() {
        db.close()
    }

    def 'test document schema creation'() {
        given: 'call init schema methods'
            User.initSchema(db)
            Category.initSchema(db)
            Product.initSchema(db)
            User.initSchemaLinks(db)
            Category.initSchemaLinks(db)
            Product.initSchemaLinks(db)
        when: 'get db schema and classes'
            def schema = db.metadata.schema
            def userClass = schema.getClass('UserCollection')
            def productClass = schema.getClass('ProductCollection')
            def categoryClass = schema.getClass('CategoryCollection')
        then: 'check classes are not null'
            userClass != null
            productClass != null
            categoryClass != null
        and: 'check properties'
            userClass.propertiesMap().containsKey('name')
            userClass.propertiesMap().containsKey('boughtProducts')
            productClass.propertiesMap().containsKey('date_released')
            productClass.propertiesMap().containsKey('product_categories')
            productClass.propertiesMap().containsKey('title')
            categoryClass.propertiesMap().containsKey('title')
            categoryClass.propertiesMap().containsKey('parent_category')
            categoryClass.propertiesMap().containsKey('subCategories')
            categoryClass.propertiesMap().containsKey('products')
        and: 'check indexes'
            userClass.indexes.find { it.name == 'UserCollection.name' && it.type == 'NOTUNIQUE' }
            productClass.indexes.find { it.name == 'ProductCollection.title' && it.type == 'UNIQUE'}
            productClass.indexes.find { it.name == 'ProductCollection.date_released' && it.type == 'NOTUNIQUE' }
            categoryClass.indexes.find { it.name == 'CategoryCollection.title' && it.type == 'UNIQUE_HASH_INDEX' }
        and: 'check linked properties type and linkedClass names'
            userClass.propertiesMap()['boughtProducts'].type == OType.LINKLIST
            userClass.propertiesMap()['boughtProducts'].linkedClass.name == 'ProductCollection'
            productClass.propertiesMap()['product_categories'].type == OType.LINKLIST
            productClass.propertiesMap()['product_categories'].linkedClass.name == 'CategoryCollection'
            categoryClass.propertiesMap()['products'].type == OType.LINKLIST
            categoryClass.propertiesMap()['parent_category'].type == OType.LINK
            categoryClass.propertiesMap()['parent_category'].linkedClass.name == 'CategoryCollection'
            categoryClass.propertiesMap()['subCategories'].type == OType.LINKSET
            categoryClass.propertiesMap()['subCategories'].linkedClass.name == 'CategoryCollection'
    }
}
