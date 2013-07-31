package com.pyruby.stubserver;

import javax.servlet.http.HttpServletRequest;

public class Stub extends Expectation {
    public Stub(StubMethod stubMethod) {
        super(stubMethod);
    }

    @Override
    boolean matches(String target, HttpServletRequest httpServletRequest) {
        return stubMethodMatches(target, httpServletRequest);
    }

    @Override
    void verify() {
        // Deliberately left blank
    }
}
