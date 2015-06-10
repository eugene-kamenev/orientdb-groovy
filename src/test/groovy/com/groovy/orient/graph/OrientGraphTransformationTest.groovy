package com.groovy.orient.graph
import com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.graph.gremlin.OGremlinHelper
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import spock.lang.Shared
import spock.lang.Specification

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
        given: 'orient graph instance'
        def orient = new OrientGraph(db)
        orient.begin()
        and: 'few entities'
        def first = new Person(firstName: 'Gomer', lastName: 'Simpson')
        def second = new Person(firstName: 'Bart', lastName: 'Simpson')
        def newYork = new City(title: 'New York')
        def amsterdam = new City(title: 'Amsterdam')
        when: 'create relations between entities'
        def edge1 = first.addToVisitedCities(newYork)
        def edge2 = first.addToVisitedCities(amsterdam)
        def edge3 = second.addToVisitedCities(amsterdam)
        def edge4 = newYork.addToVisitedPersons(second)
        def edge5 = first.addToLivesIn(amsterdam)
        def edge6 = first.addToLivesIn(newYork)
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
        newYork.citizens.find { it.id == first.id } != null
        second.visitedCities.size() > 0
        newYork.visitedPersons.size() == 2
        second.visitedCities.size() == 2
        amsterdam.visitedPersons.size() == 2
        and: 'check gremlin queries'
        first.vertex.pipe().in('Visited').has('title', 'New York').count() == 1
        second.vertex.pipe().in('Visited').has('title', 'New York').count() == 1
        second.vertex.pipe().in('Visited').count() == 2
        amsterdam.vertex.pipe().out('Visited').count() == 2
        and: 'check gremlin pipe extension method toList implementation'
        first.vertex.pipe().in('Visited').has('title', 'New York').toList(City).size() == 1
        amsterdam.vertex.pipe().out('Visited').toList(Person).size() == 2
    }
}