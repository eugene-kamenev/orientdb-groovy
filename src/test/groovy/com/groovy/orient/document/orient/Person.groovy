package com.groovy.orient.document.orient

import com.groovy.orient.document.OrientDocument
import com.groovy.orient.document.AsDocument
import groovy.transform.CompileStatic

@CompileStatic
@OrientDocument
class Person implements AsDocument {
	String id
	String firstName
	String lastName
	Date birthDate

	List<String> strings = []

	static transients = ['strings']

	static mapping = {
		id field: "rid"
		birthDate field: "birthdate"
	}
}
