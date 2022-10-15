package org.segment.web.handler

import spock.lang.Specification

class RegexMatchHandlerTest extends Specification {
    def 'match'() {
        given:
        def handler = new RegexMatchHandler() {
            @Override
            Object hi(Req req, Resp resp) {
                return null
            }
        }
        handler.context = '/a'
        handler.pattern = ~/^\/b\/.+$/
        expect:
        handler.isRequestMatch('GET', '/a/b/c', null)
    }
}
