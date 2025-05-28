package org.segment.web.handler

import groovy.util.logging.Slf4j
import org.eclipse.jetty.http.HttpMethod
import org.eclipse.jetty.http.HttpStatus

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.regex.Pattern

@Singleton
@Slf4j
class ChainHandler implements Handler {
    @Override
    boolean handle(HttpServletRequest request, HttpServletResponse response) {
        boolean r = false
        try {
            handleList(request, response, beforeList, false)
            r = handleList(request, response, list, true)
            handleList(request, response, afterList, false)
            if (!r) {
                response.status = HttpStatus.NOT_FOUND_404
                def os = response.outputStream
                os.write(HttpStatus.Code.NOT_FOUND.message.getBytes(Resp.encoding))
                os.close()
            }
        } catch (HaltEx haltEx) {
            response.status = haltEx.status
            def os = response.outputStream
            os.write(haltEx.message.getBytes(Resp.encoding))
            os.close()
        } catch (Throwable t) {
            if (exceptionHandler) {
                try {
                    exceptionHandler.handle(request, response, t)
                } catch (Exception e2) {
                    log.error('exception handle error', e2)
                }
            } else {
                throw t
            }
        } finally {
            try {
                handleList(request, response, afterAfterList, false)
            } catch (Exception e) {
                log.error('after after handle error', e)
            }
        }

        return r
    }

    private static boolean handleList(HttpServletRequest request, HttpServletResponse response, List<AbstractHandler> ll,
                                      boolean isReturnOnceMatched) {
        if (!ll) {
            return false
        }

        for (handler in ll) {
            def r = handler.handle(request, response)
            if (r && isReturnOnceMatched) {
                return r
            }
        }
        false
    }

    @Override
    String name() {
        'chain'
    }

    ArrayList<AbstractHandler> list = new ArrayList<>()
    ArrayList<AbstractHandler> beforeList = new ArrayList<>()
    ArrayList<AbstractHandler> afterList = new ArrayList<>()
    ArrayList<AbstractHandler> afterAfterList = new ArrayList<>()

    void print(Closure closure) {
        closure.call('list: ' + list.collect { it.name() })
        closure.call('beforeList: ' + beforeList.collect { it.name() })
        closure.call('afterList: ' + afterList.collect { it.name() })
        closure.call('afterAfterList: ' + afterAfterList.collect { it.name() })
    }

    private ExceptionHandler exceptionHandler

    ChainHandler exceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler
        this
    }

    private void removeOneThatExists(Handler handler, ArrayList<AbstractHandler> ll = null) {
        def r = ll == null ? list : ll
        r.removeIf { it.name() == handler.name() }
    }

    AbstractHandler findByName(String name) {
        list.find { it.name() == name }
    }

    private String context

    synchronized ChainHandler context(String context) {
        this.context = context
        this
    }

    private String addContextPath(String uri) {
        if (context == null) {
            return uri
        }
        context + uri
    }

    synchronized void group(String groupPathPrefix, Closure closure) {
        String oldContext = context
        context = oldContext == null ? groupPathPrefix : oldContext + groupPathPrefix
        closure.call()
        context = oldContext
    }

    private synchronized ChainHandler add(String uri, HttpMethod method, AbstractHandler handler,
                                          ArrayList<AbstractHandler> ll) {
        handler.uri = addContextPath(uri)
        handler.method = method
        removeOneThatExists(handler, ll)
        ll << handler
        ll.sort()
        this
    }

    private synchronized ChainHandler addRegex(Pattern pattern, HttpMethod method, RegexMatchHandler handler,
                                               ArrayList<AbstractHandler> ll) {
        handler.context = context
        handler.pattern = pattern
        handler.method = method
        handler.uri = addContextPath(pattern.toString())
        removeOneThatExists(handler, ll)
        ll << handler
        ll.sort()
        this
    }

    ChainHandler get(String uri, AbstractHandler handler) {
        add(uri, HttpMethod.GET, handler, list)
    }

    ChainHandler post(String uri, AbstractHandler handler) {
        add(uri, HttpMethod.POST, handler, list)
    }

    ChainHandler put(String uri, AbstractHandler handler) {
        add(uri, HttpMethod.PUT, handler, list)
    }

    ChainHandler delete(String uri, AbstractHandler handler) {
        add(uri, HttpMethod.DELETE, handler, list)
    }

    ChainHandler options(String uri, AbstractHandler handler) {
        add(uri, HttpMethod.OPTIONS, handler, list)
    }

    ChainHandler get(Pattern pattern, RegexMatchHandler handler) {
        addRegex(pattern, HttpMethod.GET, handler, list)
    }

    ChainHandler post(Pattern pattern, RegexMatchHandler handler) {
        addRegex(pattern, HttpMethod.POST, handler, list)
    }

    ChainHandler put(Pattern pattern, RegexMatchHandler handler) {
        addRegex(pattern, HttpMethod.PUT, handler, list)
    }

    ChainHandler delete(Pattern pattern, RegexMatchHandler handler) {
        addRegex(pattern, HttpMethod.DELETE, handler, list)
    }

    ChainHandler options(Pattern pattern, RegexMatchHandler handler) {
        addRegex(pattern, HttpMethod.OPTIONS, handler, list)
    }

    ChainHandler before(String uri, AbstractHandler handler, int seq = 0) {
        handler.seq = seq
        add(uri, HttpMethod.OPTIONS, handler, beforeList)
    }

    ChainHandler before(Pattern pattern, RegexMatchHandler handler, int seq = 0) {
        handler.seq = seq
        addRegex(pattern, HttpMethod.OPTIONS, handler, beforeList)
    }

    ChainHandler after(String uri, AbstractHandler handler, int seq = 0) {
        handler.seq = seq
        add(uri, HttpMethod.OPTIONS, handler, afterList)
    }

    ChainHandler after(Pattern pattern, RegexMatchHandler handler, int seq = 0) {
        handler.seq = seq
        addRegex(pattern, HttpMethod.OPTIONS, handler, afterList)
    }

    ChainHandler afterAfter(String uri, AbstractHandler handler, int seq = 0) {
        handler.seq = seq
        add(uri, HttpMethod.OPTIONS, handler, afterAfterList)
    }

    ChainHandler afterAfter(Pattern pattern, RegexMatchHandler handler, int seq = 0) {
        handler.seq = seq
        addRegex(pattern, HttpMethod.OPTIONS, handler, afterAfterList)
    }
}
