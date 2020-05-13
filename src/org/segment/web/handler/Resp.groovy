package org.segment.web.handler

import groovy.transform.CompileStatic
import org.apache.commons.io.IOUtils
import org.eclipse.jetty.http.HttpStatus

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

    private boolean isEnd = false

    boolean isEnd() {
        isEnd
    }

    boolean isGzip

    static final String encoding = StandardCharsets.UTF_8.name()

    void output(InputStream is) {
        assert !isEnd

        def os = response.outputStream
        def osTarget = isGzip ? new GZIPOutputStream(os) : os
        int len = IOUtils.copy(is, osTarget)
        osTarget.close()
        is.close()
        response.contentLength = len
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
}
