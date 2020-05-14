package org.segment.web.handler

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic

@CompileStatic
class DefaultJsonTransformer implements JsonTransformer {
    @Override
    String json(Object obj) {
        new ObjectMapper().writeValueAsString(obj)
    }

    @Override
    <T> T read(byte[] bytes, Class<T> clz) {
        new ObjectMapper().readValue(bytes, clz)
    }
}
