package org.segment.web.handler

import groovy.transform.CompileStatic
import org.eclipse.jetty.http.HttpHeader

import javax.servlet.MultipartConfigElement
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.Part
import java.nio.charset.StandardCharsets

@CompileStatic
class Req {
    private HttpServletRequest request

    Req(HttpServletRequest request) {
        this.request = request
    }

    static String tmpDir = '~/tmp'

    HttpServletRequest raw() {
        request
    }

    String host() {
        request.getHeader(HttpHeader.HOST.toString())
    }

    String userAgent() {
        request.getHeader(HttpHeader.USER_AGENT.toString())
    }

    String contentType() {
        request.getHeader(HttpHeader.CONTENT_TYPE.toString())
    }

    String header(String name) {
        request.getHeader(name)
    }

    Object session(String name, Object value = null) {
        if (value == null) {
            return request.session.getAttribute(name)
        }
        request.session.setAttribute(name, value)
        value
    }

    void removeSession(String name) {
        request.session.removeAttribute(name)
    }

    Object attr(String name, Object value = null) {
        if (value == null) {
            return request.getAttribute(name)
        }
        request.setAttribute(name, value)
        value
    }

    int contentLength() {
        String x = request.getHeader(HttpHeader.CONTENT_LENGTH.toString())
        if (x) {
            return x as int
        }
        def b = bodyAsBytes()
        b ? b.size() : 0
    }

    private byte[] bytes

    byte[] bodyAsBytes() {
        if (bytes == null) {
            def os = new ByteArrayOutputStream()
            Resp.copy(request.inputStream, os)
            bytes = os.toByteArray()
        }
        bytes
    }

    static final String encoding = StandardCharsets.UTF_8.name()

    private String body

    String body() {
        if (body == null) {
            body = new String(bodyAsBytes(), encoding)
        }
        body
    }

    public <T> T bodyAs(Class<T> clz = HashMap) {
        JsonReader.instance.read(bodyAsBytes(), clz)
    }

    String param(String name) {
        if (name.startsWith(':')) {
            return request.getAttribute(name)
        }
        request.getParameter(name)
    }

    Part part(String name) {
        attr('org.eclipse.jetty.multipartConfig', new MultipartConfigElement(tmpDir))
        request.getPart(name)
    }

    String form(String name) {
        def part = part(name)
        if (part == null) {
            return null
        }

        def os = new ByteArrayOutputStream()
        Resp.copy(part.inputStream, os)
        new String(os.toByteArray(), encoding)
    }

    String ip() {
        request.remoteAddr
    }

    String method() {
        request.method
    }

    static final String PROXY_URI_ATTR = 'X-uri'

    String uri() {
        def val = request.getAttribute(PROXY_URI_ATTR)
        if (val != null) {
            return val.toString()
        }

        request.requestURI
    }

    String url() {
        String x = request.requestURI
        String query = request.queryString
        query ? x + '?' + query : x
    }

    String protocol() {
        request.protocol
    }
}
