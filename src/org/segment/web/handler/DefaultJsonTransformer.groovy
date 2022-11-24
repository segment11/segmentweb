package org.segment.web.handler

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import groovy.transform.CompileStatic

@CompileStatic
class DefaultJsonTransformer implements JsonTransformer {
    @Override
    String json(Object obj) {
        def mapper = new ObjectMapper()
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        mapper.serializationInclusion = JsonInclude.Include.NON_NULL
        mapper.writeValueAsString(obj)
    }

    @Override
    <T> T read(byte[] bytes, Class<T> clz) {
        new ObjectMapper().readValue(bytes, clz)
    }
}
