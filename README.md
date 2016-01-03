# What can Groovy make for OrientDB?
[ ![Download](https://api.bintray.com/packages/eugene-kamenev/maven/orientdb-groovy/images/download.svg) ](https://bintray.com/eugene-kamenev/maven/orientdb-groovy/_latestVersion)

This project contains Groovy AST Transformations trying to mimic grails-entity style.
All useful information you can find in Spock tests dir. Document API and Graph API with gremlin are supported. Built with OrientDB 2.1.0 and Apache Groovy 2.4.4

# Latest news:
###03.01.2016
Work on this project will be continued as native GORM plugin, see this: [link](https://github.com/eugene-kamenev/orientdb-groovy/issues/23)

#Gradle config
```groovy
repositories {
    jcenter()
}

dependencies {
    compile "com.github.eugene-kamenev:orientdb-groovy:0.1.1" 
}
```
Example how to use it with Spring Boot
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
        city(type: OType.LINK)
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
###Schema Initialization
By default orientdb-groovy will not generate schema init methods. To generate initialize schema methods from entity classes you should set initSchema=true. You can define index type of a field, values can be:
'dictionary', 'hashUnique', 'hashNotUnique', 'notUnique', 'fulltext', 'fulltextHash', 'unique', 'dictionaryHash', 'spatial'

For example:
```groovy
@OrientDocument(initSchema = true, value = 'ProductCollection')
@CompileStatic
class Product {
    String title
    Date releaseDate
    BigDecimal price
    List<Category> categories

    static mapping = {
        title(index: 'unique')
        releaseDate(index: 'notUnique', field: 'date_released')
        categories(type: OType.LINKLIST, field: 'product_categories')
    }
}
```
Then orientdb-groovy will add special static methods that you should call only once to generate schema in OrientDB.
```
Product.initSchema(db)
Product.initSchemaLinks(db)
```
The first method will create properties with indexes and types, and second creates links between classes if they defined.

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
