package com.pyruby.stubserver;

public class LiteralHeaderExpectation implements HeaderExpectation {
    private String expectedValue;

    public LiteralHeaderExpectation(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    @Override
    public boolean matches(String actualValue) {
        return actualValue.equals(expectedValue);
    }

    @Override
    public String getExpectedValue() {
        return expectedValue;
    }
}
