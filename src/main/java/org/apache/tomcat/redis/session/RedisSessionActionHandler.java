package org.apache.tomcat.redis.session;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.Session;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.redis.serializer.SerializationException;
import org.apache.tomcat.redis.session.RedisCommand.Command;
import org.apache.tomcat.redis.store.RedisStoreManager;
import org.apache.tomcat.redis.util.StringUtils;

import java.io.Serializable;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handler for all Redis session actions.
 * The actions can be registered by either <code>RedisSession</code> or <code>RedisSessionManager</code>.
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
     * Redis Session Manager
     */
    protected final RedisSessionManager sessionManager;

    /**
     * Redis Store Manager
     */
    protected final RedisStoreManager storeManager;

    /**
     * the default maximum inactive interval (in seconds) for Sessions created by the Manager.
     */
    protected final int maxInactiveInterval;

    /**
     * Threadlocal variable which can be used to temporarily disable <code>RedisCommand</code> registration.
     */
    private final ThreadLocal<Boolean> registrationFlag;

    /**
     * RedisSessionActionHandler constructor. Initializes the ActionHandler
     * @param sessionManager <code>RedisSessionManager</code>
     * @param maxInactiveInterval MaximumInterval in seconds to use to set the expiry of objects in Redis.
     * @param maxRegistrySize Maximum size of the in-memory registry beyond which the <code>RedisCommand</code>s are flushed to Redis.
     * @throws LifecycleException
     */
    public RedisSessionActionHandler(final RedisSessionManager sessionManager, final int maxInactiveInterval, final int maxRegistrySize) throws LifecycleException {
        this.sessionManager = sessionManager;
        this.storeManager = sessionManager.getStoreManager();
        this.maxInactiveInterval = maxInactiveInterval;
        this.registry = new ConcurrentLinkedQueue<RedisCommand>();
        this.lock = new ReentrantLock();
        this.maxRegistrySize = maxRegistrySize;

        this.registrationFlag = new ThreadLocal<Boolean>();
        this.registrationFlag.set(Boolean.TRUE);
    }

    /**
     * Registers the session Id in Redis Store.
     *
     * @param requestedSessionId Session id requested.
     * @return requestedSessionId if the session did not exist. Else null if the session with the id already exists.
     */
    public String regsisterSessionId(final String requestedSessionId, boolean overwrite) {
        final String key = getKey(requestedSessionId, MAIN);
        try {
            return (String) this.storeManager.hset(key, ID, requestedSessionId, overwrite);
        } catch (SerializationException e) {
            LOG.error("Error registering session in redis with id "+ requestedSessionId, e);
        } finally {
            registerSessionAccess(requestedSessionId);
        }

        return null;
    }

    /**
     * Registers the <code>Principal</code> object of the <code>Session</code> for persistence.
     *
     * @param session <code>RedisSession</code> to be used to get the <code>Principal</code> object.
     */
    public void registerSessionPrincipal(final RedisSession session) {
        registerCommand(new RedisCommand(session.getId())
                .setCommand(session.getPrincipal() == null ? Command.HDEL : Command.HSET)
                .setKey(getKey(session.getId(), MAIN))
                .setField(PRINCIPAL)
                .setValue((Serializable) session.getPrincipal()));
    }

    /**
     * Registers the creation time of the <code>Session</code> for persistence.
     *
     * @param session <code>RedisSession</code> to be used to get the creation time.
     */
    public void registerSessionCreationTime(final RedisSession session) {
        registerCommand(new RedisCommand(session.getId())
                .setCommand(Command.HSET)
                .setKey(getKey(session.getId(), MAIN))
                .setField(CTIME)
                .setValue(session.getCreationTime()));
    }

    /**
     * Registers the auth-type of the <code>Session</code> for persistence.
     *
     * @param session <code>RedisSession</code> to be used to get the auth-type.
     */
    public void registerSessionAuthType(final RedisSession session) {
        registerCommand(new RedisCommand(session.getId())
                .setCommand(session.getAuthType() == null ? Command.HDEL : Command.HSET)
                .setKey(getKey(session.getId(), MAIN))
                .setField(AUTH_TYPE)
                .setValue(session.getAuthType()));
    }

    /**
     * Registers the removal of a note from the given <code>Session</code>.
     *
     * @param session <code>RedisSession</code> to be used to remove the given note.
     * @param name name of the note to be removed.
     */
    public void removeSessionNote(final RedisSession session, final String name) {
        registerCommand(new RedisCommand(session.getId())
                .setCommand(Command.HDEL)
                .setKey(getKey(session.getId(), NOTES))
                .setField(name));
    }

    /**
     * Registers the <code>Session</code> note.
     *
     * @param session <code>RedisSession</code> to be used to add the given note.
     * @param name name of the note to be added in the <code>Session</code>.
     * @param value value of the note to be added in the <code>Session</code>.
     */
    public void registerSessionNote(final RedisSession session, final String name, final Object value) {
        registerCommand(new RedisCommand(session.getId())
                .setCommand(value == null ? Command.HDEL : Command.HSET)
                .setKey(getKey(session.getId(), NOTES))
                .setField(name)
                .setValue((Serializable) value));
    }

    /**
     * Registers the removal of a attribute from the given <code>Session</code>.
     *
     * @param session <code>RedisSession</code> to be used to remove the given attribute.
     * @param name name of the attribute to be removed.
     */
    public void removeSessionAttribute(final RedisSession session, final String name) {
        registerCommand(new RedisCommand(session.getId())
                .setCommand(Command.HDEL)
                .setKey(getKey(session.getId(), ATTRIBUTES))
                .setField(name));
    }

    /**
     * Registers the <code>Session</code> attribute.
     *
     * @param session <code>RedisSession</code> to be used to add the given attribute.
     * @param name name of the attribute to be added in the <code>Session</code>.
     * @param value value of the attribute to be added in the <code>Session</code>.
     */
    public void registerSessionAttribute(final RedisSession session, final String name, final Object value) {
        registerCommand(new RedisCommand(session.getId())
                .setCommand(value == null ? Command.HDEL : Command.HSET)
                .setKey(getKey(session.getId(), ATTRIBUTES))
                .setField(name)
                .setValue((Serializable) value));
    }

    /**
     * Registers the removal of the <code>RedisSession</code> completely from redis.
     *
     * @param session <code>RedisSession</code> that is registered for removal from redis.
     */
    public void removeSession(final RedisSession session) {
        for(String keyType : SESSION_BUCKETS) {
            registerCommand(new RedisCommand(session.getId()).setCommand(Command.DEL).setKey(getKey(session.getId(), keyType)));
        }
    }

    /**
     * Registers the addition of the <code>RedisSession</code> completely from redis.
     *
     * @param session <code>RedisSession</code> that is registered for addition in redis.
     */
    public Session addSession(final RedisSession session) {
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

    /**
     * Load the <code>RedisSession</code> data from redis.
     *
     * @param sessionId Session identifier used to load a <code>Session</code> data from redis
     * @return <code>RedisSession</code> if all the data is successfully loaded. null otherwise.
     * @throws SerializationException If any issues occur during de-serialization of <code>Session</code> data.
     */
    public Session loadSession(final String sessionId) throws SerializationException {
        this.registrationFlag.set(Boolean.FALSE); // Temporarily turn off registration for this thread.
        try {
            if(sessionId != null) {
                final Map<String, Serializable> mainMap = this.storeManager.loadData(getKey(sessionId, MAIN));
                RedisSession session = null;
                if(LOG.isDebugEnabled()) { LOG.debug("Attempting to load session with id " + sessionId); }
                if(mainMap != null) {
                    if(LOG.isDebugEnabled()) { LOG.debug("Successfully loaded session with id " + sessionId); }
                    session = new RedisSession(this.sessionManager);
                    session.setValid(true);
                    session.setAuthType((String) mainMap.get(AUTH_TYPE));
                    session.setPrincipal((Principal) mainMap.get(PRINCIPAL));
                    session.setCreationTime((Long) mainMap.get(CTIME));
                    session.setId(sessionId); // This does a lot of things. Hence keeping it to the end. Refer to <code>StandardSession</code> implementation.
                }

                if(session != null) {
                    final Map<String, Serializable> notesMap = this.storeManager.loadData(getKey(sessionId, NOTES));
                    if(notesMap != null) {
                        for(Map.Entry<String, Serializable> entry : notesMap.entrySet()) {
                            session.setNote(entry.getKey(), entry.getValue());
                        }
                    }

                    final Map<String, Serializable> attributesMap = this.storeManager.loadData(getKey(sessionId, ATTRIBUTES));
                    if(attributesMap != null) {
                        for(Map.Entry<String, Serializable> entry : attributesMap.entrySet()) {
                            session.setAttribute(entry.getKey(), entry.getValue());
                        }
                    }

                    registerSessionAccess(sessionId);
                }
            }
        } finally {
            this.registrationFlag.set(Boolean.TRUE);
        }

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

    /**
     * Utility method to register a <code>RedisCommand</code> to the in-memory registry.
     *
     * @param command <code>RedisCommand</code> to be registered.
     */
    protected void registerCommand(final RedisCommand command) {
        // If the registrationFlag is not set or if it set as true, proceed with registration.
        if((this.registrationFlag.get() == null || this.registrationFlag.get())
                && command.getSessionId() != null) {
            this.registry.offer(command);

            if(this.registry.size() > this.maxRegistrySize) {
                flushActions();
            }
        }
    }

    /**
     * Method to register the expiry reset of all the session objects in redis.
     *
     * @param sessionId <code>Session</code> identifier.
     */
    public void registerSessionAccess(final String sessionId) {
        for(String keyType : SESSION_BUCKETS) {
            this.registry.add(new RedisCommand(sessionId).setCommand(Command.EXPIRY).setKey(getKey(sessionId, keyType)).setExpiryInterval(this.maxInactiveInterval));
        }
    }

    /**
     * Utility method to get the key of the hash bucket in redis.
     *
     * @param sessionId <code>Session</code> identifier.
     * @param bucketType Bucket Type for the hash-map in redis.
     * @return Key to be used to refer the bucketizied hash-map in redis.
     */
    private String getKey(final String sessionId, final String bucketType) {
        return (SESSION_BUCKETS.contains(bucketType)) ?  StringUtils.join(Arrays.asList(SESSION, bucketType, sessionId), COLON) : null;
    }
}
