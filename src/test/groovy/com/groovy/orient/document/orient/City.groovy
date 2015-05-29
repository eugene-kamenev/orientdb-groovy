package com.groovy.orient.document.orient

import com.groovy.orient.document.OrientDocument

@OrientDocument
class City {
	String id
	String title

	static mapping = {
		id(field: '@rid')
	}
}