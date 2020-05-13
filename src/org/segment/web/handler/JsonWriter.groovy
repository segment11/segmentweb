package org.segment.web.handler

import groovy.transform.CompileStatic

@CompileStatic
@Singleton
class JsonWriter {
    JsonTransformer jsonTransformer = new DefaultJsonTransformer()

    String json(Object obj) {
        jsonTransformer.json(obj)
    }
}
