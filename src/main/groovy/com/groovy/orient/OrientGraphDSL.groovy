package com.groovy.orient
import com.groovy.orient.graph.OrientGraphHelper
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import com.tinkerpop.blueprints.impls.orient.OrientVertex
import com.tinkerpop.gremlin.java.GremlinPipeline
import groovy.transform.CompileStatic

@CompileStatic
class OrientGraphDSL {

    static <T> T 'new'(OrientGraph orientGraph, Class<T> clazz, ... properties) {
        OrientGraphHelper.new(orientGraph, clazz, properties)
    }

    static <T> T get(OrientBaseGraph orientGraph, Class<T> clazz, id) {
        OrientGraphHelper.get(orientGraph, clazz, id)
    }

    static GremlinPipeline pipe(OrientVertex orientVertex) {
        OrientGraphHelper.pipe(orientVertex)
    }

    static <T> T createEdge(OrientVertex target, Vertex to, Class<T> clazz) {
        OrientGraphHelper.createEdge(target, to, clazz)
    }

    static <T> List<T> toList(GremlinPipeline pipeline, Class<T> clazz) {
        OrientGraphHelper.toList(pipeline, clazz)
    }

    static <T> T graphQuery(Class clazz, String query, boolean singleResult = false, ... params) {
        OrientGraphHelper.executeQuery(clazz, query, singleResult, params)
    }
}
