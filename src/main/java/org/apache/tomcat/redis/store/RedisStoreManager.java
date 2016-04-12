package org.apache.tomcat.redis.store;

import org.apache.catalina.LifecycleException;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.redis.session.RedisCommand;
import org.apache.tomcat.redis.util.StringUtils;
import redis.clients.jedis.*;
import redis.clients.util.Pool;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class RedisStoreManager {

    private static final Log LOG = LogFactory.getLog(RedisStoreManager.class);

    private static final String COMMA = ",";

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

    public String getHost() {
        return host;
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

    public void init() throws LifecycleException {
        try {
            if (getSentinelMaster() != null) {
                Set<String> sentinelSet = getSentinelSet();
                if (sentinelSet != null && sentinelSet.size() > 0) {
                    connectionPool = new JedisSentinelPool(getSentinelMaster(), sentinelSet, this.connectionPoolConfig, getTimeout(), getPassword());
                } else {
                    throw new LifecycleException("Error configuring Redis Sentinel connection pool: expected both `sentinelMaster` and `sentiels` to be configured");
                }
            } else {
                connectionPool = new JedisPool(this.connectionPoolConfig, getHost(), getPort(), getTimeout(), getPassword());
            }
        } catch (Exception e) {
            throw new LifecycleException("Error connecting to Redis", e);
        }
    }

    public void destroy() {
        try {
            connectionPool.destroy();
        } catch (Exception e) {
            // Do nothing to prevent anything untoward from happening
        }
    }

    public Jedis acquireConnection() {
        final Jedis jedis = connectionPool.getResource();

        if (getDatabase() != 0) {
            jedis.select(getDatabase());
        }

        return jedis;
    }

    public void returnConnection(Jedis jedis) {
        jedis.close();
    }

    public void execute(List<RedisCommand> commands) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("RedisCommands to be executed");
            if(commands != null) {
                for(RedisCommand command : commands) {
                    LOG.info(command.toString());
                }
            }
        }

        // TODO: Execute the commands in Redis DB
    }
}
