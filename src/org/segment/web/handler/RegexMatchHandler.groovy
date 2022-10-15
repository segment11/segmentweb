package org.segment.web.handler

import groovy.transform.CompileStatic
import org.eclipse.jetty.http.HttpMethod

import javax.servlet.http.HttpServletRequest
import java.util.regex.Pattern

@CompileStatic
abstract class RegexMatchHandler extends AbstractHandler {
    Pattern pattern

    String context

    @Override
    protected boolean isRequestMatch(String method, String uriInput, HttpServletRequest request) {
        if (this.method != HttpMethod.OPTIONS && this.method.name() != method) {
            return false
        }
        if (context != null && !uriInput.startsWith(context)) {
            return false
        }
        String uriToMatch = context != null ? uriInput[context.length()..-1] : uriInput
        uriToMatch ==~ pattern
    }

    @Override
    String name() {
        'regex:' + method.name() + ':' + (context ?: '') + pattern.toString()
    }
}
