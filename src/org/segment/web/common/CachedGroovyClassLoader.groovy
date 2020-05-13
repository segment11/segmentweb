package org.segment.web.common

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.codec.digest.DigestUtils
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilerConfiguration

@CompileStatic
@Slf4j
@Singleton
class CachedGroovyClassLoader {
    private volatile GroovyClassLoader gcl

    GroovyClassLoader getGcl() {
        return gcl
    }

    final static String GROOVY_FILE_EXT = '.groovy'
    final static String GROOVY_FILE_ENCODING = 'utf-8'

    void init(ClassLoader parentClassLoader = null, String classpath = null) {
        if (gcl != null) {
            return
        }

        def config = new CompilerConfiguration()
        config.sourceEncoding = GROOVY_FILE_ENCODING
        gcl = new Loader(parentClassLoader ?: CachedGroovyClassLoader.class.classLoader, config)
        if (classpath) {
            for (path in classpath.split(':')) {
                gcl.addClasspath(path)
            }
        }
    }

    Object eval(String scriptText, Map<String, Object> variables = null) {
        Script script = gcl.parseClass(scriptText).newInstance()
        if (variables != null) {
            def b = new Binding()
            variables.each { k, v ->
                b.setProperty(k, v)
            }
            script.binding = b
        }
        script.run()
    }

    @CompileStatic
    @Slf4j
    private static class Loader extends GroovyClassLoader {
        Loader(ClassLoader loader, CompilerConfiguration config) {
            super(loader, config)
        }

        private Map<String, Long> lastModified = [:]
        private Map<String, Class> classLoaded = [:]

        private boolean isModified(File f) {
            def l = lastModified[f.absolutePath]
            l != null && l.longValue() != f.lastModified()
        }

        private void logClassLoaded(Map<String, Class> x) {
            for (entry in x.entrySet()) {
                log.debug(entry.key + ':' + entry.value)
            }
        }

        private static boolean isFileMatchTargetClass(String filePath, String className) {
            filePath.replace(GROOVY_FILE_EXT, '').replaceAll(/\//, '.').endsWith(className)
        }

        private Class getFromCache(GroovyCodeSource source) {
            Class r = null
            synchronized (classCache) {
                r = classCache.find {
                    isFileMatchTargetClass(source.name, it.key)
                }?.value
                if (r != null) {
                    lastModified[source.file.absolutePath] = source.file.lastModified()
                }
            }
            r
        }

        @Override
        Class parseClass(GroovyCodeSource codeSource, boolean shouldCacheSource) throws CompilationFailedException {
            if (log.isDebugEnabled()) {
                logClassLoaded(classCache)
            }

            Class r = null
            def file = codeSource.file
            def scriptText = codeSource.scriptText
            def name = codeSource.name

            if (file != null) {
                if (!isModified(file)) {
                    synchronized (classLoaded) {
                        r = classLoaded[name]
                    }
                    if (r == null) {
                        r = getFromCache(codeSource)
                    }
                }

                if (r == null) {
                    r = super.parseClass(codeSource, false)
                    classLoaded[name] = r
                    lastModified[file.absolutePath] = file.lastModified()
                    log.debug 'recompile - ' + name
                } else {
                    log.debug 'get from cached - ' + name
                }
            } else if (scriptText != null) {
                def key = DigestUtils.md5Hex(scriptText)
                synchronized (classLoaded) {
                    r = classLoaded[key]
                }
                if (r == null) {
                    r = super.parseClass(codeSource, false)
                    classLoaded[key] = r
                    log.debug 'recompile - ' + scriptText
                } else {
                    log.debug 'get from cached - ' + scriptText
                }
            }
            r
        }
    }
}
