package org.apache.tomcat.redis.store;

import com.sun.deploy.util.StringUtils;
import org.apache.catalina.LifecycleException;
import redis.clients.jedis.*;
import redis.clients.util.Pool;

import java.util.*;

public class RedisStoreManager {

    private static final String COMMA = ",";
    protected String NULL = "null";

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

    protected Jedis acquireConnection() {
        final Jedis jedis = connectionPool.getResource();

        if (getDatabase() != 0) {
            jedis.select(getDatabase());
        }

        return jedis;
    }

    protected void returnConnection(Jedis jedis) {
        jedis.close();
    }

    /**
     * Delete all the keys of the currently selected DB. This command never fails.
     * @return Status code reply
     */
    public void flushDB() {
        final Jedis jedis = acquireConnection();
        try {
            jedis.flushDB();
        } finally {
            returnConnection(jedis);
        }
    }

    public int getSize() {
        final Jedis jedis = acquireConnection();
        try {
            return jedis.dbSize().intValue();
        } finally {
            returnConnection(jedis);
        }
    }

    public Set<String> keys() {
        return keys(null);
    }

    public Set<String> keys(String pattern) {
        pattern = pattern != null ? pattern : "*";
        final Jedis jedis = acquireConnection();
        try {
            return jedis.keys(pattern);
        } finally {
            returnConnection(jedis);
        }
    }

    /**
     * Test if the specified key exists. The command returns "1" if the key exists, otherwise "0" is
     * returned. Note that even keys set with an empty string as value will return "1". Time
     * complexity: O(1)
     *
     * @param key
     * @return Boolean reply, true if the key exists, otherwise false
     */
    public boolean exists(String key) {
        final Jedis jedis = acquireConnection();
        try {
            return jedis.exists(key);
        } finally {
            returnConnection(jedis);
        }
    }

    /**
     * Set the string value as value of the key. The string can't be longer than 1073741824 bytes (1
     * GB).
     * <p/>
     * Time complexity: O(1)
     *
     * @param key
     * @param value
     * @return Status code reply
     */
    public String set(final String key, final String value) {
        final Jedis jedis = acquireConnection();
        try {
            return jedis.set(key, value != null ? value : NULL);
        } finally {
            // TODO: Consider expiration
            returnConnection(jedis);
        }
    }

    /**
     * SETNX works exactly like {@link #set(String, String) SET} with the only difference that if the
     * key already exists no operation is performed. SETNX actually means "SET if Not eXists".
     * <p/>
     * Time complexity: O(1)
     *
     * @param key
     * @param value
     * @return Integer reply, specifically: 1 if the key was set 0 if the key was not set
     */
    public Long setnx(final String key, final String value) {
        final Jedis jedis = acquireConnection();
        try {
            return jedis.setnx(key, value != null ? value : NULL);
        } finally {
            // TODO: Consider expiration
            returnConnection(jedis);
        }
    }
}
