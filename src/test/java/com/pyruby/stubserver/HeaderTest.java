package com.pyruby.stubserver;

import org.junit.Test;

import static com.pyruby.stubserver.Header.header;
import static org.junit.Assert.assertEquals;

public class HeaderTest {
    @Test
    public void toString_shouldIncludeTheHeaderNameAndAllHeaderValues() {
        assertEquals("Name: [Value1, Value2]", header("Name", "Value1", "Value2").toString());
    }
}
