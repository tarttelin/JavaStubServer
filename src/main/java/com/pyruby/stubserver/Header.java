package com.pyruby.stubserver;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Header {
    public final String name;
    public final String value;

    public Header (String n, String v) {
        name = n;
        value = v;
    }

    @Override
    public String toString() {
        return name + ": " + value;
    }

    public static Header header (String n, String v) {
        return new Header(n, v);
    }

    public static List<Header> headers (Header... hdr) {
        return Collections.unmodifiableList(Arrays.asList(hdr));
    }
}
