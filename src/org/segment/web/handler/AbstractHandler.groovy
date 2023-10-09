package org.segment.web.handler

import groovy.transform.CompileStatic
import org.eclipse.jetty.http.HttpMethod

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@CompileStatic
abstract class AbstractHandler implements Handler, Comparable<AbstractHandler> {
    String uri

    HttpMethod method = HttpMethod.GET

    int seq = 0

    @Override
    int compareTo(AbstractHandler o) {
        if (seq != o.seq) {
            return seq <=> o.seq
        }
        // uri shorter first
        uri.length() <=> o.uri.length()
    }

    @Override
    String name() {
        method.name() + ':' + uri
    }

    @Override
    String toString() {
        name()
    }

    @Override
    boolean handle(HttpServletRequest request, HttpServletResponse response) {
        String uriInput = request.requestURI
        if (!isRequestMatch(request.method, uriInput, request)) {
            return false
        }

        def req = new Req(request)
        def resp = new Resp(req, response)

        def obj = hi(req, resp)
        if (obj != null && !resp.isEnd()) {
            resp.json(obj)
        }
        true
    }

    protected boolean isRequestMatch(String method, String uriInput, HttpServletRequest request) {
        (this.method == HttpMethod.OPTIONS || this.method.name() == method) && isUriMatch(uri, uriInput, request)
    }

    abstract Object hi(Req req, Resp resp)

    static boolean isUriMatch(String uri, String uriInput, HttpServletRequest request = null) {
        if (uri == uriInput) {
            return true
        }

        def arr = uri.split(/\//)
        def arr2 = uriInput.split(/\//)

        if (arr.length != arr2.length) {
            if (arr[-1] != '**') {
                return false
            }
            for (int i = 0; i < arr.length; i++) {
                if (arr2.length <= i || (arr[i] != '*' && arr[i] != '**' && arr[i] != arr2[i])) {
                    return false
                }
            }
            return true
        } else {
            for (int i = 0; i < arr.length; i++) {
                def s = arr[i]
                if (s == '*' || s == '**') {
                    continue
                }
                // save uri path values in request attribute
                if (s.startsWith(':')) {
                    String value = arr2[i]
                    if (request != null) {
                        request.setAttribute(s, value)
                    }
                    continue
                }
                if (s != arr2[i]) {
                    return false
                }
            }
            return true
        }
    }
}
