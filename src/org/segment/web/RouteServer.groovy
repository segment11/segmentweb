package org.segment.web

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.util.thread.QueuedThreadPool
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

    String webRoot

    int maxThreads = 200

    int minThreads = 8

    int idleTimeout = 60 * 1000

    void start(int port = 5000, String host = '0.0.0.0') {
        if (loader) {
            loader.start()
        }

        server = serverCreator ? serverCreator.create() :
                new Server(new QueuedThreadPool(maxThreads, minThreads, idleTimeout))
        def handler = new ServletContextHandler(ServletContextHandler.SESSIONS) {
            @Override
            void doHandle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                    throws IOException, ServletException {
                ChainHandler.instance.handle(request, response)
            }
        }

        def connector = new ServerConnector(server)
        connector.host = host
        connector.port = port
        server.addConnector(connector)

        if (webRoot) {
            def h = new ResourceHandler()
            h.directoriesListed = true
            String[] welcomeFiles = ['index.html', 'index.htm']
            h.welcomeFiles = welcomeFiles
            h.resourceBase = webRoot

            def list = new HandlerList()
            Handler[] handlers = [h, handler]
            list.handlers = handlers
            server.handler = list
        } else {
            server.handler = handler
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
