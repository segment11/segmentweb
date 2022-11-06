package org.segment.web.handler

import groovy.transform.CompileStatic
import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.http.HttpStatus

import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletResponse
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

@CompileStatic
class Resp {
    private Req req
    private HttpServletResponse response

    Resp(Req req, HttpServletResponse response) {
        this.req = req
        this.response = response
    }

    HttpServletResponse raw() {
        response
    }

    private boolean isEnd = false

    boolean isEnd() {
        isEnd
    }

    static final String encoding = StandardCharsets.UTF_8.name()

    static long copy(InputStream input, OutputStream output) {
        def buffer = new byte[4 * 1024]
        long count = 0;
        int n
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n)
            count += n
        }
        count
    }

    void output(InputStream is) {
        assert !isEnd

        def os = response.outputStream
        def osTarget = isGzip ? new GZIPOutputStream(os) : os
        long len = copy(is, osTarget)
        osTarget.close()
        is.close()
        response.contentLength = len.intValue()
        isEnd = true
    }

    void end(String str = null) {
        String s = str == null ? '' : str
        if (response.contentType == null) {
            response.contentType = 'text/html;charset=' + encoding
        }
        output(new ByteArrayInputStream(s.getBytes(encoding)))
    }

    void end(byte[] bytes) {
        output(new ByteArrayInputStream(bytes))
    }

    void json(Object obj) {
        response.contentType = 'application/json;charset=' + encoding
        end(JsonWriter.instance.json(obj))
    }

    void halt(int status = HttpStatus.INTERNAL_SERVER_ERROR_500, String message =
            HttpStatus.Code.INTERNAL_SERVER_ERROR.message, Throwable t = null) {
        throw t == null ? new HaltEx(status, message) : new HaltEx(status, message, t)
    }

    private boolean isGzip = false

    Resp gzip() {
        this.isGzip = true
        this
    }

    private int status = HttpStatus.OK_200

    int status() {
        status
    }

    Resp status(int s) {
        this.status = s
        response.status = s
        this
    }

    private String contentType

    String contentType() {
        contentType
    }

    Resp contentType(String s) {
        this.contentType = s
        header(HttpHeader.CONTENT_TYPE.toString(), s)
    }

    Resp header(String name, String value) {
        response.setHeader(name, value)
        this
    }

    Resp cookie(String name, String value, int maxAge = 3600, String path = '/', String domain = null,
                boolean secure = false, boolean httpOnly = false) {
        Cookie c = new Cookie(name, value)
        c.maxAge = maxAge
        c.path = path
        c.domain = domain ?: req.host()
        c.secure = secure
        c.httpOnly = httpOnly
        response.addCookie(c)
        this
    }

    Resp removeCookie(String name, String path = '/') {
        cookie(name, '', 0, path)
    }

    void redirect(String location, int s = HttpStatus.FOUND_302) {
        status(s)
        response.sendRedirect(location)
        isEnd = true
    }
}
