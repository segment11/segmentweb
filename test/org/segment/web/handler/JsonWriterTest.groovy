package org.segment.web.handler

import spock.lang.Specification

class DefaultJsonTransformerTest extends Specification {

    def 'json'() {
        expect:
        new DefaultJsonTransformer().json([1: 1]) == '{"1":1}'
    }
}
