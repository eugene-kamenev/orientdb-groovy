package com.groovy.orient.graph

import com.orientechnologies.orient.core.metadata.schema.OType
import groovy.transform.CompileStatic

@Vertex
@CompileStatic
class Person {
    String firstName
    String lastName
    City livesIn
    List<City> visitedCities
    City cityLink
    List<City> cityLinkedList
    Set<City> cityLinkedSet
    List<City> notVisitedCities

    static mapping = {
        livesIn(edge: Lives)
        visitedCities(edge: Visited)
        cityLink(type: OType.LINK)
        cityLinkedList(type: OType.LINKLIST)
        cityLinkedSet(type: OType.LINKSET)
        notVisitedCities(formula: 'select from City where @rid not in (?)', params: this.getVisitedCities())
    }
}
