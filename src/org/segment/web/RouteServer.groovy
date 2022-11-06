package org.segment.web

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.prometheus.client.exporter.MetricsServlet
import io.prometheus.client.filter.MetricsFilter
import io.prometheus.client.hotspot.DefaultExports
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.segment.web.handler.ChainHandler

import javax.servlet.DispatcherType
import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@CompileStatic
@Slf4j
@Singleton
class RouteServer {

    private Server server

    private Server metricServer

    RouteRefreshLoader loader

    boolean isStartMetricServer = false

    double[] buckets = "0.5,1,7.5,10".split(',').collect { it as double } as double[]

    Integer pathComponents = 3

    JettyServerCreator serverCreator

    String webRoot

    int maxThreads = 200

    int minThreads = 8

    int idleTimeout = 60 * 1000

    void start(int port = 5000, String host = '0.0.0.0', int metricPort = 7000) {
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
        if (isStartMetricServer) {
            // path pattern match :id may too many
            def holder = new FilterHolder(new MetricsFilter('request_filter',
                    'request filter', pathComponents, buckets))
            handler.addFilter(holder, '/*', EnumSet.of(DispatcherType.REQUEST))
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

        if (isStartMetricServer) {
            metricServer = new Server(metricPort)

            def context = new ServletContextHandler()
            context.contextPath = '/'
            context.addServlet(new ServletHolder(new MetricsServlet()), '/metrics')
            metricServer.handler = context

            DefaultExports.initialize()
        }

        Thread.start {
            server.start()
            log.info('jetty server started - {}', port)
            server.join()
        }

        if (metricServer) {
            Thread.start {
                metricServer.start()
                log.info('metric server started - {}', metricPort)
                metricServer.join()
            }
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
        if (metricServer) {
            metricServer.stop()
            log.info('metric server stopped')
        }
    }
}
