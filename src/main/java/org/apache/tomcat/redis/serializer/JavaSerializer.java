package org.apache.tomcat.redis.serializer;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.*;

/**
 * JAXB implementation of <code>ISerializer</code>.
 */
public final class JavaSerializer implements ISerializer {

    private static final BASE64Encoder ENCODER = new BASE64Encoder();
    private static final BASE64Decoder DECODER = new BASE64Decoder();

    @Override
    public String serialize(Serializable object) throws SerializationException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bos));
            try {
                oos.writeObject(object);
                oos.flush();
            } finally {
                close(oos);
            }

            return ENCODER.encode(bos.toByteArray());
        } catch (IOException e) {
            throw new SerializationException("Unexpected error during serialization.", e);
        } finally {
            close(bos);
        }
    }

    @Override
    public Serializable deSerialize(String serializedString) throws SerializationException {
        try {
            final InputStream bis = new ByteArrayInputStream(DECODER.decodeBuffer(serializedString));
            try {
                ObjectInputStream ois = new ObjectInputStream(bis);
                try {
                    return (Serializable) ois.readObject();
                } finally {
                    close(ois);
                }
            } finally {
                close(bis);
            }
        } catch (Exception e) {
            throw new SerializationException("Unexpected error during de-serialization.", e);
        }
    }

    private void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            // Do nothing
        }
    }
}
