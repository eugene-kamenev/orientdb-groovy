package com.github.eugene.kamenev.orient.document

import com.orientechnologies.orient.core.iterator.ORecordIteratorClass

/**
 * Class entity iterator
 * @param <ENTITY> entityClass
 */
class DocumentIterator<ENTITY> implements Iterable<ENTITY>, Iterator<ENTITY> {

    ORecordIteratorClass iterator
    Class<ENTITY> clazz

    DocumentIterator(ORecordIteratorClass iterator, Class<ENTITY> clazz) {
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
        return OrientDocumentHelper.transformDocument(clazz, iterator.next())
    }

    @Override
    void remove() {
        iterator.remove()
    }
}
