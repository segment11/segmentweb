package org.segment.web.handler

import spock.lang.Specification

class AbstractHandlerTest extends Specification {

    def 'match'() {
        expect:
        AbstractHandler.isUriMatch('/a', '/a')
        AbstractHandler.isUriMatch('/a/*', '/a/b')
        AbstractHandler.isUriMatch('/a/**', '/a/b/c')
        AbstractHandler.isUriMatch('/a/b/c', '/a/b/c')
        AbstractHandler.isUriMatch('/a/b/:name', '/a/b/c')
    }
}
