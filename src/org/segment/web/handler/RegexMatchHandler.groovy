package org.segment.web.handler

import groovy.transform.CompileStatic
import org.eclipse.jetty.http.HttpMethod

import java.util.regex.Pattern

@CompileStatic
abstract class RegexMatchHandler extends AbstractHandler {
    Pattern pattern

    String uriPre

    @Override
    protected boolean isRequestMatch(String method, String uriInput) {
        if (this.method != HttpMethod.OPTIONS && this.method.name() != method) {
            return false
        }
        if (uriPre != null && !uriInput.startsWith(uriPre)) {
            return false
        }
        String uriToMatch = uriPre != null ? uriInput[uriPre.length()..-1] : uriInput
        uriToMatch ==~ pattern
    }

    @Override
    String name() {
        method.name() + ':' + (uriPre ?: '') + pattern.toString()
    }
}
