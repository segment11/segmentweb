package org.segment.web.json

import groovy.transform.CompileStatic

@CompileStatic
interface JsonTransformer {
    String json(Object obj)

    <T> T read(byte[] bytes, Class<T> clz)
}