package org.vijaysanthosh.tomcat.redis.util;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;

public class StringUtilsTest extends TestCase {

    public void testMethods() throws Exception {
        final String str = "ABCD,EFGH,XYZ";
        assertEquals(Arrays.asList("ABCD", "EFGH", "XYZ"), StringUtils.splitString(str, ","));
        assertEquals(Arrays.asList("ABCD,", "GH,XYZ"), StringUtils.splitString(str, "EF"));
        assertTrue(StringUtils.splitString(str, null).isEmpty());
        assertTrue(StringUtils.splitString("", "EF").isEmpty());
        assertTrue(StringUtils.splitString(null, "EF").isEmpty());

        assertEquals(str, StringUtils.join(Arrays.asList("ABCD", "EFGH", "XYZ"), ","));
        assertEquals("", StringUtils.join(Collections.emptyList(), ","));
        assertEquals("", StringUtils.join(null, ","));

        assertTrue(StringUtils.hasLength(str));
    }

}
