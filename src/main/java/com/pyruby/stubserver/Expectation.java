package com.pyruby.stubserver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;

/**
 * Created by calling {@link StubServer#expect(StubMethod)}, the returned expectation is then used to define
 * how the server should respond when the expectation is met.  For example:-
 * <ul>
 * <li>server.expect(get("some/expected/context")).thenReturn(200, "text/html", "&lt;html>&lt;body>Hello World&lt;/body>&lt;/html>");</li>
 * <li>open a browser to http://localhost:&lt;port>/some/expected/context</li>
 * <li>web page returned showing "Hello World"</li>
 * </ul>
 */
public class Expectation {
    private static final List<Header> EMPTY_HEADERS = Collections.emptyList();
    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final String NO_MIME_TYPE =null;

    private final StubMethod stubbedMethod;
    private CannedResponse cannedResponse;
    private boolean satisfied;

    Expectation(StubMethod stubbedMethod) {
        this.stubbedMethod = stubbedMethod;
    }

    /**
     * Define how the server should respond to an expected request.
     *
     * @param statusCode The response status code, e.g. 204
     */
    public void thenReturn(int statusCode) {
        thenReturn(statusCode, NO_MIME_TYPE, EMPTY_BYTES, EMPTY_HEADERS);
    }

    /**
     * Define how the server should respond to an expected request.  My current use cases don't include specific
     * handling multi-part responses, so I haven't implemented any.
     * This method is deprecated because it expects a mime type when there is no response - use thenReturn(int statusCode) instead.
     *
     * @param statusCode The response status code, e.g. 200
     * @param mimeType   The content type of the response, e.g. text/html
     */
    @Deprecated
    public void thenReturn(int statusCode, String mimeType) {
        thenReturn(statusCode, mimeType, EMPTY_BYTES, EMPTY_HEADERS);
    }

    /**
     * Define how the server should respond to an expected request.  My current use cases don't include specific
     * handling multi-part responses, so I haven't implemented any.
     *
     * @param statusCode   The response status code, e.g. 200
     * @param mimeType     The content type of the response, e.g. text/html
     * @param responseBody The body of the response, e.g. &lt;html>&lt;body>Hello World&lt;/body>&lt;/html>
     */
    public void thenReturn(int statusCode, String mimeType, String responseBody) {
        thenReturn(statusCode, mimeType, responseBody, EMPTY_HEADERS);
    }

    /**
     * Define how the server should respond to an expected request.  My current use cases don't include specific
     * handling multi-part responses, so I haven't implemented any.
     *
     * @param statusCode      The response status code, e.g. 200
     * @param mimeType        The content type of the response, e.g. text/html
     * @param responseBody    The body of the response, e.g. &lt;html>&lt;body>Hello World&lt;/body>&lt;/html>
     * @param responseHeaders An optional map of headers that should be returned in the response. May be null.
     */
    public void thenReturn(int statusCode, String mimeType, String responseBody, List<Header> responseHeaders) {
        this.cannedResponse = new CannedResponse(statusCode, mimeType, responseBody, responseHeaders);
    }

    /**
     * Define how the server should respond to an expected request.  My current use cases don't include specific
     * handling multi-part responses, so I haven't implemented any.
     *
     * @param statusCode   The response status code, e.g. 200
     * @param mimeType     The content type of the response, e.g. text/html
     * @param responseBody The body of the response
     */
    public void thenReturn(int statusCode, String mimeType, byte[] responseBody) {
        thenReturn(statusCode, mimeType, responseBody, EMPTY_HEADERS);
    }

    /**
     * Define how the server should respond to an expected request.  My current use cases don't include specific
     * handling multi-part responses, so I haven't implemented any.
     *
     * @param statusCode      The response status code, e.g. 200
     * @param mimeType        The content type of the response, e.g. text/html
     * @param responseBody    The body of the response
     * @param responseHeaders An optional map of headers that should be returned in the response. May be null.
     */
    public void thenReturn(int statusCode, String mimeType, byte[] responseBody, List<Header> responseHeaders) {
        this.cannedResponse = new CannedResponse(statusCode, mimeType, responseBody, responseHeaders);
    }

    /**
     * Proxy this request to the target server defined in {@link com.pyruby.stubserver.StubServer()#proxy}
     *
     */
    public void thenDelegate() {
        throw new IllegalStateException("Stub server does not have a proxy configured");
    }

    boolean matches(String target, HttpServletRequest httpServletRequest) {
        if (satisfied) return false;
        boolean matched = stubbedMethod.matches(target, httpServletRequest);
        if (matched) satisfied = true;
        return matched;
    }

    void verify() {
        if (!satisfied) {
            throw new AssertionError(stubbedMethod.toString());
        }
    }

    void respond(HttpServletResponse httpServletResponse) throws IOException {
        cannedResponse.respond(httpServletResponse);
    }

    static class CannedResponse {
        private final int statusCode;
        private final String mimeType;
        private final byte[] body;
        private final List<Header> headers;

        CannedResponse(final int statusCode, final String mimeType, final byte[] body, final List<Header> headers) {
            this.statusCode = statusCode;
            this.mimeType = mimeType;
            this.body = body == null ? EMPTY_BYTES : body;
            this.headers = headers != null ? headers : EMPTY_HEADERS;
        }

        CannedResponse(final int statusCode, final String mimeType, final String body, final List<Header> headers) {
            this(statusCode, mimeType, asBytes(body), headers);
        }

        void respond(final HttpServletResponse httpServletResponse) throws IOException {
            httpServletResponse.setHeader("Content-Type", mimeType);
            for (Header hdr : headers) {
                httpServletResponse.addHeader(hdr.name, hdr.value);
            }
            httpServletResponse.setStatus(statusCode);
            OutputStream writer = httpServletResponse.getOutputStream();
            writer.write(body);
            writer.flush();
            writer.close();
        }
    }

    static byte[] asBytes(String string) {
        try {
            return string != null ? string.getBytes("UTF-8") : null;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    static String asString(byte[] bytes) {
        try {
            return bytes != null ? new String(bytes, "UTF-8") : null;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
