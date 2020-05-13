package org.segment.web

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import org.segment.web.handler.ChainHandler

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@CompileStatic
@Slf4j
@Singleton
class RouteServer {

    private Server server

    RouteRefreshLoader loader

    JettyServerCreator serverCreator

    void start(int port = 5000) {
        if (loader) {
            loader.start()
        }

        server = serverCreator ? serverCreator.create() : new Server(port)
        server.handler = new AbstractHandler() {
            @Override
            void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {
                ChainHandler.instance.handle(request, response)
            }
        }

        Thread.start {
            server.start()
            log.info('jetty server started - ' + port)
            server.join()
        }
    }

    void stop() {
        if (loader) {
            loader.stop()
        }
        if (server) {
            server.stop()
            log.info('jetty server stopped')
        }
    }
}
