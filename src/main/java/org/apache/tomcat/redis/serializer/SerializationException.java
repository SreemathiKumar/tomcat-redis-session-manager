package org.apache.tomcat.redis.serializer;

import java.io.Serializable;

class SerializationException extends Exception implements Serializable {

    public SerializationException() {
        super();
    }

    public SerializationException(String s) {
        super(s);
    }

    public SerializationException(String s, Exception e) {
        super(s, e);
    }
}