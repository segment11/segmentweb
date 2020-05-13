package org.segment.web.handler

import groovy.transform.CompileStatic

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@CompileStatic
interface Handler {
    boolean handle(HttpServletRequest request, HttpServletResponse response)

    String name()
}