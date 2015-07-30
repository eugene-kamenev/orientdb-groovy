package com.github.eugene.kamenev.orient.graph

import groovy.transform.CompileStatic

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
