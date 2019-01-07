package com.github.eugene.kamenev.orient.graph

import com.orientechnologies.orient.graph.gremlin.OGremlinHelper
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory
import spock.lang.Shared
import spock.lang.Specification

class OrientGraphTransformationTest extends Specification {

    @Shared
    OrientGraphFactory factory

    @Shared
    def db

    def setup() {
        factory = new OrientGraphFactory('memory:graphTest', 'admin', 'admin')
        db = factory.getNoTx().rawGraph
        OGremlinHelper.global().create()
        if (!db.exists()) {
            db.create()
        }
    }

    def cleanup() {
        db.close()
        factory.close()
    }

    def 'test that transformations applied right'() {
        given: 'orient graph instance'
        def orient = new OrientGraph(db,false)
        orient.begin()
        and: 'few entities'
        def first = new Person(firstName: 'Gomer', lastName: 'Simpson')
        def second = new Person(firstName: 'Bart', lastName: 'Simpson')
        def cities = first.visitedCities?.isEmpty()
        def newYork = new City(title: 'New York')
        def amsterdam = new City(title: 'Amsterdam')
        when: 'create relations between entities'
        def edge1 = first.addToVisitedCities(newYork)
        def edge2 = first.addToVisitedCities(amsterdam)
        def edge3 = second.addToVisitedCities(amsterdam)
        def edge4 = newYork.addToVisitedPersons(second)
        def edge5 = first.addToLivesIn(amsterdam)
        def edge6 = first.addToLivesIn(newYork)
        and: 'create relations using links'
        first.setCityLink(amsterdam)
        first.setCityLinkedList([newYork, amsterdam])
        first.setCityLinkedSet([amsterdam, newYork] as HashSet)
        and: 'commit changes'
        orient.commit()
        and: 'test extension get method'
        first = orient.get(Person, first.id)
        second = orient.get(Person, second.id)
        newYork = orient.get(City, newYork.id)
        amsterdam = orient.get(City, amsterdam.id)
        then: 'check properties'
        first.firstName == 'Gomer'
        first.lastName == 'Simpson'
        newYork.title == 'New York'
        amsterdam.title == 'Amsterdam'
        and: 'check relationships'
        edge1.in.id == first.id
        edge1.out.id == newYork.id
        edge2.out.id == amsterdam.id
        edge5.in.id == first.id
        first.visitedCities.size() > 0
        first.livesIn.id == edge5.out.id
        edge5.out.citizens.size() == 1
        edge5.out.id == amsterdam.id
        newYork.citizens.find {it.id == first.id} != null
        second.visitedCities.size() > 0
        newYork.visitedPersons.size() == 2
        second.visitedCities.size() == 2
        amsterdam.visitedPersons.size() == 2
        and: 'check realtions using links'
        first.cityLink.id == amsterdam.id
        first.cityLinkedList.size() == 2
        first.cityLinkedSet.size() == 2
        first.cityLinkedList[0].id == newYork.id
        and: 'check gremlin queries'
        first.vertex.pipe().in('Visited').has('title', 'New York').count() == 1
        second.vertex.pipe().in('Visited').has('title', 'New York').count() == 1
        second.vertex.pipe().in('Visited').count() == 2
        amsterdam.vertex.pipe().out('Visited').count() == 2
        and: 'check formula feature'
        first.notVisitedCities.size() == 0
        new Person().notVisitedCities.size() == 2
        and: 'check gremlin pipe extension method toList implementation'
        first.vertex.pipe().in('Visited').has('title', 'New York').toList(City).size() == 1
        amsterdam.vertex.pipe().out('Visited').toList(Person).size() == 2
    }
}
