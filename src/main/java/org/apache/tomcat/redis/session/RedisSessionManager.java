package org.apache.tomcat.redis.session;

import org.apache.catalina.*;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.io.IOException;

public class RedisSessionManager extends BaseRedisStoreManager implements Lifecycle {

    private static final Log LOG = LogFactory.getLog(RedisSessionManager.class);

    protected static final String info = "RedisSessionManager/1.0";
    protected static String name = "RedisSessionManager";

    protected String serializationStrategyClass = "org.apache.tomcat.redis.serializer.JavaSerializer";

    public void setSerializationStrategyClass(String strategy) {
        this.serializationStrategyClass = strategy;
    }

    /**
     * Session Handler Valve for callbacks
     */
    protected RedisSessionHandlerValve handlerValve;

    /**
     * Redis Action handler
     */
    protected RedisSessionActionHandler actionHandler;

    public RedisSessionActionHandler getActionHandler() {
        return actionHandler;
    }

    @Override
    public String getInfo() {
        return (info);
    }

    @Override
    public String getName() {
        return (name);
    }

    @Override
    public void load() throws ClassNotFoundException, IOException {
        // Do not load all sessions into memory
        // Even though we use Redis for persistence, the sessions are accesses lazily.
        // Hence there is not need to load all the sessions at once
    }

    @Override
    public void unload() throws IOException {
        // Do not persist all the sessions into Redis.
        // Even though we use Redis for persistence, the sessions are persisted lazily.
        // Hence there is not need to persist all the sessions at once
    }

    @Override
    public int getRejectedSessions() {
        // Do nothing.
        return 0;
    }

    protected synchronized void attachRedisSessionHandlerValve() throws LifecycleException {
        for (Valve valve : getContainer().getPipeline().getValves()) {
            if (valve instanceof RedisSessionHandlerValve) {
                this.handlerValve = (RedisSessionHandlerValve) valve;
                this.handlerValve.setRedisSessionManager(this);
                break;
            }
        }

        if (this.handlerValve == null) {
            final String msg = "No valve of type RedisSessionHandlerValve defined and attached. Kindly follow instructions.";
            LOG.fatal(msg);
            throw new LifecycleException(msg);
        }
    }

    protected synchronized void attachRedisActionHandler() throws LifecycleException {
        this.actionHandler = new RedisSessionActionHandler(serializationStrategyClass, getStoreManager());
        this.actionHandler.init();
    }

    @Override
    protected synchronized void startInternal() throws LifecycleException {
        super.startInternal();

        LOG.info("Starting " + name);
        LOG.info("Expiry set as " + getMaxInactiveInterval() + " seconds");

        setState(LifecycleState.STARTING);
        attachRedisSessionHandlerValve();
        attachRedisActionHandler();
    }

    @Override
    protected synchronized void stopInternal() throws LifecycleException {
        setState(LifecycleState.STOPPING);

        LOG.info("Stopping " + name);
        this.actionHandler.destroy();

        super.stopInternal();
    }

    protected String getCompletedSessionId(String sessionId) {
        final String jvmRoute = getJvmRoute();
        if (jvmRoute != null) {
            String jvmRouteSuffix = '.' + jvmRoute;
            return sessionId.endsWith(jvmRouteSuffix) ? sessionId : sessionId + jvmRouteSuffix;
        }
        return sessionId;
    }

    private String _registerSessionId(String requestedSessionId) {
        return (requestedSessionId != null) ? this.actionHandler.regsisterSessionId(getCompletedSessionId(requestedSessionId), false) : null;
    }

    @Override
    protected String generateSessionId() {
        String key;
        do {
            key = _registerSessionId(super.generateSessionId());
        } while (key == null);

        if(LOG.isDebugEnabled()) { LOG.debug("Generated session Id " + key); }

        return key;
    }

    @Override
    public Session createEmptySession() {
        return new RedisSession(this);
    }

    @Override
    public Session createSession(String requestedSessionId) {

        if(LOG.isDebugEnabled()) { LOG.debug("Attempting to create Session with Id " + requestedSessionId); }
        final Session session = super.createSession(requestedSessionId);

        if (session != null && session instanceof  RedisSession) {
            if(LOG.isDebugEnabled()) { LOG.debug("Created Session with Id " + requestedSessionId); }
            return this.actionHandler.addSession((RedisSession) session);
        }
        return session;
    }

    @Override
    public void add(Session session) {
        if(LOG.isDebugEnabled()) { LOG.debug("Attempting to add session with id " + session.getId()); }
        super.add(session);
        if (session instanceof RedisSession) {
            this.actionHandler.addSession((RedisSession) session);
        }
    }

    @Override
    public void remove(Session session) {
        remove(session, false);
    }

    @Override
    public void remove(Session session, boolean update) {
        if(LOG.isDebugEnabled()) { LOG.debug("Attempting to remove session with id " + session.getId()); }
        super.remove(session, update);
        if (session instanceof RedisSession) {
            this.actionHandler.removeSession((RedisSession) session);
        }
    }

    @Override
    public Session findSession(String id) throws IOException {
        if(LOG.isDebugEnabled()) { LOG.debug("Attempting to find session with id " + id); }
        final Session session = super.findSession(id);
        return (session instanceof RedisSession) ? session : this.actionHandler.loadSession(id);
    }

    public void postRequest() {
        this.actionHandler.flushActions();
    }

}

