package com.pyruby.stubserver;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory class for constructing the expectation for the {@link StubServer#expect} method.  Only supports
 * the basic GET/POST/PUT/DELETE methods.
 */
public class StubMethod {
    private final String method;
    private final String url;
    /**
     * The query parameters in the matched request
     */
    public Map<String, String[]> query;
    /**
     * The content of the matched requests body.
     */
    public String body;
    public Map<String, String> headers;

    private StubMethod(String method, String url) {
        this.method = method;
        this.url = url;
    }

    /**
     * Expect a GET request for the supplied url.
     * @param url A regular expression string used to test if the request url matches the expected url.
     * @return stubMethod An instance of StubMethod.  When matched, it retains information from the request that
     * can be asserted against.
     */
    public static StubMethod get(String url) {
        return new StubMethod("GET", url);
    }

    /**
     * Expect a POST request for the supplied url.
     * @param url A regular expression string used to test if the request url matches the expected url.
     * @return stubMethod An instance of StubMethod.  When matched, it retains information from the request that
     * can be asserted against.
     */
    public static StubMethod post(String url) {
        return new StubMethod("POST", url);
    }

    /**
     * Expect a PUT request for the supplied url.
     * @param url A regular expression string used to test if the request url matches the expected url.
     * @return stubMethod An instance of StubMethod.  When matched, it retains information from the request that
     * can be asserted against.
     */
    public static StubMethod put(String url) {
        return new StubMethod("PUT", url);
    }

    /**
     * Expect a DELETE request for the supplied url.
     * @param url A regular expression string used to test if the request url matches the expected url.
     * @return stubMethod An instance of StubMethod.  When matched, it retains information from the request that
     * can be asserted against.
     */
    public static StubMethod delete(String url) {
        return new StubMethod("DELETE", url);
    }

    @SuppressWarnings({"unchecked"})
    boolean matches(String target, HttpServletRequest httpServletRequest) {
        query = httpServletRequest.getParameterMap();
        boolean match = target.matches(url) && httpServletRequest.getMethod().equals(method);
        if (match) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpServletRequest.getInputStream()));
                body = "";
                for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                    body += line;
                }
                Enumeration headerNames = httpServletRequest.getHeaderNames();
                headers = new HashMap<String, String>();
                while(headerNames.hasMoreElements()) {
                    String headerName = (String)headerNames.nextElement();
                    headers.put(headerName, httpServletRequest.getHeader(headerName));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return match;
    }

    @Override
    public String toString() {
        return String.format("url: %s, method: %s", url, method);
    }
}
