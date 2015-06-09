package com.groovy.orient.document

import groovy.transform.CompileStatic

@OrientDocument
@CompileStatic
class City {
	String id
	String title

	static mapping = {
		id(field: '@rid')
	}
}