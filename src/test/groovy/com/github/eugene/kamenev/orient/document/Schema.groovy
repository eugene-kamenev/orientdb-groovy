package com.github.eugene.kamenev.orient.document

import com.orientechnologies.orient.core.metadata.schema.OType
import groovy.transform.CompileStatic

@CompileStatic
@OrientDocument(initSchema = true)
class Profile {
    Boolean isPublic
    List<String> phones
    City city

    static mapping = {
        city(type: OType.LINK)
    }
}

@CompileStatic
@OrientDocument(initSchema = true)
class Person {
    String id
    String firstName
    String lastName

    City city

    Profile profile

    List<String> strings = []

    List<City> cities

    Set<City> citiesSet

    List<City> cityFormula

    static transients = ['strings']

    static mapping = {
        id(field: '@rid')
        city(type: OType.LINK, fetch: 'eager')
        profile(type: OType.EMBEDDED)
        cities(type: OType.LINKLIST, fetch: 'eager')
        citiesSet(type: OType.LINKSET)
        cityFormula(formula: 'select from City where @rid <> :id', params: [id: this.getId()])
    }
}

@OrientDocument(initSchema = true)
@CompileStatic
class City {
    String id
    String title

    static mapping = {
        id(field: '@rid')
    }
}