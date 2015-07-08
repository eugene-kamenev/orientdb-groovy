# What can Groovy make for OrientDB?

In this project I am trying to provide Groovy way to work with orientdb as simple as possible.
This project contains Groovy AST Transformations trying to mimic grails-entity style.
All useful information you can find in Spock tests dir.
For now Document API and Graph API with gremlin are supported.

And here is an example how I will use it with Spring Boot transactions
https://github.com/eugene-kamenev/orientdb-spring-boot-example

This library will transform your entity with direct vertex/edge properties/methods access, no proxies here.

###IDE Support
This lib contains *.gdsl script for IntelliJ IDEA, it will work even in Community Edition, so you will feel very nice code completion. No red 'missing' methods!

##Graph example
```groovy
@Vertex
@CompileStatic
class City {
    String title
    List<Person> visitedPersons
    List<Person> citizens

    static mapping = {
        visitedPersons(edge: Visited)
        citizens(edge: Lives)
    }
}

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

@Edge(from = Person, to = City)
@CompileStatic
class Visited {
    Date visitDate
}

@Edge(from = Person, to = City)
@CompileStatic
class Lives {
    Date since
}
```
##Graph creation
When you define property as connected by edge, orientdb-groovy will generate special methods for adding edges.
```groovy
def first = new Person(firstName: 'First Name')
def second = new Person(firstName: 'First Name 2')
def newYork = new City(title: 'New York')
def amsterdam = new City(title: 'Amsterdam')
def visited = first.addToVisitedCities(newYork)
visited.visitDate = new Date()
def lives = first.addToLivesIn(amsterdam)
lives.since = new Date()
amsterdam.addToCitizens(second)
amsterdam.addToVisitedPersons(second)
db.commit()
```
##Gremlin graph queries
Gremlin pipes are supported.
```groovy
def count = newYork.vertex.pipe().out('Visited').count()
def persons = newYork.vertex.pipe().out('Visited').has('firstName', 'First Name').toList(Person)
```
##Document example
```groovy
@OrientDocument
@CompileStatic // yes it is fully supported
class City {
    String id
    String title

    static mapping = {
        id(field: '@rid')
    }
}

@OrientDocument
@CompileStatic
class Profile {
    String email
    City city
    Date birthday
    List<String> phones
    Boolean isPublic

    Integer years

    static transients = ['years'] // this property will not be persisted

    static mapping = {
        birthday(field: 'birth_date')
        phones(field: 'user_phones')
        city(type: OType.LINK, fetch: 'eager')
    }

    Integer getYears() {
        this.years = TimeCategory.minus(new Date(), this.birthday).years
    }
}

@OrientDocument
@CompileStatic
class Person {
    String id
    String firstName
    String lastName

    Profile profile

    static mapping = {
        id(field: '@rid')
        profile(type: OType.EMBEDDED)
    }
}

@OrientDocument
@CompileStatic
class Country {
    String id
    String title
    List<City> cities

    static mapping = {
        id(field: '@rid')
        cities(type: OType.LINKLIST)
    }
}

```
### Document creation
```groovy
    def phones = ['+900000000', '+800000000', '+7000000']
    def city = new City(title: 'New York')
    def profile = new Profile(isPublic: true, phones: phones, city: city, birthDay: new Date())
    def person = new Person(profile: profile, firstName: 'PersonFirstName', lastName: 'PersonLastName')
    person.save()
```

###Document Quering
```groovy
    def personList = Person.executeQuery('select from Person where firstName=?', 'Bart')
    def personList2 = User.executeQuery('select from User where firstName=:a and lastName like :b', [a: 'Bart', b: '%Simpson%'])
```

Check [OrientDocumentTransformationsTest](https://github.com/eugene-kamenev/orientdb-groovy/blob/master/src/test/groovy/com/groovy/orient/document/tests/OrientDocumentTransformationsTest.groovy) for more details.
