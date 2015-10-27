package com.github.eugene.kamenev.orient.graph

import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import com.tinkerpop.blueprints.impls.orient.OrientVertex
import com.tinkerpop.gremlin.java.GremlinPipeline
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
/**
 * OrientDB graph helper methods
 *
 * @author @eugenekamenev
 * @since 0.1.0
 */
@CompileStatic
class OrientGraphHelper {

    /**
     * Creates new temporary vertex
     * @since 0.1.0
     *
     * @param orientGraph
     * @param clazz
     * @param properties
     * @return
     */
    static <T> T 'new'(OrientGraph orientGraph, Class<T> clazz, ... properties) {
        clazz.newInstance(orientGraph.addTemporaryVertex("$clazz.simpleName", properties))
    }

    /**
     * Wrap vertex with entity class
     * @since 0.1.0
     *
     * @param entityClass
     * @param vertex
     * @return
     */
    static <T> T transformVertexToEntity(Class<T> entityClass, Object vertex) {
        if (vertex) {
            return entityClass.newInstance(vertex)
        }
        return null
    }

    /**
     * Dynamic method to get vertex instance from entity
     * @since 0.1.0
     *
     * @param clazz
     * @param object
     * @return
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    static OrientVertex getVertexFromEntity(object) {
        return (OrientVertex) object?.vertex
    }

    /**
     * Transforms collection of OrientDB vertex instances into collection of entities
     * @since 0.1.0
     *
     * @param collection
     * @param type
     * @param entityClass
     * @return
     */
    static <T, C extends Collection<T>> C transformVertexCollectionToEntity(Class<T> entityClass, def collection, OType type = null) {
        def result = ((Iterable)collection).collect {
            transformVertexToEntity(entityClass, it)
        }
        switch (type) {
            case OType.LINKSET:
                return new LinkedHashSet<T>(result)
                break;
        }
        return result
    }

    /**
     * Transforms collection of entities into collection of vertices
     * @since 0.1.0
     *
     * @param collection
     * @return
     */
    static Collection<OrientVertex> transformEntityCollectionToVertex(Iterable collection) {
        collection?.collect {
            getVertexFromEntity(it)
        }
    }

    /**
     * Create Gremlin pipe query starting from vertex
     * @since 0.1.0
     *
     * @param orientVertex
     * @return
     */
    static GremlinPipeline pipe(OrientVertex orientVertex) {
        new GremlinPipeline(orientVertex)
    }

    /**
     * Edge creator helper
     * @since 0.1.0
     *
     * @param target
     * @param to
     * @param clazz
     * @return
     */
    static <T> T createEdge(OrientVertex target, com.tinkerpop.blueprints.Vertex to, Class<T> clazz) {
        (T) clazz.newInstance(target.addEdge("${clazz.simpleName}", to))
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
        (List<T>) transformVertexCollectionToEntity(clazz, (Iterable<OrientVertex>) pipeline.toList())
    }

    /**
     * Get new entity instance for vertex
     * @since 0.1.0
     *
     * @param orientGraph
     * @param clazz
     * @param id
     * @return
     */
    static <T> T get(OrientBaseGraph orientGraph, Class<T> clazz, id) {
        (T) clazz.newInstance(orientGraph.getVertex(id))
    }

    /**
     * Execute native OrientDB query and transform result into entity.
     * Supports @param singleResult for getting only one result
     * @since 0.1.0
     *
     * @param resultClass
     * @param query
     * @param singleResult
     * @param params
     * @return
     */
    static <T> T executeQuery(Class<T> resultClass, String query, boolean singleResult, ... params) {
        def sqlQuery = new OSQLSynchQuery<com.tinkerpop.blueprints.Vertex>(query)
        def result = OrientGraph.activeGraph.command(sqlQuery).execute(params)
        if (singleResult) {
            return (T) transformVertexToEntity(resultClass, (OrientVertex) ((Iterable) result)[0])
        } else {
            return (T) transformVertexCollectionToEntity(resultClass, (Iterable) result, OType.LINKLIST)
        }
    }

    static Long count(String className) {
        return OrientGraph.activeGraph.rawGraph.countClass(className)
    }

    static <T> Iterable<T> iterate(Class clazz, String className) {
        return new GraphIterator<T>(OrientGraph.activeGraph.rawGraph.browseClass(className), clazz)
    }
}
