package com.github.eugene.kamenev.orient.schema.graph

import com.github.eugene.kamenev.orient.graph.Edge
import com.github.eugene.kamenev.orient.graph.Vertex
import groovy.transform.CompileStatic

@Vertex(initSchema = true)
@CompileStatic
class Person {
    String firstName
    String lastName
    List<Pet> ownedPets

    static mapping = {
        firstName index: 'notUnique', field: 'first_name'
        lastName index: 'unique', field: 'last_name'
        ownedPets edge: Owns
    }
}

@Edge(from = Person, to = Pet, initSchema = true)
@CompileStatic
class Owns {
    Date on
    static mapping = {
        on field: 'on_date', index: 'notUnique'
    }
}

@Vertex(initSchema = true)
@CompileStatic
class Pet {
    String name
    Date birthDate
    Person owner

    static mapping = {
        name index: 'notUnique', field: 'pet_name'
        birthDate index: 'notUnique'
        owner edge: Owns
    }
}