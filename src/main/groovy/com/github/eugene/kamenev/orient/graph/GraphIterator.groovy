package com.github.eugene.kamenev.orient.graph

import com.orientechnologies.orient.core.iterator.ORecordIteratorClass

/**
 * Class entity iterator
 * @param <ENTITY>
 */
class GraphIterator<ENTITY> implements Iterable<ENTITY>, Iterator<ENTITY> {

    ORecordIteratorClass iterator
    Class<ENTITY> clazz

    GraphIterator(ORecordIteratorClass iterator, Class<ENTITY> clazz) {
        this.iterator = iterator
        this.clazz = clazz
    }

    @Override
    Iterator<ENTITY> iterator() {
        this
    }

    @Override
    boolean hasNext() {
        return iterator.hasNext()
    }

    @Override
    ENTITY next() {
        return OrientGraphHelper.transformVertexToEntity(clazz, iterator.next())
    }

    @Override
    void remove() {
        iterator.remove()
    }
}
