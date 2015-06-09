package com.groovy.orient.graph

import groovy.transform.CompileStatic

@Vertex
@CompileStatic
class City {
    String title
    String title2

    List<Person> visitedPersons
    List<Person> citizens

    static mapping = {
        title(field: 'city_title')
        visitedPersons(edge: Visited)
        citizens(edge: Lives)
    }
}
