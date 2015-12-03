package com.pyruby.stubserver;

import org.mortbay.jetty.HttpURI;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.pyruby.stubserver.Header.header;

/**
 * A factory class for constructing the expectation for the {@link StubServer#expect} method. Only supports
 * the basic HEAD/GET/POST/PUT/DELETE methods.
 * <p/>
 * After verification, if the request matched the expectation, the instances of this class will have captured
 * the request headers, the query parameters and the request body.
 */
public class StubMethod {
    private final HttpURI url;
    private final String method;
    private final CountDownLatch latch = new CountDownLatch(1);

    private final Map<String, HeaderExpectation> headerExpectations = new HashMap<String, HeaderExpectation>();

    /**
     * The headers received in the matched request.
     */
    public Map<String, Header> requestHeaders;
    /**
     * The query parameters in the matched request
     */
    public Map<String, String[]> query;
    /**
     * The content of the matched request's body.
     */
    public byte[] body;

    protected StubMethod(String method, String url) {
        this.method = method;
        this.url = new HttpURI(url);
    }

    /**
     * Expect a HEAD request for the supplied url.
     *
     * @param url A regular expression string used to test if the request url matches the expected url.
     * @return stubMethod An instance of StubMethod.  When matched, it retains information from the request that
     *         can be asserted against.
     */
    public static StubMethod head(String url) {
        return new StubMethod("HEAD", url);
    }

    /**
     * Expect a GET request for the supplied url.
     *
     * @param url A regular expression string used to test if the request url matches the expected url.
     * @return stubMethod An instance of StubMethod.  When matched, it retains information from the request that
     *         can be asserted against.
     */
    public static StubMethod get(String url) {
        return new StubMethod("GET", url);
    }

    /**
     * Expect a POST request for the supplied url.
     *
     * @param url A regular expression string used to test if the request url matches the expected url.
     * @return stubMethod An instance of StubMethod.  When matched, it retains information from the request that
     *         can be asserted against.
     */
    public static StubMethod post(String url) {
        return new StubMethod("POST", url);
    }

    /**
     * Expect a PUT request for the supplied url.
     *
     * @param url A regular expression string used to test if the request url matches the expected url.
     * @return stubMethod An instance of StubMethod.  When matched, it retains information from the request that
     *         can be asserted against.
     */
    public static StubMethod put(String url) {
        return new StubMethod("PUT", url);
    }

    /**
     * Expect a DELETE request for the supplied url.
     *
     * @param url A regular expression string used to test if the request url matches the expected url.
     * @return stubMethod An instance of StubMethod.  When matched, it retains information from the request that
     *         can be asserted against.
     */
    public static StubMethod delete(String url) {
        return new StubMethod("DELETE", url);
    }

    /**
     * Expect a TRACE request for the supplied url.
     *
     * @param url A regular expression string used to test if the request url matches the expected url.
     * @return stubMethod An instance of StubMethod.  When matched, it retains information from the request that
     *         can be asserted against.
     */
    public static StubMethod trace(String url) {
        return new StubMethod("TRACE", url);
    }

    /**
     * Expect an OPTIONS request for the supplied url.
     *
     * @param url A regular expression string used to test if the request url matches the expected url.
     * @return stubMethod An instance of StubMethod.  When matched, it retains information from the request that
     *         can be asserted against.
     */
    public static StubMethod options(String url) {
        return new StubMethod("OPTIONS", url);
    }

    /**
     * Sets an expectation that requests will only be matched if they have a request header that matches
     * a specified expression.
     *
     * @param key        the name of the header, e.g. "Content-Type". Note that this is canse-sensitive, although
     *                   HTTP headers are generally not actually case-sensitive.
     * @param valueRegex the pattern to be matched.
     * @return this
     */
    public StubMethod ifHeader(String key, String valueRegex) {
        headerExpectations.put(key, new RegexHeaderExpectation(valueRegex));
        return this;
    }

    /**
     * Sets an expectation that requests will only be matched if they have a request header that matches
     * a specified expression.
     *
     * @param key        the name of the header, e.g. "Content-Type". Note that this is case-sensitive, although
     *                   HTTP headers are generally not actually case-sensitive.
     * @param value the exact value to be matched.
     * @return this
     */
    public StubMethod ifExactHeader(String key, String value) {
        headerExpectations.put(key, new LiteralHeaderExpectation(value));
        return this;
    }

