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
        def refreshLoader = RouteRefreshLoader.create(loader.gcl).compileStatic(false).jarLoad(false).
                addDir(rootPath + '/ext/script').addClasspath(rootPath + '/ext').addVariable('name', 'kerry').
                refreshFileCallback { File file ->
                    println 'eval groovy file callback, file: ' + file.name
                }
        refreshLoader.intervalSeconds(1).start()
        Thread.sleep(500)
        expect:
        User.instance.name == 'kerry'
        cleanup:
        refreshLoader.stop()
    }
}
