package com.groovy.orient.document.tests

import com.groovy.orient.document.orient.Person
import com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory
import com.orientechnologies.orient.core.record.impl.ODocument
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

@Stepwise
class OrientDocumentTransformationsTest extends Specification {

    @Shared
    def factory

    @Shared
    def db

    def setup() {
        factory = new OPartitionedDatabasePoolFactory()
        db = factory.get('memory:test', 'admin', 'admin').acquire()
        if (!db.exists()) {
            db.create()
        }
    }

    def cleanup() {
        db.close()
    }

    def 'test that transformation applied right'() {
        given: 'test person entity class'
        def person = new Person(firstName: 'Gomer Simpson')
        when: 'start transaction and save document'
        db.begin()
        person.save()
        db.commit()
        then: 'assert that firstName equals to assigned before'
        assert person.firstName == 'First Name'
    }

    /*def 'test query execution method #type'(String query, args, def list) {
        given:
        db.begin()
        new Person(lastName: 'Simpson 1').save()
        new Person(lastName: 'Simpson 2').save()
        new Person(lastName: 'Simpson 3').save()
        db.commit()
        expect:
        Person.executeQuery(query, args)
        where:
        query                                                 | args                      | list
        'select from Person where lastName=?'                 | 'Simpson 1'               | list.size() == 1
        'select from Person where lastName=:namedParam'       | [namedParam: 'Simpson 2'] | list.size() == 1
        'select from Person where lastName=? and firstName=?' | ['Simpson 3', null]       | list.size() == 1


    }*/

    def 'test that person was saved to db'() {
        given: 'find person by firstName'
        List<ODocument> result = Person.executeQuery('select from Person where firstName = ?', 'Gomer Simpson')
        when:
        List<Person> personList = Person.transformCollection(result, Person)
        then:
        result.size() == 1
        personList.size() == 1
        and:
        personList.first().firstName == 'Gomer Simpson'
    }
}
