package com.groovy.orient.document.orient
import com.groovy.orient.document.OrientDocument
import groovy.transform.CompileStatic

@CompileStatic
@OrientDocument
class Person {
	String id
	String firstName
	String lastName
	Date birthDate

	List<String> strings = []

	static transients = ['strings']

	static mapping = {
		id(field: '@rid')
		birthDate(field: 'birthdate')
	}
}
