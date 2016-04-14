package org.vijaysanthosh.tomcat.redis.session;

import java.io.Serializable;

/**
 * Redis Commands needed for the Action Handler
 */
public class RedisCommand {
    public enum Command {
        DEL,
        HSET, HDEL,
        EXPIRY
    }

    private final String sessionId;
    private Command command;
    private String key;
    private String field;
    private Serializable value;
    private int expiryInterval;

    public RedisCommand(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Command getCommand() {
        return command;
    }

    public String getKey() {
        return key;
    }

    public String getField() {
        return field;
    }

    public Serializable getValue() {
        return value;
    }

    public int getExpiryInterval() {
        return expiryInterval;
    }

    public RedisCommand setCommand(Command command) {
        this.command = command;
        return this;
    }

    public RedisCommand setKey(String key) {
        this.key = key;
        return this;
    }

    public RedisCommand setField(String field) {
        this.field = field;
        return this;
    }

    public RedisCommand setValue(Serializable value) {
        this.value = value;
        return this;
    }

    public RedisCommand setExpiryInterval(int expiryInterval) {
        this.expiryInterval = expiryInterval;
        return this;
    }

    @Override
    public String toString() {
        return "RedisCommand{" +
                "command=" + command +
                ", key='" + key + '\'' +
                ", field='" + field + '\'' +
                ", value=" + value +
                ", expiryInterval=" + expiryInterval +
                '}';
    }
}
