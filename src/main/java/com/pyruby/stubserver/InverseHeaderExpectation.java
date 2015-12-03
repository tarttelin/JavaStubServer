package com.pyruby.stubserver;

public class InverseHeaderExpectation implements HeaderExpectation {
    private final HeaderExpectation expectation;

    public InverseHeaderExpectation(HeaderExpectation expectation) {
        this.expectation = expectation;
    }

    @Override
    public boolean matches(String actualValue) {
        return !expectation.matches(actualValue);
    }

    @Override
    public String getExpectedValue() {
        return "not " + expectation.getExpectedValue();
    }
}
