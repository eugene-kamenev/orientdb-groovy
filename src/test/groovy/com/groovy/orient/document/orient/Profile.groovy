package com.groovy.orient.document.orient

import com.groovy.orient.document.OrientDocument
import com.orientechnologies.orient.core.metadata.schema.OType
import groovy.transform.CompileStatic

@CompileStatic
@OrientDocument
class Profile {
	Boolean isPublic
	List<String> phones
	City city

	static mapping = {
		city(type: OType.LINK)
	}
}
