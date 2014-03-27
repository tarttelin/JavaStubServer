package com.pyruby.stubserver;

interface HeaderExpectation {
    boolean matches(String actualValue);

    String getExpectedValue();
}
