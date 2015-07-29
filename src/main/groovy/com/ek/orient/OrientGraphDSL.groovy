package com.ek.orient
import com.ek.orient.graph.OrientGraphHelper
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory
import com.tinkerpop.blueprints.impls.orient.OrientVertex
import com.tinkerpop.gremlin.java.GremlinPipeline
import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.FromString

/**
 * OrientDB groovy extension methods for graphs
 *
 * @author @eugenekamenev
 * @since 0.1.0
 */
@CompileStatic
class OrientGraphDSL {

    /**
     * Creates new temporary OrientDB vertex
     * @since 0.1.0
     *
     * @param orientGraph
     * @param clazz
     * @param properties
     * @return
     */
    static <T> T 'new'(OrientGraph orientGraph, Class<T> clazz, ... properties) {
        OrientGraphHelper.new(orientGraph, clazz, properties)
    }

    /**
     * Get entity for vertex id
     * @since 0.1.0
     *
     * @param orientGraph
     * @param clazz
     * @param id
     * @return
     */
    static <T> T get(OrientBaseGraph orientGraph, Class<T> clazz, id) {
        OrientGraphHelper.get(orientGraph, clazz, id)
    }

    /**
     * Start Gremlin pipe query from vertex
     * @since 0.1.0
     *
     * @param orientVertex
     * @return
     */
    static GremlinPipeline pipe(OrientVertex orientVertex) {
        OrientGraphHelper.pipe(orientVertex)
    }

    /**
     * Create edge
     * @since 0.1.0
     *
     * @param target
     * @param to
     * @param clazz
     * @return
     */
    static <T> T createEdge(OrientVertex target, Vertex to, Class<T> clazz) {
        OrientGraphHelper.createEdge(target, to, clazz)
    }

    /**
     * Extension method for
     * @see GremlinPipeline#toList()
     * with auto convertation into entity collection
     * @since 0.1.0
     *
     * @param pipeline
     * @param clazz
     * @return
     */
    static <T> List<T> toList(GremlinPipeline pipeline, Class<T> clazz) {
        OrientGraphHelper.toList(pipeline, clazz)
    }
    /**
     * Execute graph query for resulting entity
     * @since 0.1.0
     *
     * @param clazz
     * @param query
     * @param singleResult
     * @param params
     * @return
     */
    static <T> T graphQuery(Class<T> clazz, String query, boolean singleResult = false, ...params) {
        OrientGraphHelper.executeQuery(clazz, query, singleResult, params)
    }

    /**
     * Start OrientDB transaction
     * and execute closure inside transaction
     * @since 0.1.0
     *
     * @param dbf
     * @param closure
     * @return
     */
    static <T> T withTransaction(OrientGraphFactory dbf, @ClosureParams(value = FromString, options = 'com.tinkerpop.blueprints.impls.orient.OrientGraph') Closure<T> closure) {
        def orientGraph = (OrientGraph) OrientGraph.activeGraph
        if (!orientGraph) {
            orientGraph = dbf.tx
        }
        try {
            orientGraph.begin()
            def result = closure.call(orientGraph)
            orientGraph.commit()
            return result
        } catch (Exception e) {
            orientGraph.rollback()
            throw e
        } finally {
            orientGraph.shutdown(false)
        }
    }
}
