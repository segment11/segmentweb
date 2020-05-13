package org.segment.web

import groovy.transform.CompileStatic
import org.eclipse.jetty.server.Server

@CompileStatic
interface JettyServerCreator {
    Server create()
}