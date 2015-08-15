package com.github.eugene.kamenev.orient.schema.document

import com.github.eugene.kamenev.orient.document.OrientDocument
import com.orientechnologies.orient.core.metadata.schema.OType
import groovy.transform.CompileStatic

@OrientDocument(initSchema = true, value = 'UserCollection')
@CompileStatic
class User {
    String name = 'guest'
    List<Product> boughtProducts

    static mapping = {
        name(index: 'notUnique')
        boughtProducts(type: OType.LINKLIST)
    }
}

@OrientDocument(initSchema = true, value = 'ProductCollection')
@CompileStatic
class Product {
    String title
    Date releaseDate
    BigDecimal price
    List<Category> categories

    static mapping = {
        title(index: 'unique')
        releaseDate(index: 'notUnique', field: 'date_released')
        categories(type: OType.LINKLIST, field: 'product_categories')
    }
}

@OrientDocument(initSchema = true, value = 'CategoryCollection')
@CompileStatic
class Category {
    String title
    Category parent
    List<Product> products
    Set<Category> subCategories

    static mapping = {
        title(index: 'hashUnique')
        products(type: OType.LINKLIST)
        subCategories(type: OType.LINKSET)
        parent(type: OType.LINK, field: 'parent_category')
    }
}
