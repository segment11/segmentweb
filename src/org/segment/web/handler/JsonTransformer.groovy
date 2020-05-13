package org.segment.web.handler

import groovy.transform.CompileStatic

@CompileStatic
interface JsonTransformer {
    String json(Object obj)
}