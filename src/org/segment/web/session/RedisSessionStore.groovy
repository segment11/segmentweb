package org.segment.web.session

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.eclipse.jetty.server.session.AbstractSessionDataStore
import org.eclipse.jetty.server.session.SessionData
import org.eclipse.jetty.util.ClassLoadingObjectInputStream
import redis.clients.jedis.JedisPool

import java.util.concurrent.atomic.AtomicReference

@CompileStatic
@Slf4j
class RedisSessionStore extends AbstractSessionDataStore {

    private JedisPool jedisPool

    RedisSessionStore(JedisPool jedisPool) {
        this.jedisPool = jedisPool
    }

    @Override
    void doStore(String id, SessionData data, long lastSaveTime) throws Exception {
        def bos = new ByteArrayOutputStream()
        def oos = new ObjectOutputStream(bos)
        def jedis = jedisPool.resource
        try {
            oos.writeObject(data)
            jedis.set(getCacheKey(id).bytes, bos.toByteArray())
            log.debug("Session {} saved to Redis, expires {} ", id, data.expiry)
        } finally {
            oos.close()
            bos.close()
            jedis.close()
        }
    }

    @Override
    SessionData doLoad(String id) throws Exception {
        def jedis = jedisPool.resource
        try {
            log.debug("Loading bytes {} from Redis", id)
            final byte[] bytes = jedis.get(getCacheKey(id).bytes)
            if (bytes == null) {
                return
            }

            def is = new ByteArrayInputStream(bytes)
            def ois = new ClassLoadingObjectInputStream(is)
            try {
                SessionData sd = (SessionData) ois.readObject()
                return sd
            } finally {
                ois.close()
                is.close()
            }
        } finally {
            jedis.close()
        }
    }

    @Override
    Set<String> doGetExpired(Set<String> candidates) {
        if (candidates == null || candidates.isEmpty())
            return candidates

        long now = System.currentTimeMillis()

        Set<String> expired = []

        for (String candidate : candidates) {
            log.debug("Checking expiry for candidate {}", candidate)
            try {
                SessionData sd = load(candidate)

                //if the session no longer exists
                if (sd == null) {
                    expired.add(candidate)
                    log.debug("Session {} does not exist in Redis", candidate)
                } else {
                    if (_context.workerName == sd.getLastNode()) {
                        //we are its manager, add it to the expired set if it is expired now
                        if ((sd.expiry > 0) && sd.expiry <= now) {
                            expired.add(candidate)
                            log.debug("Session {} managed by {} is expired", candidate, _context.workerName)
                        }
                    } else {
                        //if we are not the session's manager, only expire it iff:
                        // this is our first expiryCheck and the session expired a long time ago
                        //or
                        //the session expired at least one graceperiod ago
                        if (_lastExpiryCheckTime <= 0) {
                            if ((sd.expiry > 0) && sd.expiry < (now - (1000L * (3 * _gracePeriodSec))))
                                expired.add(candidate)
                        } else {
                            if ((sd.expiry > 0) && sd.expiry < (now - (1000L * _gracePeriodSec)))
                                expired.add(candidate)
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Error checking if candidate {} is expired", candidate, e)
            }
        }

        return expired
    }

    @Override
    boolean isPassivating() {
        return true
    }

    private String getCacheKey(String id) {
        return _context.getCanonicalContextPath() + "_" + _context.getVhost() + "_" + id
    }

    @Override
    boolean exists(String id) throws Exception {
        final AtomicReference<Boolean> reference = new AtomicReference<>()
        final AtomicReference<Exception> exception = new AtomicReference<>()

        Runnable load = {
            def jedis = jedisPool.resource
            try {
                final boolean exists = jedis.exists(getCacheKey(id))
                if (!exists) {
                    reference.set(Boolean.FALSE)
                    return
                }

                SessionData sd = load(id)
                if (sd.expiry <= 0)
                    reference.set(Boolean.TRUE)
                else
                    reference.set(sd.expiry > System.currentTimeMillis())
            } catch (Exception e) {
                exception.set(e)
            } finally {
                jedis.close()
            }
        }

        //ensure the load runs in the context classloader scope
        _context.run(load)

        if (exception.get() != null)
            throw exception.get()

        return reference.get()
    }

    @Override
    boolean delete(String id) throws Exception {
        log.debug("Deleting session with id {} from Redis", id)
        def jedis = jedisPool.resource
        try {
            return jedis.del(getCacheKey(id)) != null
        } finally {
            jedis.close()
        }
    }
}
