package org.segment.web.handler

import groovy.transform.CompileStatic

import javax.servlet.http.HttpServletRequest

@CompileStatic
class Req {
    private HttpServletRequest request

    Req(HttpServletRequest request) {
        this.request = request
    }
}
