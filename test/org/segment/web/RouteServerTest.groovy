package org.segment.web

import com.github.kevinsawicki.http.HttpRequest
import org.eclipse.jetty.http.HttpStatus
import org.segment.web.handler.ChainHandler
import spock.lang.Specification

class RouteServerTest extends Specification {
    def 'server'() {
        given:
        def handler = ChainHandler.instance
        handler.context('/context').exceptionHandler { req, resp, t ->
            resp.status = 500
            def os = resp.outputStream
            os.write((t.message ?: 'error').bytes)
            os.close()
        }
        handler.group('/a') {
            handler.group('/b') {
                handler.before('/c') { req, resp ->
                    println 'before c filter'
                }.get('/c') { req, resp ->
                    resp.end('get c')
                }.post('/c') { req, resp ->
                    resp.end('post c')
                }.put('/c') { req, resp ->
                    resp.end('put c')
                }.delete('/c') { req, resp ->
                    resp.end('delete c')
                }.options('/d') { req, resp ->
                    resp.end('options d')
                }.after('/c') { req, resp ->
                    println 'after c filter'
                }.get('/json') { req, resp ->
                    [1: 1]
                }
            }
            handler.group('/test') {
                handler.get('/halt') { req, resp ->
                    resp.halt()
                }.get('/exception') { req, resp ->
                    throw new RuntimeException('xxx')
                }.afterAfter('/exception') { req, resp ->
                    println 'after exception'
                }
            }
            handler.group('/regex') {
                handler.get(~/^\/book.+$/) { req, resp ->
                    resp.end('get book')
                }.post(~/^\/book.+$/) { req, resp ->
                    resp.end('post book')
                }.put(~/^\/book.+$/) { req, resp ->
                    resp.end('put book')
                }.delete(~/^\/book.+$/) { req, resp ->
                    resp.end('delete book')
                }.options(~/^\/book.+$/) { req, resp ->
                    resp.end('options book')
                }
            }
        }
        handler.print { String x ->
            println x
        }
        def server = RouteServer.instance
        server.start()
        Thread.sleep(1000)
        expect:
        HttpRequest.get('http://localhost:5000/context/a/b/c').body() == 'get c'
        HttpRequest.post('http://localhost:5000/context/a/b/c').body() == 'post c'
        HttpRequest.put('http://localhost:5000/context/a/b/c').body() == 'put c'
        HttpRequest.delete('http://localhost:5000/context/a/b/c').body() == 'delete c'
        HttpRequest.get('http://localhost:5000/context/a/b/json').body() == '{"1":1}'
        HttpRequest.get('http://localhost:5000/context/a/b/d').body() == 'options d'
        HttpRequest.get('http://localhost:5000/context/a/test/halt').body() == HttpStatus.Code.INTERNAL_SERVER_ERROR.message
        HttpRequest.get('http://localhost:5000/context/a/test/exception').body() == 'xxx'
        HttpRequest.get('http://localhost:5000/context/a/regex/book1').body() == 'get book'
        cleanup:
        server.stop()
    }
}
