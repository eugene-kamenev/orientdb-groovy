package com.groovy.orient.document.orient

import com.groovy.orient.document.OrientDocument
import com.groovy.orient.document.AsDocument
import groovy.transform.CompileStatic

@OrientDocument
@CompileStatic
class Person implements AsDocument {
	String id
	String firstName
	String lastName
	Date birthdate

	List<String> strings = []

	static transients = ['strings']

	static mapping = {

	}
}
