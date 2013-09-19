package com.pyruby.stubserver;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Header {
    public final String name;
    public final List<String> values;

    public static Header header(String name, String... values) {
        return new Header(name, Collections.unmodifiableList(Arrays.asList(values)));
    }

    public static Header header(String name, List<String> values) {
        return new Header(name, values);
    }

    public static List<Header> headers(Header... hdr) {
        return Collections.unmodifiableList(Arrays.asList(hdr));
    }

    private Header(String name, List<String> values) {
        this.name = name;
        this.values = values;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", name, values);
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Header header = (Header) o;

        if (!name.equals(header.name)) {
            return false;
        }

        if (!values.equals(header.values)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + values.hashCode();
        return result;
    }


}
