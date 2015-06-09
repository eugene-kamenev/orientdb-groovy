package com.groovy.orient.graph

import com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.graph.gremlin.OGremlinHelper
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

@Stepwise
class OrientGraphTransformationTest extends Specification {
    @Shared
    OPartitionedDatabasePoolFactory factory

    @Shared
    ODatabaseDocumentTx db

    def setup() {
        OGremlinHelper.global().create()
        factory = new OPartitionedDatabasePoolFactory()
        db = factory.get('memory:tests', 'root', '123456').acquire()
        if (!db.exists()) {
            db.create()
        }
    }

    def cleanup() {
        db.close()
        factory.close()
    }

    def 'test that transformations applied right'() {
        given: 'orient graph instance and few entities'
        def orient = new OrientGraph(db)
        orient.begin()
        and:
        def first = new Person(firstName: 'Gomer55', lastName: 'Simpson')
        def second = new Person(firstName: 'Bart', lastName: 'Simpson')
        def newYork = new City(title: 'New York')
        def amsterdam = new City(title: 'Amsterdam')
        when:
        def edge1 = first.addToVisitedCities(newYork)
        def edge2 = first.addToVisitedCities(amsterdam)
        def edge3 = second.addToVisitedCities(amsterdam)
        def edge4 = newYork.addToVisitedPersons(second)
        def edge5 = first.addToLivesIn(amsterdam)
        def edge6 = first.addToLivesIn(newYork)
        orient.commit()
        def nPerson = orient.get(Person, second.id)
        then:
        first.firstName == 'Gomer55'
        edge1.in.id == first.id
        edge1.out.id == newYork.id
        edge2.out.id == amsterdam.id
        edge5.in.id == first.id
        first.visitedCities.size() > 0
        first.livesIn == edge5.out
        first.vertex.pipe().in('Visited').has('city_title', 'New York').count() == 1
        second.vertex.pipe().in('Visited').has('city_title', 'New York').count() == 1
        second.visitedCities.size() > 0
        newYork.visitedPersons.size() == 2
        nPerson.vertex.pipe().in('Visited').count() == 2
        nPerson.visitedCities.size() == 2
        amsterdam.visitedPersons.size() == 2
        amsterdam.vertex.pipe().out('Visited').count() == 2
    }
}
