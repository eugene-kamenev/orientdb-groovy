package com.groovy.orient.document.orient

class City {
	Integer id
	String title

	static belongsTo = [country: Country]

	static mapping = {
		id id: true
	}
}