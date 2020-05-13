package org.segment.web.handler

import groovy.transform.CompileStatic

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@CompileStatic
interface ExceptionHandler {
    void handle(HttpServletRequest request, HttpServletResponse response, Throwable t)
}