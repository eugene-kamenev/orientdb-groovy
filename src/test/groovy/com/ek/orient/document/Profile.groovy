package com.ek.orient.document

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
