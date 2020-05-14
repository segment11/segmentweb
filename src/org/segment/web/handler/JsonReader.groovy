package org.segment.web.handler

import groovy.transform.CompileStatic

@CompileStatic
@Singleton
class JsonReader {
    JsonTransformer jsonTransformer = new DefaultJsonTransformer()

    public <T> T read(byte[] bytes, Class<T> clz) {
        jsonTransformer.read(bytes, clz)
    }
}
