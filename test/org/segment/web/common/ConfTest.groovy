package org.segment.web.common

import spock.lang.Specification

class ConfTest extends Specification {
    def 'parse class'() {
        given:
        def conf = Conf.instance
        conf.resetWorkDir()
        def projectPath = conf.projectPath('/')
        println projectPath
        String[] args = ['a=1']
        conf.loadArgs(args)
        conf.put('x', 1)
        conf.on('isDebug')
        conf.off('isDebug')
        conf.('isDebug')
        println conf.params

        expect:
        System.getProperty('os.name').toLowerCase().contains('windows') == Conf.isWindows()
        conf.projectPath('/src') == projectPath + 'src'
        conf.get('a') == '1'
        conf.getInt('a', 2) == 1
        conf.getInt('b', 2) == 2
        conf.get('x') == '1'
        conf.getString('x', '2') == '1'
        conf.getString('y', '2') == '2'
        !conf.isOn('isDebug')
    }
}
