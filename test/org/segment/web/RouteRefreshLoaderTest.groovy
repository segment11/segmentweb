package org.segment.web

import org.segment.web.common.CachedGroovyClassLoader
import spock.lang.Specification

class RouteRefreshLoaderTest extends Specification {

    def 'refresh'() {
        given:
        def rootPath = new File('.').absolutePath.replaceAll("\\\\", '/').
                replace(this.class.name.replaceAll('.', '/'), '')
        def loader = CachedGroovyClassLoader.instance
        loader.init()
        def refreshLoader = RouteRefreshLoader.create(loader.gcl).
                addDir(rootPath + '/ext/script').addClasspath(rootPath + '/ext').addVariable('name', 'kerry')
        refreshLoader.refresh()
        expect:
        User.instance.name == 'kerry'
    }
}
