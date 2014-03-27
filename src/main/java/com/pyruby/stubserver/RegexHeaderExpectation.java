package com.pyruby.stubserver;

public class RegexHeaderExpectation implements HeaderExpectation {
    private String valueRegex;

    public RegexHeaderExpectation(String valueRegex){
        this.valueRegex = valueRegex;
    }

    @Override
    public boolean matches(String actualValue) {
        return actualValue.matches(valueRegex);
    }

    @Override
    public String getExpectedValue() {
        return valueRegex;
    }
}
