package com.groovy.orient.graph

import groovy.transform.CompileStatic

@Vertex
@CompileStatic
class Person {
    String firstName
    String lastName
    City livesIn
    List<City> visitedCities

    static mapping = {
        livesIn(edge: Lives)
        visitedCities(edge: Visited)
    }
}
