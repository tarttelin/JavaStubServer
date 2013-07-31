package com.pyruby.stubserver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.BindException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.pyruby.stubserver.Header.header;
import static com.pyruby.stubserver.Header.headers;
import static com.pyruby.stubserver.StubMethod.*;
import static org.junit.Assert.*;

public class StubServerTest {

    protected String baseUrl = "http://localhost:44804";
    protected StubServer server;

    @Before
    public void setUp() throws IOException {
        server = new StubServer(44804);
        server.start();
    }

    @After
    public void tearDown() {
        server.stop();
    }

    @Test
    public void start_shouldStartAWebServerOnTheDesignatedPort() throws IOException {
        TestStubResponse response = makeRequest("", "GET");

        assertNotNull(response);
    }

    @Test
    public void expect_shouldAcceptAGetRequestToAUrlAndThenReturnTheExpectedResponse_withHeaders() throws IOException {
        server.expect(get("/my/expected/context")).thenReturn(200, "application/json", "My expected response",
            headers(header("Set-Cookie", "c1=aaa"), header("Set-Cookie", "c2=bbb")));

        TestStubResponse response = makeRequest("/my/expected/context", "GET");

        assertEquals("My expected response", response.bodyString());

        List<String> cookies = response.headerFields.get("Set-Cookie");
        assertEquals(2, cookies.size());
        assertTrue(cookies.contains("c1=aaa"));
        assertTrue(cookies.contains("c2=bbb"));
    }

    @Test
    public void expect_shouldAcceptAHeadRequestToAUrlAndThenReturnTheExpectedStringResponse() throws IOException {
        server.expect(head("/my/expected/context")).thenReturn(200, "application/json", "");

        makeRequest("/my/expected/context", "HEAD");

        server.verify();
    }

    @Test
    public void expect_shouldAcceptATraceRequestToAUrlAndThenReturnTheExpectedResponse() throws IOException {
        server.expect(trace("/my/expected/context")).thenReturn(200, "application/json", "");

        makeRequest("/my/expected/context", "TRACE");

        server.verify();
    }

    @Test
    public void expect_shouldAcceptAnOptionsRequestToAUrlAndThenReturnTheExpectedResponse() throws IOException {
        server.expect(options("/my/expected/context")).thenReturn(200, "application/json", "");

        makeRequest("/my/expected/context", "OPTIONS");

        server.verify();
    }

    @Test
    public void expect_shouldAcceptAGetRequestToAUrlThatMatchesARegEx() throws IOException {
        server.expect(get("/my/expected/\\d+")).thenReturn(200, "text/plain", "");

        makeRequest("/my/expected/2312", "GET");

        server.verify();
    }

    @Test
    public void expect_shouldAcceptAPostRequestWithACollectionOfPostParameters() throws IOException {
        StubMethod expectedPath = post("/my/posted/context");
        server.expect(expectedPath).thenReturn(201);
        Map<String, String> postParams = new HashMap<String, String>();
        postParams.put("name", "arthur");
        postParams.put("age", "1074");

        TestStubResponse response = makeRequest("/my/posted/context", "POST", postParams);

        assertEquals(201, response.responseCode);
        assertNotNull(expectedPath.query);
        assertEquals("arthur", expectedPath.query.get("name")[0]);
    }

    @Test
    public void expect_shouldAcceptAGetRequestToAUrlAndThenReturnTheExpectedBytesResponse() throws IOException {
        byte[] res = new byte[256];
        for (int i = 0; i < 256; i++) {
            res[i] = (byte) i;
        }

        server.expect(get("/my/expected/context").ifContentType("application/octet\\-stream"))
            .thenReturn(200, "application/octet-stream", res);

        TestStubResponse response = makeRequest("/my/expected/context", "GET", "", "Content-Type", "application/octet-stream");

        server.verify();

        for (int i = 0; i < 256; i++) {
            assertEquals((byte) i, response.body[i]);
        }
    }

    @Test
    public void expect_shouldAcceptAPostRequestToAUrlAndThenReturnTheExpectedBytesResponse() throws IOException {
        byte[] req = new byte[256];
        for (int i = 0; i < 256; i++) {
            req[i] = (byte) i;
        }

        server.expect(post("/my/expected/context").ifContentType("application/octet-stream"))
            .thenReturn(204, "application/octet-stream", "");

        TestStubResponse response = makeRequest("/my/expected/context", "POST", req, "Content-Type", "application/octet-stream");

        server.verify();

        assertEquals(0, response.body.length);
    }

