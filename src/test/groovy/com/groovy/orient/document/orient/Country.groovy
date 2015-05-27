package com.groovy.orient.document.orient

class Country {
	String id
	String title

	static hasMany = [cities: City]

	static mapping = {
		id id: true
	}
}
