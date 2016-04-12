package org.apache.tomcat.redis.session;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.Session;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.redis.session.RedisCommand.Command;
import org.apache.tomcat.redis.store.RedisStoreManager;
import org.apache.tomcat.redis.util.StringUtils;
import redis.clients.jedis.Jedis;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handler for all Redis session actions.
 */
public class RedisSessionActionHandler {
    private static final Log LOG = LogFactory.getLog(RedisSessionActionHandler.class);

    /**
     * Session keys
     */
    private static final String SESSION = "session";
    private static final String MAIN = "main";
    private static final String NOTES = "notes";
    private static final String ATTRIBUTES = "attributes";
    private static final String COLON = ":";

    private static final List<String> SESSION_BUCKETS = Arrays.asList(MAIN, NOTES, ATTRIBUTES);

    /**
     * Session main map keys
     */
    private static final String ID = "id";
    private static final String AUTH_TYPE = "authtype";
    private static final String PRINCIPAL = "principal";
    private static final String CTIME = "ctime";

    /**
     * Redis Command Registry
     */
    protected final Queue<RedisCommand> registry;
    protected final int maxRegistrySize;
    protected final Lock lock;


    /**
     * Redis Store Manager
     */
    protected final RedisStoreManager storeManager;

    /**
     * the default maximum inactive interval (in seconds) for Sessions created by the Manager.
     */
    protected final int maxInactiveInterval;

    public RedisSessionActionHandler(final RedisStoreManager storeManager, final int maxInactiveInterval, final int maxRegistrySize) throws LifecycleException {
        this.storeManager = storeManager;
        this.maxInactiveInterval = maxInactiveInterval;
        this.registry = new ConcurrentLinkedQueue<RedisCommand>();
        this.lock = new ReentrantLock();
        this.maxRegistrySize = maxRegistrySize;
    }

    /**
     * Registers the session Id in Redis Store.
     *
     * @param requestedSessionId
     * @return requestedSessionId if the session did not exist. Else null if the session with the id already exists.
     */
    public String regsisterSessionId(String requestedSessionId, boolean overwrite) {
        if(requestedSessionId == null) {
            return null;
        }

        final String key = getKey(requestedSessionId, MAIN);

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
            registerSessionAccess(requestedSessionId);
            this.storeManager.returnConnection(jedis);
        }
    }

    public void registerSessionPrincipal(RedisSession session) {
        registerCommand(new RedisCommand(session.getId())
                .setCommand(session.getPrincipal() == null ? Command.HDEL : Command.HSET)
                .setKey(getKey(session.getId(), MAIN))
                .setField(PRINCIPAL)
                .setValue((Serializable) session.getPrincipal()));
    }

    public void registerSessionCreationTime(RedisSession session) {
        registerCommand(new RedisCommand(session.getId())
                .setCommand(Command.HSET)
                .setKey(getKey(session.getId(), MAIN))
                .setField(CTIME)
                .setValue(session.getCreationTime()));
    }

    public void registerSessionAuthType(RedisSession session) {
        registerCommand(new RedisCommand(session.getId())
                .setCommand(session.getAuthType() == null ? Command.HDEL : Command.HSET)
                .setKey(getKey(session.getId(), MAIN))
                .setField(AUTH_TYPE)
                .setValue(session.getAuthType()));
    }

    public void removeSessionNote(RedisSession session, String name) {
        registerCommand(new RedisCommand(session.getId())
                .setCommand(Command.HDEL)
                .setKey(getKey(session.getId(), NOTES))
                .setField(name));
    }

    public void registerSessionNote(RedisSession session, String name, Object value) {
        registerCommand(new RedisCommand(session.getId())
                .setCommand(value == null ? Command.HDEL : Command.HSET)
                .setKey(getKey(session.getId(), NOTES))
                .setField(name)
                .setValue((Serializable) value));
    }

    public void removeSessionAttribute(RedisSession session, String name) {
        registerCommand(new RedisCommand(session.getId())
                .setCommand(Command.HDEL)
                .setKey(getKey(session.getId(), ATTRIBUTES))
                .setField(name));
    }

    public void registerSessionAttribute(RedisSession session, String name, Object value) {
        registerCommand(new RedisCommand(session.getId())
                .setCommand(value == null ? Command.HDEL : Command.HSET)
                .setKey(getKey(session.getId(), ATTRIBUTES))
                .setField(name)
                .setValue((Serializable) value));
    }

    public void removeSession(RedisSession session) {
        for(String keyType : SESSION_BUCKETS) {
            registerCommand(new RedisCommand(session.getId()).setCommand(Command.DEL).setKey(getKey(session.getId(), keyType)));
        }
    }

    public Session addSession(RedisSession session) {
        if(session.getId() != null) {
            regsisterSessionId(session.getId(), true);
            registerSessionAccess(session.getId());
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
        }

        return session;
    }

    public Session loadSession(String sessionId) {
        registerSessionAccess(sessionId);
        // TODO:
        return null;
    }

    /**
     * Attempts to flush the actions in memory.
     * Will be successful if it was able to get hold of a lock.
     */
    public void flushActions() {
        if(LOG.isDebugEnabled()) { LOG.debug("Attempting to flush all redis actions "); }

        if(this.lock.tryLock()) {
            try {
                final List<RedisCommand> commands = new ArrayList<RedisCommand>(this.registry.size());
                while(!this.registry.isEmpty()) {
                    commands.add(this.registry.poll());
                }
                this.storeManager.execute(commands);
            } finally {
                this.lock.unlock();
            }
        }
    }

    protected void registerCommand(RedisCommand command) {
        this.registry.offer(command);

        if(this.registry.size() > this.maxRegistrySize) {
            flushActions();
        }
    }

    protected void registerSessionAccess(final String sessionId) {
        for(String keyType : SESSION_BUCKETS) {
            this.registry.add(new RedisCommand(sessionId).setCommand(Command.EXPIRY).setKey(getKey(sessionId, keyType)).setExpiryInterval(this.maxInactiveInterval));
        }
    }

    private String getKey(String sessionId, String bucketType) {
        return (SESSION_BUCKETS.contains(bucketType)) ?  StringUtils.join(Arrays.asList(SESSION, bucketType, sessionId), COLON) : null;
    }
}
