package org.vijaysanthosh.tomcat.redis.serializer;

import junit.framework.TestCase;
import org.vijaysanthosh.tomcat.redis.serializer.model.SampleEntry;
import org.vijaysanthosh.tomcat.redis.serializer.model.SimpleEntry;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class JavaSerializerTest extends TestCase {
    public void testSerialization() throws Exception {

        final ISerializer serializer1 = new JavaSerializer();
        final ISerializer serializer2 = new JavaSerializer();
        serializer2.setClassLoader(getClass().getClassLoader());

        for(ISerializer serializer : Arrays.asList(serializer1, serializer2)) {
            final SimpleEntry simpleEntry = new SimpleEntry();
            simpleEntry.setMsg("MSG - SIMPLE");
            simpleEntry.setStatusCode(1231);

            final SampleEntry sampleEntry = new SampleEntry();
            sampleEntry.setMsg("MSG - SAMPLE");
            sampleEntry.setStatusCode(9876);
            sampleEntry.setEntry(simpleEntry);


            final List<Serializable> objects = Arrays.asList("ABC", 123, (long) 123, simpleEntry, sampleEntry);
            for(Object obj : objects) {
                checkSerializer((Serializable) obj, serializer);
            }
        }
    }

    private void checkSerializer(Serializable obj, ISerializer serializer) throws SerializationException {
        final String serializedString = serializer.serialize(obj);
        final Object deSerializedObject = serializer.deSerialize(serializedString);

        assertEquals(obj, deSerializedObject);
    }

}
