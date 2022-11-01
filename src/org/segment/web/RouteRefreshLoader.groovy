package org.segment.web

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.control.CompilerConfiguration
import org.segment.web.common.CachedGroovyClassLoader
import org.segment.web.common.NamedThreadFactory

import java.util.concurrent.ScheduledThreadPoolExecutor

@CompileStatic
@Slf4j
class RouteRefreshLoader {
    private RouteRefreshLoader() {}

    static RouteRefreshLoader create(GroovyClassLoader gcl) {
        def r = new RouteRefreshLoader()
        r.gcl = gcl
        r
    }

    private ScheduledThreadPoolExecutor sh = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory('Route-Refresh'))

    private List<String> dirList = []

    private List<String> classpathList = []

    private Map<String, Object> variables = [:]

    private GroovyClassLoader gcl

    private boolean isJarLoad = false

    RouteRefreshLoader jarLoad(boolean flag) {
        this.isJarLoad = flag
        this
    }

    RouteRefreshLoader addVariable(String key, Object value) {
        variables[key] = value
        this
    }

    RouteRefreshLoader addDir(String dir) {
        dirList << dir
        this
    }

    RouteRefreshLoader addClasspath(String classpath) {
        classpathList << classpath
        this
    }

    private GroovyShell getShell() {
        def config = new CompilerConfiguration()
        config.sourceEncoding = CachedGroovyClassLoader.GROOVY_FILE_ENCODING
        config.classpath = classpathList
        if (classpathList) {
            for (classpath in classpathList) {
                gcl.addClasspath(classpath)
            }
        }

        def b = new Binding()
        variables.each { k, v ->
            b.setProperty(k, v)
        }
        new GroovyShell(gcl, b, config)
    }

    private void loadAndRun(String packageNameDir) {
        def dirs = this.getClass().classLoader.getResources(packageNameDir)
        for (url in dirs) {
            if ('jar' == url.protocol) {
                def jarFile = ((JarURLConnection) url.openConnection()).jarFile
                for (entry in jarFile.entries()) {
                    if (entry.isDirectory()) {
                        continue
                    } else {
                        String name = entry.name
                        runGroovyScriptInJar(name, packageNameDir)
                    }
                }
            }
        }
    }

    private void runGroovyScriptInJar(String name, String packageNameDir) {
        if (name[0] == '/') {
            name = name[1..-1]
        }

        if (name.startsWith(packageNameDir) && name.endsWith('.class') && !name.contains('$') && !name.contains('_')) {
            String className = name[0..-7].replaceAll(/\//, '.')
            def one = Class.forName(className).newInstance()
            if (one instanceof Script) {
                Script gs = one as Script
                def b = new Binding()
                variables.each { k, v ->
                    b.setProperty(k, v)
                }
                gs.setBinding(b)
                gs.run()
                log.info('run script {}', name)
            }
        }
    }

    void refresh() {
        def shell = getShell()
        for (dir in dirList) {
            if (isJarLoad) {
                def index = dir.indexOf('/src/')
                def packageNameDir = dir[index + 5..-1]
                loadAndRun(packageNameDir)
                continue
            }

            def d = new File(dir)
            if (!d.exists() || !d.isDirectory()) {
                continue
            }

            d.eachFileRecurse { File f ->
                if (f.isDirectory()) {
                    return
                }

                if (!f.name.endsWith(CachedGroovyClassLoader.GROOVY_FILE_EXT)) {
                    return
                }

                refreshFile(f, shell)
            }
        }
    }

    private Map<String, Long> lastModified = [:]

    void refreshFile(File file, GroovyShell shell = null) {
        if (shell == null) {
            shell = getShell()
        }

        def l = lastModified[file.absolutePath]
        if (l != null && l.longValue() == file.lastModified()) {
            return
        }

        def name = file.name
        log.info 'begin refresh ' + name
        try {
            shell.evaluate(file)
            lastModified[file.absolutePath] = file.lastModified()
            log.info 'done refresh ' + name
            if (refreshFileCallback != null) {
                refreshFileCallback.call(file)
            }
        } catch (Exception e) {
            log.error('fail eval - ' + name, e)
        }
    }

    private Closure<Void> refreshFileCallback

    RouteRefreshLoader refreshFileCallback(Closure<Void> refreshFileCallback) {
        this.refreshFileCallback = refreshFileCallback
        this
    }

    void stop() {
        if (sh) {
            sh.shutdown()
            log.info 'stop route refresh loader interval'
            sh = null
        }
    }

    void start() {
        if (isJarLoad) {
            refresh()
            return
        }

        sh = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory('Route-Refresh'))
        sh.scheduleWithFixedDelay({
            try {
                refresh()
            } catch (Exception e) {
                log.error('fail route refresh', e)
            }
        }, 0, 1000 * 10, java.util.concurrent.TimeUnit.MILLISECONDS)
        log.info 'start route refresh loader interval'
    }
}
