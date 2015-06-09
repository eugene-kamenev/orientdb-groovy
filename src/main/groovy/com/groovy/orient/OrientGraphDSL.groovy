package com.groovy.orient

import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import com.tinkerpop.blueprints.impls.orient.OrientVertex
import com.tinkerpop.gremlin.java.GremlinPipeline
import groovy.transform.CompileStatic

@CompileStatic
class OrientGraphDSL {

    static <T> T 'new'(OrientGraph orientGraph, Class<T> clazz, ... properties) {
        (T) clazz.newInstance(orientGraph.addVertex("class:$clazz.simpleName".toString(), properties))
    }

    static <T> T get(OrientGraph orientGraph, Class<T> clazz, id) {
        (T) clazz.newInstance(orientGraph.getVertex(id))
    }

    static GremlinPipeline pipe(OrientVertex orientVertex) {
        new GremlinPipeline(orientVertex)
    }

    static <T> T createEdge(OrientVertex target, OrientVertex to, Class<T> clazz) {
        (T) clazz.newInstance(target.addEdge("${clazz.simpleName}", (Vertex)to))
    }

    static <T> List<T> transformToVertexList(Class<T> vertexClass, List object) {
        return object.collect { Object o ->
            transformToVertex(vertexClass, o)
        }
    }

    static <T> T transformToVertex(Class<T> vertexClass, Object object) {
        vertexClass.newInstance(object)
    }
}
