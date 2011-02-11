package com.pyruby.stubserver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by calling {@link StubServer#expect(StubMethod)}, the returned expectation is then used to define
 * how the server should respond when the expectation is met.  For example:-
 * <p>
 * <ul>
 *   <li>server.expect(get("some/expected/context")).thenReturn(200, "text/html", "&lt;html>&lt;body>Hello World&lt;/body>&lt;/html>");</li>
 *   <li>open a browser to http://localhost:&lt;port>/some/expected/context</li>
 *   <li>web page returned showing "Hello World"</li>
 * </ul>
 */
public class Expectation {
    private final StubMethod stubbedMethod;
    private CannedResponse cannedResponse;
    private boolean satisfied;

    Expectation(StubMethod stubbedMethod) {
        this.stubbedMethod = stubbedMethod;
    }

    /**
     * Define how the server should respond to an expected request.  My current use cases don't include specific
     * headers on the response, or handling multi-part or non-string based responses, so I haven't implemented any.
     *
     * @param statusCode The response status code, i.e. 200
     * @param mimeType The content type of the response, i.e. text/html
     * @param body The body of the response, i.e. &lt;html>&lt;body>Hello World&lt;/body>&lt;/html>
     */
    public void thenReturn(int statusCode, String mimeType, String body) {
        this.cannedResponse = new CannedResponse(statusCode, mimeType, body);
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
        private final String body;

        CannedResponse(final int statusCode, final String mimeType, final String body) {
            this.statusCode = statusCode;
            this.mimeType = mimeType;
            this.body = body;
        }

        void respond(final HttpServletResponse httpServletResponse) throws IOException {
            httpServletResponse.setHeader("Content-Type", mimeType);
            httpServletResponse.setStatus(statusCode);
            PrintWriter writer = httpServletResponse.getWriter();
            writer.write(body == null ? "" : body);
            writer.flush();
            writer.close();
        }
    }
}
