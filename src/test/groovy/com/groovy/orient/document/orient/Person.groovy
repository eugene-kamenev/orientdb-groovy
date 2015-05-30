package com.groovy.orient.document.orient
import com.groovy.orient.document.OrientDocument
import com.orientechnologies.orient.core.metadata.schema.OType
import groovy.transform.CompileStatic

@CompileStatic
@OrientDocument
class Person {
	String id
	String firstName
	String lastName

	City city

	Profile profile

	List<String> strings = []

	List<City> cities

	Set<City> citiesSet

	static transients = ['strings']

	static mapping = {
		id(field: '@rid')
		city(type: OType.LINK, fetch: 'eager')
		profile(type: OType.EMBEDDED)
		cities(type: OType.LINKLIST, fetch: 'eager')
		citiesSet(type: OType.LINKSET)
	}
}
