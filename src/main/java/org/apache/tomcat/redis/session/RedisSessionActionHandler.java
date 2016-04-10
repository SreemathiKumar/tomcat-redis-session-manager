package org.apache.tomcat.redis.session;

import com.sun.deploy.util.StringUtils;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Session;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.redis.serializer.ISerializer;
import org.apache.tomcat.redis.store.RedisStoreManager;

import java.util.Arrays;

/**
 * Handler for all Redis session actions.
 */
public class RedisSessionActionHandler {
    private static final Log LOG = LogFactory.getLog(RedisSessionActionHandler.class);

    private static final int SESSION_ID = 0;
    private static final int SESSION_PRINCIPAL = 1;
    private static final int SESSION_ATTRIBUTES = 2;
    private static final int SESSION_NOTES = 3;

    private static final String COLON = ":";

    /**
     * Current Session Placeholder for web-request thread.
     */
    protected ThreadLocal<RedisSession> currentSession = new ThreadLocal<RedisSession>();

    /**
     * Serializer initialization
     */
    protected final ISerializer serializer;

    /**
     * Redis Store Manager
     */
    protected final RedisStoreManager storeManager;

    public RedisSessionActionHandler(final String serializationStrategyClass, RedisStoreManager storeManager) throws LifecycleException {
        this.serializer = getSerializer(serializationStrategyClass);
        this.storeManager = storeManager;
    }

    protected ISerializer getSerializer(final String serializationStrategyClass) throws LifecycleException {
        try {
            LOG.info("Instantiating serializer of type " + serializationStrategyClass);
            return (ISerializer) Class.forName(serializationStrategyClass).newInstance();
        } catch (ClassNotFoundException e) {
            throw new LifecycleException(e);
        } catch (InstantiationException e) {
            throw new LifecycleException(e);
        } catch (IllegalAccessException e) {
            throw new LifecycleException(e);
        }
    }

    private String getKey(String sessionId, int type) {
        return StringUtils.join(Arrays.asList("session", sessionId, type), COLON);
    }

    public void init() throws LifecycleException {
        this.storeManager.init();
    }

    public void destroy() {
        try {
            this.storeManager.destroy();
        } catch (Exception e) {
            // Do nothing to prevent anything untoward from happening
        }
    }

    /**
     * Registers the session Id in Redis Store.
     *
     * @param requestedSessionId
     * @return requestedSessionId if the session did not exist. Else null if the session with the id already exists.
     */
    public String regsisterSessionId(String requestedSessionId) {
        final String key = getKey(requestedSessionId, SESSION_ID);
        return this.storeManager.setnx(key, null) == 0L ? null : key;
    }

    /**
     * Registers the session in Redis Store.
     *
     * @param session
     * @return Session Object after registering in the Redis Store.
     */
    public Session registerSession(RedisSession session) {
        this.currentSession.set(session);
        return session;
    }

    public Session addSession(RedisSession session) {
        // TODO: SAVE the Session in Redis
        return registerSession(session);
    }

    public void remove(RedisSession session, boolean update) {
        // TODO: DELETE the Session from Redis
    }

    public Session loadSession(String sessionId) {
        // TODO:
        return null;
    }

    public void flush() {
        // TODO
    }
}
