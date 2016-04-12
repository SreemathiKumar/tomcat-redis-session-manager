package org.apache.tomcat.redis.store;

import org.apache.catalina.LifecycleException;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.redis.serializer.ISerializer;
import org.apache.tomcat.redis.session.RedisCommand;
import org.apache.tomcat.redis.util.StringUtils;
import redis.clients.jedis.*;
import redis.clients.util.Pool;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisStoreManager {

    private static final Log LOG = LogFactory.getLog(RedisStoreManager.class);

    private static final String COMMA = ",";

    /**
     * Serializer initialization
     */
    protected ISerializer serializer;
    protected String serializationStrategyClass = "org.apache.tomcat.redis.serializer.JavaSerializer";

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

    protected ExecutorService executor = null;
    protected int executorPoolSize = Runtime.getRuntime().availableProcessors();

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

    public String getSentinels() {
        return sentinels;
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

    public void init() throws LifecycleException {

        this.executor = Executors.newFixedThreadPool(this.executorPoolSize);

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
        try {
            this.executor.shutdown();
        } catch (Exception e) {
            // Do nothing to prevent anything untoward from happening
        }

        try {
            this.connectionPool.destroy();
        } catch (Exception e) {
            // Do nothing to prevent anything untoward from happening
        }
    }

    public Jedis acquireConnection() {
        final Jedis jedis = this.connectionPool.getResource();

        if (getDatabase() != 0) {
            jedis.select(getDatabase());
        }

        return jedis;
    }

    public void returnConnection(Jedis jedis) {
        jedis.close();
    }

    public void execute(List<RedisCommand> commands) {
        if(commands != null) {
            this.executor.execute(new PersistTask(commands, this, this.serializer));
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
            if(LOG.isDebugEnabled()) { LOG.debug("RedisCommands to be executed"); }

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

    protected ISerializer getSerializer() throws LifecycleException {
        try {
            LOG.info("Instantiating serializer of type " + this.serializationStrategyClass);
            return (ISerializer) Class.forName(this.serializationStrategyClass).newInstance();
        } catch (ClassNotFoundException e) {
            throw new LifecycleException(e);
        } catch (InstantiationException e) {
            throw new LifecycleException(e);
        } catch (IllegalAccessException e) {
            throw new LifecycleException(e);
        }
    }
}
