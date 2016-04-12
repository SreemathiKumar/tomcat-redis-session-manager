package org.apache.tomcat.redis.session;

import java.io.Serializable;

/**
 * Redis Commands needed for the Action Handler
 */
public class RedisCommand {
    enum Command {
        HSET, HGET, HDEL,
        DEL, SET,
        EXPIRY
    }

    private Command command;
    private String key;
    private String field;
    private Serializable value;

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

    @Override
    public String toString() {
        return "RedisCommand{" +
                "command=" + command +
                ", key='" + key + '\'' +
                ", field='" + field + '\'' +
                ", value=" + value +
                '}';
    }
}
