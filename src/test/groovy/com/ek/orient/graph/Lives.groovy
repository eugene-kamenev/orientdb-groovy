package com.ek.orient.graph

import groovy.transform.CompileStatic

@Edge(from = Person, to = City)
@CompileStatic
class Lives {
    Date since
}
