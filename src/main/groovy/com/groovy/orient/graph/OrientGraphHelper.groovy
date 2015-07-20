package com.groovy.orient.graph

import com.orientechnologies.orient.core.db.record.OIdentifiable
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import com.tinkerpop.blueprints.impls.orient.OrientVertex
import com.tinkerpop.gremlin.java.GremlinPipeline
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

@CompileStatic
class OrientGraphHelper {

    static <T> T 'new'(OrientGraph orientGraph, Class<T> clazz, ... properties) {
        clazz.newInstance(orientGraph.addTemporaryVertex("$clazz.simpleName", properties))
    }

    public static <T> T transformVertexToEntity(Class<T> entityClass, OIdentifiable vertex) {
        if (vertex) {
            return entityClass.newInstance(vertex)
        }
        return null
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    public static OrientVertex getVertexFromEntity(Class clazz, object) {
        return (OrientVertex) object?.vertex
    }

    public
    static <T, C extends Collection<T>> C transformVertexCollectionToEntity(Iterable<OrientVertex> collection, OType type, Class<T> entityClass) {
        def result = collection.collect {
            transformVertexToEntity(entityClass, (OrientVertex) it)
        }
        switch (type) {
            case OType.LINKSET:
                return new LinkedHashSet<T>(result)
                break;
        }
        return result
    }

    public
    static <T, C extends Collection<T>> C transformVertexCollectionToEntity(Class<T> entityClass, Iterable<OrientVertex> collection) {
        transformVertexCollectionToEntity(collection, null, entityClass)
    }

    public static Collection<OrientVertex> transformEntityCollectionToVertex(Iterable collection) {
        collection?.collect {
            getVertexFromEntity(null, it)
        }
    }

    static GremlinPipeline pipe(OrientVertex orientVertex) {
        new GremlinPipeline(orientVertex)
    }

    static <T> T createEdge(OrientVertex target, com.tinkerpop.blueprints.Vertex to, Class<T> clazz) {
        (T) clazz.newInstance(target.addEdge("${clazz.simpleName}", to))
    }

    static <T> List<T> toList(GremlinPipeline pipeline, Class<T> clazz) {
        (List<T>) transformVertexCollectionToEntity((Iterable<OrientVertex>) pipeline.toList(), (OType) null, clazz)
    }

    static <T> T get(OrientBaseGraph orientGraph, Class<T> clazz, id) {
        (T) clazz.newInstance(orientGraph.getVertex(id))
    }

    static <T> T executeQuery(Class<T> resultClass, String query, boolean singleResult, ... params) {
        def sqlQuery = new OSQLSynchQuery<com.tinkerpop.blueprints.Vertex>(query)
        def result = OrientGraph.activeGraph.command(sqlQuery).execute(params)
        if (singleResult) {
            return (T) transformVertexToEntity(resultClass, (OrientVertex) ((Iterable) result)[0])
        } else {
            return (T) transformVertexCollectionToEntity((Iterable) result, OType.LINKLIST, resultClass)
        }
    }
}
