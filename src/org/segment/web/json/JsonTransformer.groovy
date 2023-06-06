package org.segment.web.json

import groovy.transform.CompileStatic

@CompileStatic
interface JsonTransformer {
    String json(Object obj)

    public <T> T read(byte[] bytes, Class<T> clz)
}