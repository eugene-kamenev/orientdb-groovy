package com.github.eugene.kamenev.orient.graph
import com.github.eugene.kamenev.orient.schema.SchemaHelper
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.metadata.schema.OType
import groovy.transform.CompileStatic

@Vertex
@CompileStatic
class Person {
    String firstName = 'Fucking bullshit'
    String lastName
    City livesIn
    List<City> visitedCities
    City cityLink
    List<City> cityLinkedList
    Set<City> cityLinkedSet
    List<City> notVisitedCities

    static mapping = {
        firstName(index: 'unique')
        livesIn(edge: Lives)
        visitedCities(edge: Visited)
        cityLink(type: OType.LINK)
        cityLinkedList(type: OType.LINKLIST)
        cityLinkedSet(type: OType.LINKSET)
        notVisitedCities(formula: 'select from City where @rid not in (?)', params: this.getVisitedCities())
    }

    static void init(ODatabaseDocumentTx tx) {
        def mapping = [firstName: [index: 'unique', clazz: String],
                       lastName: [clazz: String]]
        def schema = tx.getMetadata().getSchema()
        def oclass = SchemaHelper.getOrCreateClass(schema, 'Person')
        SchemaHelper.createSimpleProperties(oclass, mapping)
    }
}
