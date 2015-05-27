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
	Date birthdate

	List<String> strings = []

	List<City> cities

	static transients = ['strings']

	static mapping = {
		id field: "rid"
		firstName field: "first_name"
		lastName field: "last_name"
		cities formula: "select from City where id = ? order by id desc", params: [id]
	}
}
