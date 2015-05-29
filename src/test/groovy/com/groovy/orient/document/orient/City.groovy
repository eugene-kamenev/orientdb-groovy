package com.groovy.orient.document.orient

import com.groovy.orient.document.OrientDocument
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