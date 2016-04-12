package org.apache.tomcat.redis.session;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.Session;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.redis.serializer.ISerializer;
import org.apache.tomcat.redis.session.RedisCommand.Command;
import org.apache.tomcat.redis.store.RedisStoreManager;
import org.apache.tomcat.redis.util.StringUtils;
import redis.clients.jedis.Jedis;

import java.io.Serializable;
import java.util.*;

/**
 * Handler for all Redis session actions.
 */
public class RedisSessionActionHandler {
    private static final Log LOG = LogFactory.getLog(RedisSessionActionHandler.class);

    private static final int SESSION_MAIN = 0;
    private static final int SESSION_ATTRIBUTES = 2;
    private static final int SESSION_NOTES = 3;

    /**
     * Session keys
     */
    private static final String SESSION = "session";
    private static final String MAIN = "main";
    private static final String NOTES = "notes";
    private static final String ATTRIBUTES = "attributes";
    private static final String COLON = ":";

    /**
     * Session main map keys
     */
    private static final String ID = "id";
    private static final String AUTH_TYPE = "authtype";
    private static final String PRINCIPAL = "principal";
    private static final String CTIME = "ctime";

    /**
     * Current Session Placeholder for web-request thread.
     */
    protected ThreadLocal<List<RedisCommand>> COMMAND_REGISTER = new ThreadLocal<List<RedisCommand>>();

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
    public String regsisterSessionId(String requestedSessionId, boolean overwrite) {
        final String key = getKey(requestedSessionId, SESSION_MAIN);

        // This has to be synchronous call
        // Hence getting Jedis instance and operating on it.
        final Jedis jedis = this.storeManager.acquireConnection();
        try {
            if(overwrite) {
                jedis.hset(key, ID, requestedSessionId);
                return requestedSessionId;
            }
            return jedis.hsetnx(key, ID, requestedSessionId) == 0L ? null : requestedSessionId;
        } finally {
            this.storeManager.returnConnection(jedis);
        }
    }

    public void registerSessionPrincipal(RedisSession session) {
        registerCommand(new RedisCommand()
                .setCommand(session.getPrincipal() == null ? Command.HDEL : Command.HSET)
                .setKey(getKey(session.getId(), SESSION_MAIN))
                .setField(PRINCIPAL)
                .setValue((Serializable) session.getPrincipal()));
    }

    public void registerSessionCreationTime(RedisSession session) {
        registerCommand(new RedisCommand()
                .setCommand(Command.HSET)
                .setKey(getKey(session.getId(), SESSION_MAIN))
                .setField(CTIME)
                .setValue(session.getCreationTime()));
    }

    public void registerSessionAuthType(RedisSession session) {
        registerCommand(new RedisCommand()
                .setCommand(session.getAuthType() == null ? Command.HDEL : Command.HSET)
                .setKey(getKey(session.getId(), SESSION_MAIN))
                .setField(AUTH_TYPE)
                .setValue(session.getAuthType()));
    }

    public void removeSessionNote(RedisSession session, String name) {
        registerCommand(new RedisCommand()
                .setCommand(Command.HDEL)
                .setKey(getKey(session.getId(), SESSION_NOTES))
                .setField(name));
    }

    public void registerSessionNote(RedisSession session, String name, Object value) {
        registerCommand(new RedisCommand()
                .setCommand(value == null ? Command.HDEL : Command.HSET)
                .setKey(getKey(session.getId(), SESSION_NOTES))
                .setField(name)
                .setValue((Serializable) value));
    }

    public void removeSessionAttribute(RedisSession session, String name) {
        registerCommand(new RedisCommand()
                .setCommand(Command.HDEL)
                .setKey(getKey(session.getId(), SESSION_ATTRIBUTES))
                .setField(name));
    }

    public void registerSessionAttribute(RedisSession session, String name, Object value) {
        registerCommand(new RedisCommand()
                .setCommand(value == null ? Command.HDEL : Command.HSET)
                .setKey(getKey(session.getId(), SESSION_ATTRIBUTES))
                .setField(name)
                .setValue((Serializable) value));
    }

    public void removeSession(RedisSession session) {
        registerCommands(Arrays.asList(
                new RedisCommand().setCommand(Command.DEL).setKey(getKey(session.getId(), SESSION_MAIN)),
                new RedisCommand().setCommand(Command.DEL).setKey(getKey(session.getId(), SESSION_NOTES)),
                new RedisCommand().setCommand(Command.DEL).setKey(getKey(session.getId(), SESSION_ATTRIBUTES))
        ));
    }

    public Session addSession(RedisSession session) {
        regsisterSessionId(session.getId(), true);
        registerSessionPrincipal(session);
        registerSessionCreationTime(session);
        registerSessionAuthType(session);

        Iterator<String> notesIter = session.getNoteNames();
        while (notesIter.hasNext()) {
            final String note = notesIter.next();
            final Object value = session.getNote(note);
            registerSessionNote(session, note, value);
        }

        final Enumeration<String> attributes = session.getAttributeNames();
        while (attributes.hasMoreElements()) {
            final String attribute = attributes.nextElement();
            final Object value = session.getAttribute(attribute);
            registerSessionAttribute(session, attribute, value);
        }

        return session;
    }

    public Session loadSession(String sessionId) {
        // TODO:
        return null;
    }

    public void flushActions() {
        if(LOG.isDebugEnabled()) { LOG.debug("Attempting to flush all redis actions "); }
        this.storeManager.execute(COMMAND_REGISTER.get());
        COMMAND_REGISTER.set(new ArrayList<RedisCommand>()); // RESET the command registry
    }

    protected void registerCommand(RedisCommand command) {
        registerCommands(Collections.singletonList(command));
    }

    protected void registerCommands(List<RedisCommand> commands) {
        if(COMMAND_REGISTER.get() == null) {
            COMMAND_REGISTER.set(new ArrayList<RedisCommand>());
        }

        COMMAND_REGISTER.get().addAll(commands);
    }

    private String getKey(String sessionId, int type) {
        switch (type) {
            case SESSION_MAIN:
                return StringUtils.join(Arrays.asList(SESSION, MAIN, sessionId), COLON);
            case SESSION_ATTRIBUTES:
                return StringUtils.join(Arrays.asList(SESSION, ATTRIBUTES, sessionId), COLON);
            case SESSION_NOTES:
                return StringUtils.join(Arrays.asList(SESSION, NOTES, sessionId), COLON);
        }

        return null;
    }
}
