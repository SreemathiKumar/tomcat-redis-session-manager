package org.vijaysanthosh.tomcat.redis.store;

import org.apache.catalina.LifecycleException;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.vijaysanthosh.tomcat.redis.serializer.ISerializer;
import org.vijaysanthosh.tomcat.redis.serializer.SerializationException;
import org.vijaysanthosh.tomcat.redis.session.RedisCommand;
import org.vijaysanthosh.tomcat.redis.util.StringUtils;
import redis.clients.jedis.*;
import redis.clients.util.Pool;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RedisStoreManager {

    private static final Log LOG = LogFactory.getLog(RedisStoreManager.class);

    private static final String COMMA = ",";

    /**
     * Serializer initialization
     */
    protected ISerializer serializer;
    protected String serializationStrategyClass = "org.vijaysanthosh.tomcat.redis.serializer.JavaSerializer";

    protected String host = "localhost";
    protected int port = 6379;
    protected int database = 0;
    protected String password = null;
    protected int timeout = Protocol.DEFAULT_TIMEOUT;

    protected String sentinels = null;
    protected String sentinelMaster = null;
    protected Set<String> sentinelSet = null;

    protected Pool<Jedis> connectionPool;
    protected JedisPoolConfig connectionPoolConfig = new JedisPoolConfig();

    protected ExecutorService[] executors = null;
    protected int executorPoolSize = Runtime.getRuntime().availableProcessors();

    protected ClassLoader classLoader = null;

    public String getHost() {
        return host;
    }

    public void setSerializationStrategyClass(String strategy) {
        this.serializationStrategyClass = strategy;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void setSentinels(String sentinels) {
        this.sentinelSet = new LinkedHashSet<String>();
        if (sentinels != null) {
            for (String sentinel : StringUtils.splitString(sentinels, COMMA))
                this.sentinelSet.add(sentinel.trim());
        }

        this.sentinels = StringUtils.join(this.sentinelSet, COMMA);
    }

    public String getSentinelMaster() {
        return sentinelMaster;
    }

    public void setSentinelMaster(String sentinelMaster) {
        this.sentinelMaster = sentinelMaster;
    }

    public Set<String> getSentinelSet() {
        return this.sentinelSet;
    }

    public JedisPoolConfig getConnectionPoolConfig() {
        return this.connectionPoolConfig;
    }

    public void setExecutorPoolSize(int executorPoolSize) {
        this.executorPoolSize = executorPoolSize;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void init() throws LifecycleException {

        // Need to ensure that the Commands belonging to the same session
        // are executed serially. Hence creating multiple executors of size 1
        // for different buckets of commands.
        this.executors = new ExecutorService[this.executorPoolSize];
        for(int i = 0; i< this.executors.length; i++) {
            this.executors[i] = Executors.newSingleThreadExecutor();
        }

        try {
            this.serializer = getSerializer();

            if (getSentinelMaster() != null) {
                Set<String> sentinelSet = getSentinelSet();
                if (sentinelSet != null && sentinelSet.size() > 0) {
                    this.connectionPool = new JedisSentinelPool(getSentinelMaster(), sentinelSet, this.connectionPoolConfig, getTimeout(), getPassword());
                } else {
                    throw new LifecycleException("Error configuring Redis Sentinel connection pool: expected both `sentinelMaster` and `sentiels` to be configured");
                }
            } else {
                this.connectionPool = new JedisPool(this.connectionPoolConfig, getHost(), getPort(), getTimeout(), getPassword());
            }
        } catch (Exception e) {
            throw new LifecycleException("Error connecting to Redis", e);
        }
    }

    public void destroy() {

        shutdownExecutors(); // Shutdown Executors. This ensures that no new tasks are accepted.
        awaitTermination(2, TimeUnit.MINUTES); // Ensures previous tasks are completed. Taking 2 mins as the timeout

        // After completion of all tasks, the Redis connection pool can be destroyed.
        try {
            this.connectionPool.destroy();
        } catch (Exception e) {
            // Do nothing to prevent anything untoward from happening
        }
    }

    private void shutdownExecutors() {
        for (ExecutorService executor : this.executors) {
            try {
                if (executor != null && !executor.isShutdown()) {
                    executor.shutdown();
                }
            } catch (Exception e) {
                // Do nothing to prevent anything untoward from happening
            }
        }
    }

    private void awaitTermination(long timeout, TimeUnit unit) {
        for (ExecutorService executor : this.executors) {
            try {
                if (executor != null && !executor.isShutdown()) {
                    executor.awaitTermination(timeout, unit);
                }
            } catch (Exception e) {
                // Do nothing to prevent anything untoward from happening
            }
        }
    }

    protected Jedis acquireConnection() {
        final Jedis jedis = this.connectionPool.getResource();

        if (getDatabase() != 0) {
            jedis.select(getDatabase());
        }

        return jedis;
    }

    protected void returnConnection(Jedis jedis) {
        jedis.close();
    }

    public Serializable hset(final String key, final String field, final Serializable value, final boolean overwrite) throws SerializationException {
        if(key == null || field == null || value == null) {
            return null;
        }

        // This has to be synchronous call
        // Hence getting Jedis instance and operating on it.
        final Jedis jedis = acquireConnection();
        try {
            final String serializedValue = this.serializer.serialize(value);
            if(overwrite) {
                jedis.hset(key, field, serializedValue);
                return value;
            }
            return jedis.hsetnx(key, field, serializedValue) == 0L ? null : value;
        } finally {
            returnConnection(jedis);
        }
    }

    public void execute(List<RedisCommand> commands) {

        // The logic below ensures that the commands belonging to the same session are executed serially.
        // The execution are async which ensures that the callers are not blocked for persistence.
        if(commands != null && !commands.isEmpty()) {
            final Map<Integer, List<RedisCommand>> orderMap = new HashMap<Integer, List<RedisCommand>>();
            for(RedisCommand command : commands) {
                final int hashkey = (command.getSessionId() != null ? Math.abs(command.getSessionId().hashCode()) : 0) % this.executors.length;
                if(orderMap.get(hashkey) == null) {
                    orderMap.put(hashkey, new ArrayList<RedisCommand>());
                }
                orderMap.get(hashkey).add(command);
            }

            for(Map.Entry<Integer, List<RedisCommand>> entry : orderMap.entrySet()) {
                this.executors[entry.getKey()].execute(new PersistTask(entry.getValue(), this, this.serializer));
            }
        }
    }

    public Map<String, Serializable> loadData(final String key) throws SerializationException {
        final Jedis jedis = acquireConnection();
        try {
            final Map<String, String> rawData = jedis.hgetAll(key);
            if(rawData != null && !rawData.isEmpty()) {
                final Map<String, Serializable> deSerialized = new HashMap<String, Serializable>(rawData.size());
                for(Map.Entry<String, String> rawEntry : rawData.entrySet()) {
                    try {
                        deSerialized.put(rawEntry.getKey(), this.serializer.deSerialize(rawEntry.getValue()));
                    } catch (SerializationException e) {
                        // Need the details for better messaging.
                        throw new SerializationException("Error loading data from redis for key "+ key + " and field "+ rawEntry.getKey(), e);
                    }
                }
                return deSerialized;
            }
        } finally {
            returnConnection(jedis);
        }

        return null;
    }

    protected ISerializer getSerializer() throws LifecycleException {
        try {
            LOG.info("Instantiating serializer of type " + this.serializationStrategyClass);
            ISerializer serializer = (ISerializer) Class.forName(this.serializationStrategyClass).newInstance();
            serializer.setClassLoader(this.classLoader);
            return serializer;
        } catch (ClassNotFoundException e) {
            throw new LifecycleException(e);
        } catch (InstantiationException e) {
            throw new LifecycleException(e);
        } catch (IllegalAccessException e) {
            throw new LifecycleException(e);
        }
    }

    private static class PersistTask implements Runnable {
        private static final Log LOG = LogFactory.getLog(PersistTask.class);

        private final List<RedisCommand> commands;
        private final RedisStoreManager storeManager;
        private final ISerializer serializer;

        private PersistTask(List<RedisCommand> commands, RedisStoreManager storeManager, ISerializer serializer) {
            this.commands = commands;
            this.storeManager = storeManager;
            this.serializer = serializer;
        }

        @Override
        public void run() {
            if(LOG.isDebugEnabled()) { LOG.debug("Number of RedisCommands to be executed is " + commands.size()); }

            final Jedis jedis = this.storeManager.acquireConnection();
            try {
                for(RedisCommand command : commands) {
                    executeCommand(command, jedis);
                }
            } finally {
                this.storeManager.returnConnection(jedis);
            }

        }

        private void executeCommand(final RedisCommand command, final Jedis jedis) {
            try {
                if(LOG.isDebugEnabled()) { LOG.debug("Executing RedisCommand " + command); }
                switch (command.getCommand()) {
                    case DEL:
                        jedis.del(command.getKey());
                        break;
                    case HSET:
                        jedis.hset(command.getKey(), command.getField(), this.serializer.serialize(command.getValue()));
                        break;
                    case HDEL:
                        jedis.hdel(command.getKey(), command.getField());
                        break;
                    case EXPIRY:
                        jedis.expire(command.getKey(), command.getExpiryInterval());
                        break;
                }

            } catch (Exception e) {
                LOG.error("Error executing RedisCommand " + command, e);
            }
        }
    }
}
