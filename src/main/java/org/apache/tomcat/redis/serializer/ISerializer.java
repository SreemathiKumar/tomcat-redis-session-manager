package org.apache.tomcat.redis.serializer;

import java.io.Serializable;

/**
 * Interface for Serializer.
 */
public interface ISerializer {

    /**
     * Setting the <code>ClassLoader</code> to be used for serialization and de-serialization.
     * @param classLoader
     */
    void setClassLoader(ClassLoader classLoader);

    /**
     * Serialize a <code>Serializable</code> object.
     * @param object Object to be serialized.
     * @return <code>String</code> version of the serialized version of the Object.
     * @throws SerializationException
     */
    String serialize(final Serializable object) throws SerializationException;

    /**
     * Construct the <code>Serializable</code> object back from <code>String</code> version.
     * @param serializedString <code>String</code> version of the <code>Serializable</code> object.
     * @return <code>Serializable</code> object.
     * @throws SerializationException
     */
    Serializable deSerialize(final String serializedString) throws SerializationException;
}