    @Test
    public void verify_shouldRaiseAnAssertionException_givenThereAreUnsatisfiedUrlExpectations() throws IOException {
        server.expect(get("/some/url")).thenReturn(200, "text/html", "Got me");
        server.expect(get("/some/unsatisfied/url")).thenReturn(200, "text/html", "didn't got me");

        makeRequest("/some/url", "GET");

        try {
            server.verify();
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("/some/unsatisfied/url"));
            return;
        }
        fail("Should not have met all expectations");
    }

    @Test
    public void expect_shouldAcceptAGetRequestToAUrlThatMatchesAHeader() throws IOException {
        server.expect(get("/my/expected/context").ifHeader("x-test", "foobar"))
            .thenReturn(200, "application/json", "My expected response");

        makeRequest("/my/expected/context", "GET", "", "x-test", "foobar");

        server.verify();
    }

    @Test
    public void verify_shouldRaiseAnAssertionException_givenThereAreUnsatisfiedHeaderExpectations() throws IOException {
        server.expect(get("/some/url").ifHeader("Content-Type", "foobar")).thenReturn(200, "text/html", "Got me");

        makeRequest("/some/url", "GET", "", "Content-Type", "stuff");

        try {
            server.verify();
        } catch (AssertionError e) {
            String message = e.getMessage();
            assertTrue(message, message.contains("/some/url"));
            assertTrue(message, message.contains("where Content-Type matches foobar"));
            return;
        }
        fail("Should not have met all expectations");
    }

    @Test
    public void expect_shouldAcceptAPostRequestWithABodyAndCaptureItForLaterAssertion() throws Exception {
        StubMethod postedRequest = post("/some/posted/json");
        server.expect(postedRequest).thenReturn(201);
        String json = "{\"key\":\"value\"}";

        TestStubResponse response = makeRequest("/some/posted/json", "POST", json, "application/json");

        assertEquals(201, response.responseCode);
        assertEquals(json, postedRequest.bodyString());
    }

    @Test
    public void expect_shouldAcceptAPostRequestWithHeadersAndCaptureThemForLaterAssertion() throws Exception {
        StubMethod postedRequest = post("/some/posted/json");
        server.expect(postedRequest).thenReturn(201);
        String json = "{}";

        TestStubResponse response = makeRequest("/some/posted/json", "POST", json, "application/checkMe");

        assertEquals(201, response.responseCode);
        assertEquals("application/checkMe", postedRequest.requestHeaders.get("Content-Type"));
    }

    @Test
    public void expect_shouldReturnContentWithDefinedContentType() throws Exception {
        server.expect(get("/some/url")).thenReturn(200, "application/json", "[1,2,3]");

        TestStubResponse response = makeRequest("/some/url", "GET");

        assertEquals("application/json", response.contentType);
    }

    @Test
    public void expect_shouldHandlePutRequest() throws Exception {
        server.expect(put("/some/resource/id")).thenReturn(409, null, "Conflicts with resource at that location");

        makeRequest("/some/resource/id", "PUT", "Yo Mamma");

        server.verify();
    }

    @Test
    public void expect_shouldHandleDeleteRequest() throws Exception {
        server.expect(delete("/some/resource/id")).thenReturn(404, null, "No matching resource found");

        makeRequest("/some/resource/id", "DELETE", "Yo Mamma");

        server.verify();
    }

    @Test
    public void expect_shouldHandleNoContentResponsesWithNoContentType() throws IOException {
        server.expect(post("/some/resource/id")).thenReturn(204);

        TestStubResponse response = makeRequest("/some/resource/id", "POST", "Inbound content");

        server.verify();
        assertFalse(response.headerFields.containsKey("Content-Type"));
        assertEquals(0, response.body.length);
    }

    @Test
    public void clear_shouldRemoveDanglingExpectations() throws Exception {
        server.expect(get("/some/unsatisfied/url")).thenReturn(200, "text/html", "didn't got me");
        server.clearExpectations();
        server.verify();
    }

    @Test
    public void proxy_shouldDelegateUnmatchedCallsToTheProxyServer() throws Exception {
        StubServer delegate = new StubServer(0);
        delegate.start();
        try {
            server.proxy("http://localhost:" + delegate.getLocalPort());
            delegate.expect(get("/unmatched")).thenReturn(200, "text/html", "hit delegate");
            server.expect(get("/matched")).thenReturn(200, "text/html", "hit stub");

            TestStubResponse delegateResponse = makeRequest("/unmatched", "GET");
            TestStubResponse stubResponse = makeRequest("/matched", "GET");

            server.verify();
            assertEquals("hit stub", stubResponse.bodyString());
            delegate.verify();
            assertEquals("hit delegate", delegateResponse.bodyString());
        } finally {
            delegate.stop();
        }
    }

    @Test
    public void expectAndDelegateTo_shouldMatchAndPassTheCallToTheProxy() throws IOException {
        StubServer delegate = new StubServer(0);
        delegate.start();
        try {
            server.proxy("http://localhost:" + delegate.getLocalPort());
            delegate.expect(get("/matched")).thenReturn(200, "text/html", "hit delegate");
            server.expect(get("/matched")).thenDelegate();

            TestStubResponse response = makeRequest("/matched", "GET");

            server.verify();
            delegate.verify();
            assertEquals("hit delegate", response.bodyString());
        } finally {
            delegate.stop();
        }
    }

    @Test
    public void stub_matchesMoreThanOnce() throws IOException {
        server.stub(StubMethod.get("/foo")).thenReturn(200, "text/plain", "You cannot be serious!");

        TestStubResponse result1 = makeRequest("/foo", "GET");
        TestStubResponse result2 = makeRequest("/foo", "GET");

        assertEquals(200, result1.responseCode);
        assertEquals("You cannot be serious!", result1.bodyString());
        assertEquals(200, result2.responseCode);
        assertEquals("You cannot be serious!", result2.bodyString());

        server.verify();
    }

    @Test
    public void stub_verifiesEvenIfNeverCalled() throws IOException {
        server.stub(StubMethod.get("/foo")).thenReturn(200, "text/plain", "You cannot be serious!");

        server.verify();
    }

    private TestStubResponse makeRequest(String path, String method, Map<String, String> query) throws IOException {
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> entry : query.entrySet()) {
            body.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                .append("=")
                .append(URLEncoder.encode(entry.getValue(), "UTF-8"))
                .append('&');
        }
        body.deleteCharAt(body.length() - 1);
        return makeRequest(path, method, body.toString());
    }

    private TestStubResponse makeRequest(String path, String method) throws IOException {
        return makeRequest(path, method, null, null);
    }

    private TestStubResponse makeRequest(String path, String method, String body) throws IOException {
        return makeRequest(path, method, body, null, null);
    }

    private TestStubResponse makeRequest(String path, String method, String body, String contentType) throws IOException {
        return makeRequest(path, method, body, "Content-Type", contentType);
    }

    private TestStubResponse makeRequest(String path, String method, String body,
                                         String headerKey, String headerValue) throws IOException {
        return makeRequest(path, method, Expectation.asBytes(body), headerKey, headerValue);
    }

    private TestStubResponse makeRequest(String path, String method, byte[] requestBody,
                                         String headerKey, String headerValue) throws IOException {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        if (headerKey != null && headerValue != null) {
            conn.setRequestProperty(headerKey, headerValue);
        }
        if (requestBody != null && requestBody.length > 0 && !method.equals("DELETE")) {
            conn.setDoOutput(true);
            BufferedOutputStream writer = new BufferedOutputStream(conn.getOutputStream());
            writer.write(requestBody);
            writer.flush();
            writer.close();
        }
        byte[] bytes = new byte[0];
        if (conn.getResponseCode() < 300) {
            BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
            bytes = copyBytes(in);
            in.close();
        }
        return new TestStubResponse(conn.getResponseCode(), conn.getHeaderFields(), conn.getContentType(), bytes);
    }

    private byte[] copyBytes(InputStream input) throws IOException {
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

    private static class TestStubResponse {
        public final int responseCode;
        public final Map<String, List<String>> headerFields;
        public final String contentType;
        public final byte[] body;

        public TestStubResponse(int responseCode, Map<String, List<String>> headerFields, String contentType, byte[] body) {
            this.responseCode = responseCode;
            this.headerFields = headerFields;
            this.contentType = contentType;
            this.body = body;
        }

        public String bodyString() {
            return Expectation.asString(body);
        }
    }
}
