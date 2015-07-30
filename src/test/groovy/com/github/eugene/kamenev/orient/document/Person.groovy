package com.github.eugene.kamenev.orient.document

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
