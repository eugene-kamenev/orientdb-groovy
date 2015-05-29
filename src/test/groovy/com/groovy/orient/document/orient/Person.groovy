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

	List<String> strings = []

	static transients = ['strings']

	static mapping = {
		id(field: '@rid')
		city(field: 'city', type: OType.LINK)
	}
}
