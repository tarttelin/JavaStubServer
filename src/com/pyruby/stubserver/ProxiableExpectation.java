package com.pyruby.stubserver;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

class ProxiableExpectation extends Expectation {
    private final ProxyResponder proxyResponder;
    private boolean delegating;

    ProxiableExpectation(StubMethod stubbedMethod, ProxyResponder proxyResponder) {
        super(stubbedMethod);
        this.proxyResponder = proxyResponder;
    }

    @Override
    boolean matches(String target, HttpServletRequest httpServletRequest) {
        boolean matches = super.matches(target, httpServletRequest);
        if (matches && delegating) {
            return proxyResponder.matches(target, httpServletRequest);
        } else {
            return matches;
        }
    }

    @Override
    public void thenDelegate() {
        this.delegating = true;
    }

    @Override
    void respond(HttpServletResponse httpServletResponse) throws IOException {
        if (delegating) {
            proxyResponder.respond(httpServletResponse);
        } else {
            super.respond(httpServletResponse);
        }
    }
}
