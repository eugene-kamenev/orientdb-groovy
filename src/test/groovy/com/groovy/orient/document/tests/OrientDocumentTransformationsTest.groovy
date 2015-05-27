package com.groovy.orient.document.tests

import com.groovy.orient.document.orient.Person
import com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

@Stepwise
class OrientDocumentTransformationsTest extends Specification {

	@Shared
	def factory = new OPartitionedDatabasePoolFactory()

	@Shared
	def db = factory.get('remote:127.0.0.1/test', 'root', '123456').acquire()

	def setup() {
	}

	def cleanup() {
	}

	def 'test that transformation applied right'() {
		given: 'test person entity class'
			Person person = new Person(firstName: 'First Name')
		when: 'explicitly start transaction and save document'
		    db.begin()
		    person.save()
			db.commit()
		then: 'assert that firstName equals to assigned before'
		 	assert person.firstName == 'First Name'
	}
}
