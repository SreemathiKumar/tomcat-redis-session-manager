package org.apache.tomcat.redis.session;

import org.apache.catalina.LifecycleListener;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.tomcat.redis.store.RedisStoreManager;

/**
 *
 */
abstract class BaseRedisStoreManager extends ManagerBase {
    /**
     * The lifecycle event support for this component.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        this.lifecycle.addLifecycleListener(listener);
    }

    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return this.lifecycle.findLifecycleListeners();
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        this.lifecycle.removeLifecycleListener(listener);
    }

    /**
     * Redis Store Manager
     */
    private RedisStoreManager storeManager = new RedisStoreManager();

    public RedisStoreManager getStoreManager() {
        return storeManager;
    }

    public void setHost(String host) {
        this.storeManager.setHost(host);
    }

    public void setPort(int port) {
        this.storeManager.setPort(port);
    }

    public void setDatabase(int database) {
        this.storeManager.setDatabase(database);
    }

    public void setPassword(String password) {
        this.storeManager.setPassword(password);
    }

    public void setTimeout(int timeout) {
        this.storeManager.setTimeout(timeout);
    }

    public void setSentinels(String sentinels) {
        this.storeManager.setSentinels(sentinels);
    }

    public void setSentinelMaster(String sentinelMaster) {
        this.storeManager.setSentinelMaster(sentinelMaster);
    }

    public int getConnectionPoolMaxTotal() {
        return this.storeManager.getConnectionPoolConfig().getMaxTotal();
    }

    public void setConnectionPoolMaxTotal(int connectionPoolMaxTotal) {
        this.storeManager.getConnectionPoolConfig().setMaxTotal(connectionPoolMaxTotal);
    }

    public int getConnectionPoolMaxIdle() {
        return this.storeManager.getConnectionPoolConfig().getMaxIdle();
    }

    public void setConnectionPoolMaxIdle(int connectionPoolMaxIdle) {
        this.storeManager.getConnectionPoolConfig().setMaxIdle(connectionPoolMaxIdle);
    }

    public int getConnectionPoolMinIdle() {
        return this.storeManager.getConnectionPoolConfig().getMinIdle();
    }

    public void setConnectionPoolMinIdle(int connectionPoolMinIdle) {
        this.storeManager.getConnectionPoolConfig().setMinIdle(connectionPoolMinIdle);
    }

    public boolean getLifo() {
        return this.storeManager.getConnectionPoolConfig().getLifo();
    }

    public void setLifo(boolean lifo) {
        this.storeManager.getConnectionPoolConfig().setLifo(lifo);
    }

    public long getMaxWaitMillis() {
        return this.storeManager.getConnectionPoolConfig().getMaxWaitMillis();
    }

    public void setMaxWaitMillis(long maxWaitMillis) {
        this.storeManager.getConnectionPoolConfig().setMaxWaitMillis(maxWaitMillis);
    }

    public long getMinEvictableIdleTimeMillis() {
        return this.storeManager.getConnectionPoolConfig().getMinEvictableIdleTimeMillis();
    }

    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.storeManager.getConnectionPoolConfig().setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
    }

    public long getSoftMinEvictableIdleTimeMillis() {
        return this.storeManager.getConnectionPoolConfig().getSoftMinEvictableIdleTimeMillis();
    }

    public void setSoftMinEvictableIdleTimeMillis(long softMinEvictableIdleTimeMillis) {
        this.storeManager.getConnectionPoolConfig().setSoftMinEvictableIdleTimeMillis(softMinEvictableIdleTimeMillis);
    }

    public int getNumTestsPerEvictionRun() {
        return this.storeManager.getConnectionPoolConfig().getNumTestsPerEvictionRun();
    }

    public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        this.storeManager.getConnectionPoolConfig().setNumTestsPerEvictionRun(numTestsPerEvictionRun);
    }

    public boolean getTestOnCreate() {
        return this.storeManager.getConnectionPoolConfig().getTestOnCreate();
    }

    public void setTestOnCreate(boolean testOnCreate) {
        this.storeManager.getConnectionPoolConfig().setTestOnCreate(testOnCreate);
    }

    public boolean getTestOnBorrow() {
        return this.storeManager.getConnectionPoolConfig().getTestOnBorrow();
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        this.storeManager.getConnectionPoolConfig().setTestOnBorrow(testOnBorrow);
    }

    public boolean getTestOnReturn() {
        return this.storeManager.getConnectionPoolConfig().getTestOnReturn();
    }

    public void setTestOnReturn(boolean testOnReturn) {
        this.storeManager.getConnectionPoolConfig().setTestOnReturn(testOnReturn);
    }

    public boolean getTestWhileIdle() {
        return this.storeManager.getConnectionPoolConfig().getTestWhileIdle();
    }

    public void setTestWhileIdle(boolean testWhileIdle) {
        this.storeManager.getConnectionPoolConfig().setTestWhileIdle(testWhileIdle);
    }

    public long getTimeBetweenEvictionRunsMillis() {
        return this.storeManager.getConnectionPoolConfig().getTimeBetweenEvictionRunsMillis();
    }

    public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        this.storeManager.getConnectionPoolConfig().setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
    }

    public String getEvictionPolicyClassName() {
        return this.storeManager.getConnectionPoolConfig().getEvictionPolicyClassName();
    }

    public void setEvictionPolicyClassName(String evictionPolicyClassName) {
        this.storeManager.getConnectionPoolConfig().setEvictionPolicyClassName(evictionPolicyClassName);
    }

    public boolean getBlockWhenExhausted() {
        return this.storeManager.getConnectionPoolConfig().getBlockWhenExhausted();
    }

    public void setBlockWhenExhausted(boolean blockWhenExhausted) {
        this.storeManager.getConnectionPoolConfig().setBlockWhenExhausted(blockWhenExhausted);
    }

    public boolean getJmxEnabled() {
        return this.storeManager.getConnectionPoolConfig().getJmxEnabled();
    }

    public void setJmxEnabled(boolean jmxEnabled) {
        this.storeManager.getConnectionPoolConfig().setJmxEnabled(jmxEnabled);
    }

    public String getJmxNameBase() {
        return this.storeManager.getConnectionPoolConfig().getJmxNameBase();
    }

    public void setJmxNameBase(String jmxNameBase) {
        this.storeManager.getConnectionPoolConfig().setJmxNameBase(jmxNameBase);
    }

    public String getJmxNamePrefix() {
        return this.storeManager.getConnectionPoolConfig().getJmxNamePrefix();
    }

    public void setJmxNamePrefix(String jmxNamePrefix) {
        this.storeManager.getConnectionPoolConfig().setJmxNamePrefix(jmxNamePrefix);
    }
}
