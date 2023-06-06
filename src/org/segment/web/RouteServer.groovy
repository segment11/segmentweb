package org.segment.web

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.segment.web.handler.ChainHandler
import org.segment.web.json.DefaultJsonTransformer
import org.segment.web.json.JsonTransformer

import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@CompileStatic
@Slf4j
@Singleton
class RouteServer {

    private Server server

    RouteRefreshLoader loader

    JettyServerCreator serverCreator

    JsonTransformer jsonTransformer = new DefaultJsonTransformer()

    String webRoot

    int maxThreads = 200

    int minThreads = 8

    int idleTimeout = 60 * 1000

    SslContextFactory sslContextFactory

    void start(int port = 5000, String host = '0.0.0.0', int httpsPort = 5001) {
        if (loader) {
            loader.start()
        }

        server = serverCreator ? serverCreator.create() :
                new Server(new QueuedThreadPool(maxThreads, minThreads, idleTimeout))

        def handler = new ServletContextHandler(ServletContextHandler.SESSIONS)
        handler.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
                ChainHandler.instance.handle(request, response)
            }
        }), "/*")

        def connector = new ServerConnector(server)
        connector.host = host
        connector.port = port
        server.addConnector(connector)

        if (sslContextFactory) {
            def httpsConnector = new ServerConnector(server, sslContextFactory)
            httpsConnector.host = host
            httpsConnector.port = httpsPort
            server.addConnector(httpsConnector)
        }

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
            log.info('jetty server started - {}', port)
            if (sslContextFactory) {
                log.info('jetty https server started - {}', httpsPort)
            }
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
