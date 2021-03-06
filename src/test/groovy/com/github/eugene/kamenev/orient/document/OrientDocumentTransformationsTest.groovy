package com.github.eugene.kamenev.orient.document
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.sql.OCommandSQL
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory
import spock.lang.Specification

class OrientDocumentTransformationsTest extends Specification {

    ODatabaseDocumentTx db

    OrientGraphFactory orientGraphFactory

    def setup() {
        orientGraphFactory = new OrientGraphFactory('memory:documentTest', 'admin', 'admin')
        db = orientGraphFactory.noTx.rawGraph
        if (!db.exists()) {
            db.create()
        }
    }

    def cleanup() {
       // db.close()
    }

    def 'test that transformation applied right'() {
        given: 'test person entity class'
            def person = new Person(firstName: 'AnyName')
        when: 'start transaction and save document'
            db.begin()
                person.save()
            db.commit()
        then: 'assert that firstName equals to assigned before'
            assert person.firstName == 'AnyName'
        and: 'invoke count method on class'
            assert Person.count() == 1
        and: 'check iterate method call'
            assert Person.iterate().collect().size() == 1
    }

    def 'test query execution method with different params'() {
        given: 'insert new persons'
            db.begin()
                new Person(lastName: 'Simpson', firstName: 'Gomer').save()
                new Person(lastName: 'Simpson', firstName: 'Lara').save()
                new Person(lastName: 'Simpson', firstName: 'Bart').save()
            db.commit()
        when: 'execute queries'
            Person gomer = Person.executeQuery('select from Person where firstName=?', true, 'Gomer')
            Person lara = Person.executeQuery('select from Person where firstName=:firstName and lastName like :lastName', true, [firstName: 'Lara', lastName: '%impso%'])
            Person bart = Person.executeQuery('select from Person where firstName=? and lastName like ?', true, 'Bart', '%imps%')
        then: 'check retrieved values'
            assert gomer.firstName == 'Gomer'
            assert lara.firstName == 'Lara'
            assert bart.firstName == 'Bart'
        and: 'invoke count check again'
            assert Person.count() == 4
        and: 'check iterate method call'
            assert Person.iterate().collect().size() == 4
    }

    def 'test OType.LINK OrientDB relationship'() {
        given: 'test relationship creation'
            def person = new Person(city: new City(title: 'New York'))
        when: 'persist documents to orientdb'
            db.begin()
                person.save()
            db.commit()
        Person nPerson = Person.executeQuery('select from Person where city.title = ?', true, 'New York')
        then: 'check that entities have generated ids'
            person.id != null
            person.city.id != null
            nPerson.city.id != null
    }

    def 'test OType.EMBEDDED OrientDB relationship'() {
        given: 'test relationship creation'
            def phones = ['900000', '800000', '400000']
            def person = new Person(profile: new Profile(phones: phones, isPublic: true))
        when: 'persist document to orientdb'
            db.begin()
                person.save()
            db.commit()
        Person person1 = Person.executeQuery('select from Person where profile[isPublic] = ?', true, true)
        then: 'check entities'
            person1 != null
            person1.profile != null
            person1.profile.isPublic
            person1.profile.phones.size() == 3
    }

    def 'test OType.LINKLIST OrientDB relationship'() {
        given: 'test relationship creation'
            def cities = [new City(title: 'Almaty'), new City(title: 'Astana')]
            def person = new Person(cities: cities)
        when: 'persist document to orientdb'
            db.begin()
                person.save()
            db.commit()
        Person person1 = Person.executeQuery('select from Person where cities.size() > ?', true, 0)
        then: 'check entities'
            person1.cities.size() == 2

    }

    def 'test OType.LINKSET OrientDB relationship'() {
        given: 'test relationship creation'
            def cities = [new City(title: 'Almaty'), new City(title: 'Astana')]
            def person = new Person(citiesSet: cities)
        when: 'persist document to orientdb'
            db.begin()
                person.save()
            db.commit()
        Person person1 = Person.executeQuery('select from Person where citiesSet.size() > ?', true, 0)
        then: 'check entities'
            person1.citiesSet.size() == 2
    }

    def 'test formula feature'() {
        given: 'create City and Person entities'
          OCommandSQL sql = new OCommandSQL('DELETE FROM City')
          db.command(sql).execute()
          def city = new City(title: 'Las Vegas')
          def person = new Person()
        when: 'persist into database'
            db.begin()
                city.save()
            db.commit()
        then: 'call formula method'
         person.cityFormula.size() == 1
    }
}
