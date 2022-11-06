package org.segment.web.handler

import spock.lang.Specification

class ChainHandlerTest extends Specification {

    def 'context group'() {
        given:
        def handler = ChainHandler.instance
        handler.list.clear()
        handler.context('/context')
        handler.group('/a') {
            handler.group('/b') {
                handler.get('/c') { req, resp ->
                    resp.end('get c')
                }.post('/c') { req, resp ->
                    resp.end('post c')
                }
            }
        }
        handler.print { String x ->
            println x
        }
        expect:
        handler.list.size() == 2
    }
}