    /**
     * Sets an expectation that requests will not be matched if they have a request header that matches
     * a specified expression. To meet the expectation, the header must not match.
     *
     * @param key        the name of the header, e.g. "Content-Type". Note that this is canse-sensitive, although
     *                   HTTP headers are generally not actually case-sensitive.
     * @param valueRegex the pattern to be matched. Note that ".*" is useful for testing that a given header is
     *                   actually absent.
     * @return this
     */
    public StubMethod ifNotHeader(String key, String valueRegex) {
        headerExpectations.put(key, new InverseHeaderExpectation(new RegexHeaderExpectation(valueRegex)));
        return this;
    }

    /**
     * Sets an expectation that requests will not be matched if they have a request header that matches
     * a specified expression. To meet the expectation, the header must not match.
     *
     * @param key        the name of the header, e.g. "Content-Type". Note that this is case-sensitive, although
     *                   HTTP headers are generally not actually case-sensitive.
     * @param value the exact value to be matched.
     * @return this
     */
    public StubMethod ifNotExactHeader(String key, String value) {
        headerExpectations.put(key, new InverseHeaderExpectation(new LiteralHeaderExpectation(value)));
        return this;
    }

    /**
     * Sets an expectation that requests will only be matched if they have a content-type that matches
     * a specified expression. This is a special case of {@link #ifHeader(String, String)}.
     *
     * @param valueRegex the pattern to be matched.
     * @return this
     */
    public StubMethod ifContentType(String valueRegex) {
        headerExpectations.put("Content-Type", new RegexHeaderExpectation(valueRegex));
        return this;
    }

    /**
     * Sets an expectation that requests will only be matched if they have a content-type that matches
     * a specified expression. This is a special case of {@link #ifHeader(String, String)}.
     *
     * @param value the exact value to be matched.
     * @return this
     */
    public StubMethod ifExactContentType(String value) {
        headerExpectations.put("Content-Type", new LiteralHeaderExpectation(value));
        return this;
    }

    public String bodyString() {
        return Expectation.asString(body);
    }

    @SuppressWarnings({"unchecked"})
    boolean matches(HttpServletRequest httpServletRequest) {
        boolean matchFound = checkForRequestMatch(httpServletRequest);

        if (matchFound) {
            Map<String, Header> hdrs = copyTheRequestHeaders(httpServletRequest);

            for (String key : headerExpectations.keySet()) {
                HeaderExpectation exp = headerExpectations.get(key);
                Header act = hdrs.get(key);
                if (act == null || !exp.matches(act.values.get(0))) {
                    return false;
                }
            }

            requestHeaders = hdrs;
            query = httpServletRequest.getParameterMap();

            try {
                copyTheBody(httpServletRequest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            latch.countDown();
        }

        return matchFound;
    }

    protected Map<String, Header> copyTheRequestHeaders(HttpServletRequest httpServletRequest) {
        Enumeration headerNames = httpServletRequest.getHeaderNames();
        Map<String, Header> headers = new LinkedHashMap<String, Header>();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            Enumeration headerValues = httpServletRequest.getHeaders(headerName);
            List<String> headerStringValues = new ArrayList<String>();
            while(headerValues.hasMoreElements()) {
                headerStringValues.add((String) headerValues.nextElement());
            }
            headers.put(headerName, header(headerName, headerStringValues));
        }
        return headers;
    }

    private void copyTheBody(HttpServletRequest httpServletRequest) throws IOException {
        BufferedInputStream in = new BufferedInputStream(httpServletRequest.getInputStream());
        body = copyBytes(in);
        in.close();
    }

    protected byte[] copyBytes(InputStream input) throws IOException {
        final int bufferSize = 1024 * 16;
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream(bufferSize);
        final byte[] buffer = new byte[bufferSize];
        int n = input.read(buffer);
        while (n >= 0) {
            outStream.write(buffer, 0, n);
            n = input.read(buffer);
        }
        return outStream.toByteArray();
    }

    private boolean checkForRequestMatch(HttpServletRequest httpServletRequest) {
        boolean requestPathMatches = httpServletRequest.getRequestURI().matches(url.getPathAndParam());
        boolean methodMatches = httpServletRequest.getMethod().equals(this.method);
        boolean queryStringMatches = url.getQuery() == null || url.getQuery().equals(httpServletRequest.getQueryString());

        return requestPathMatches && methodMatches && queryStringMatches;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(method);
        b.append(' ').append(url);
        for (String key : headerExpectations.keySet()) {
            HeaderExpectation exp = headerExpectations.get(key);
            b.append(" where ").append(key).append(" must ").append(exp.getExpectedValue());
        }
        return b.toString();
    }

    public void waitOnCall() throws InterruptedException {
        latch.await();
    }

    public void waitOnCall(int period, TimeUnit unit) throws InterruptedException {
        latch.await(period, unit);
    }
}
