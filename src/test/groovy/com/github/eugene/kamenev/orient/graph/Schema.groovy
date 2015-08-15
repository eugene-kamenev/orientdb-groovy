package com.github.eugene.kamenev.orient.graph

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
        firstName(index: 'unique')
        livesIn(edge: Lives)
        visitedCities(edge: Visited)
        cityLink(type: OType.LINK)
        cityLinkedList(type: OType.LINKLIST)
        cityLinkedSet(type: OType.LINKSET)
        notVisitedCities(formula: 'select from City where @rid not in (?)', params: this.getVisitedCities())
    }
}

@Edge(from = Person, to = City)
@CompileStatic
class Visited {
}

@Edge(from = Person, to = City)
@CompileStatic
class Lives {
    Date since
}

@Vertex
@CompileStatic
class City {
    String title
    Date dateCreated

    List<Person> visitedPersons
    List<Person> citizens

    static mapping = {
        dateCreated(field: 'date_created')
        visitedPersons(edge: Visited)
        citizens(edge: Lives)
    }
}
