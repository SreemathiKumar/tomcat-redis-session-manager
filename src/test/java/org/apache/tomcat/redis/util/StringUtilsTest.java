package org.apache.tomcat.redis.util;

import junit.framework.TestCase;

import java.util.Arrays;

public class StringUtilsTest extends TestCase {

    public void testMethods() throws Exception {
        final String str = "ABCD,EFGH,XYZ";
        assertEquals(Arrays.asList("ABCD", "EFGH", "XYZ"), StringUtils.splitString(str, ","));
        assertEquals(Arrays.asList("ABCD,", "GH,XYZ"), StringUtils.splitString(str, "EF"));
        assertEquals(str, StringUtils.join(Arrays.asList("ABCD", "EFGH", "XYZ"), ","));

        assertTrue(StringUtils.hasLength(str));
    }

}
