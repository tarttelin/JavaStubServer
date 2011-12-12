package com.pyruby.stubserver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.pyruby.stubserver.StubMethod.*;
import static org.junit.Assert.*;

public class StubServerTest {

    static String baseUrl = "http://localhost:44804";
    private StubServer server;

    @Before
    public void setUp() {
        server = new StubServer(44804);
        server.start();
    }

    @After
    public void tearDown() {
        server.stop();
    }

    @Test
    public void start_shouldStartAWebServerOnTheDesignatedPort() throws IOException {
        StubResponse response = makeRequest("", "GET", (String) null);

        assertNotNull(response);
    }

    @Test
    public void expect_shouldAcceptAGetRequestToAUrlAndThenReturnTheExpectedResponse() throws IOException {
        server.expect(get("/my/expected/context")).thenReturn(200, "application/json", "My expected response");

        StubResponse response = makeRequest("/my/expected/context", "GET", (String)null);

        assertEquals("My expected response", response.body);
    }

    @Test
    public void expect_shouldAcceptAGetRequestToAUrlThatMatchesARegEx() throws IOException {
        server.expect(get("/my/expected/\\d+")).thenReturn(200, "text/plain", null);

        makeRequest("/my/expected/2312", "GET", (String)null);

        server.verify();
    }

    @Test
    public void expect_shouldAcceptAPostRequestWithACollectionOfPostParameters() throws IOException {
        StubMethod expectedPath = post("/my/posted/context");
        server.expect(expectedPath).thenReturn(201, null, null);
        Map<String, String> postParams = new HashMap<String, String>();
        postParams.put("name", "arthur");
        postParams.put("age", "1074");

        StubResponse response = makeRequest("/my/posted/context", "POST", postParams);

        assertEquals(201, response.responseCode);
        assertNotNull(expectedPath.query);
        assertEquals("arthur", expectedPath.query.get("name")[0]);

    }

    @Test
    public void verify_shouldRaiseAnAssertionException_givenThereAreUnsatisfiedExpectations() throws IOException {
        server.expect(get("/some/url")).thenReturn(200, "text/html", "Got me");
        server.expect(get("/some/unsatisfied/url")).thenReturn(200, "text/html", "didn't got me");

        makeRequest("/some/url", "GET", (String) null);

        try {
            server.verify();
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("/some/unsatisfied/url"));
            return;
        }
        fail("Should not have met all expectations");
    }

    @Test
    public void expect_shouldAcceptAPostRequestWithABodyAndCaptureItForLaterAssertion() throws Exception {
        StubMethod postedRequest = post("/some/posted/json");
        server.expect(postedRequest).thenReturn(201, null, null);
        String json = "{\"key\":\"value\"}";

        StubResponse response = makeRequest("/some/posted/json", "POST", json, "application/json");

        assertEquals(201, response.responseCode);
        assertEquals(json, postedRequest.body);
    }

    @Test
    public void expect_shouldAcceptAPostRequestWithHeadersAndCaptureThemForLaterAssertion() throws Exception {
        StubMethod postedRequest = post("/some/posted/json");
        server.expect(postedRequest).thenReturn(201, null, null);
        String json = "{}";

        StubResponse response = makeRequest("/some/posted/json", "POST", json, "application/checkMe");

        assertEquals(201, response.responseCode);
        assertEquals("application/checkMe", postedRequest.headers.get("Content-Type"));
    }

    @Test
    public void expect_shouldReturnContentWithDefinedContentType() throws Exception {
        server.expect(get("/some/url")).thenReturn(200,"application/json", "[1,2,3]");

        StubResponse response = makeRequest("/some/url", "GET", (String)null);

        assertEquals("application/json", response.contentType);
    }

    @Test
    public void expect_shouldHandlePUTRequest() throws Exception {
        server.expect(put("/some/resource/id")).thenReturn(409, null, "Conflicts with resource at that location");

        makeRequest("/some/resource/id", "PUT", "Yo Mamma");

        server.verify();
    }

    @Test
    public void expect_shouldHandleDELETERequest() throws Exception {
        server.expect(delete("/some/resource/id")).thenReturn(404, null, "No matching resource found");

        makeRequest("/some/resource/id", "DELETE", "Yo Mamma");

        server.verify();
    }

    private StubResponse makeRequest(String path, String method, Map<String, String> query) throws IOException {
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

    private StubResponse makeRequest(String path, String method, String body, String contentType) throws IOException {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        if (contentType != null) {
            conn.setRequestProperty("Content-Type", contentType);
        }
        if (body != null && !method.equals("DELETE")) {
            conn.setDoOutput(true);
            PrintWriter writer = new PrintWriter(conn.getOutputStream());
            writer.write(body);
            writer.flush();
            writer.close();
        }
        StringBuilder response = new StringBuilder();
        if (conn.getResponseCode() < 300) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                response.append(line);
            }
            in.close();
        }
        return new StubResponse(conn.getResponseCode(), conn.getHeaderFields(), conn.getContentType(), response.toString());
    }

    private StubResponse makeRequest(String path, String method, String body) throws IOException {
        return makeRequest(path, method, body, null);
    }

    private static class StubResponse {
        public final int responseCode;
        public final Map<String, List<String>> headerFields;
        public final String contentType;
        public final String body;

        public StubResponse(int responseCode, Map<String, List<String>> headerFields, String contentType, String body) {
            this.responseCode = responseCode;
            this.headerFields = headerFields;
            this.contentType = contentType;
            this.body = body;
        }
    }
}
