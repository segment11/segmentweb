package org.segment.web.handler

import groovy.transform.CompileStatic
import org.eclipse.jetty.http.HttpStatus

@CompileStatic
class HaltEx extends RuntimeException {
    int status = HttpStatus.NOT_FOUND_404

    HaltEx(int status, String message) {
        super(message)
        this.status = status
    }

    HaltEx(int status, String message, Throwable cause) {
        super(message, cause)
        this.status = status
    }
}
