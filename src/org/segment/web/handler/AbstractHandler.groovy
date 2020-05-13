package org.segment.web.handler

import groovy.transform.CompileStatic
import org.eclipse.jetty.http.HttpMethod

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@CompileStatic
abstract class AbstractHandler implements Handler {
    String uri

    HttpMethod method = HttpMethod.GET

    @Override
    String name() {
        method.name() + ':' + uri
    }

    @Override
    boolean handle(HttpServletRequest request, HttpServletResponse response) {
        String uriInput = request.requestURI
        if (!isRequestMatch(request.method, uriInput)) {
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

    protected boolean isRequestMatch(String method, String uriInput) {
        (this.method == HttpMethod.OPTIONS || this.method.name() == method) && isUriMatch(uri, uriInput)
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
            int c = 0

            for (int i = 0; i < arr.length; i++) {
                def s = arr[i]
                if (s == '*' || s == '**') {
                    continue
                }
                if (s.startsWith(':')) {
                    String key = s[1..-1]
                    String value = arr2[i]
                    if (request != null) {
                        request.setAttribute(key, value)
                        request.setAttribute('p' + c, value)
                    }
                    c++
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
