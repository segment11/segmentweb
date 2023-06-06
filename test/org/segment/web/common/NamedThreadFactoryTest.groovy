package org.segment.web.common

import spock.lang.Specification

class NamedThreadFactoryTest extends Specification {
    def "new thread"() {
        given:
        def tf = new NamedThreadFactory('test')
        def t = tf.newThread {
            println 'running'
        }
        expect:
        t.name.contains('test-') && t.name.contains('-thread-')
    }
}
