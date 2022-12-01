package org.segment.web.session

import groovy.transform.CompileStatic
import org.eclipse.jetty.server.session.AbstractSessionDataStoreFactory
import org.eclipse.jetty.server.session.SessionDataStore
import org.eclipse.jetty.server.session.SessionHandler
import redis.clients.jedis.JedisPool

@CompileStatic
class RedisSessionDataStoreFactory extends AbstractSessionDataStoreFactory {
    private JedisPool jedisPool
    private int gracePeriodSec = 60 * 60

    RedisSessionDataStoreFactory(JedisPool jedisPool, int gracePeriodSec) {
        this.jedisPool = jedisPool
        this.gracePeriodSec = gracePeriodSec
    }

    @Override
    SessionDataStore getSessionDataStore(SessionHandler handler) throws Exception {
        def store = new RedisSessionStore(jedisPool)
        store.setGracePeriodSec(gracePeriodSec)
        store
    }
}
