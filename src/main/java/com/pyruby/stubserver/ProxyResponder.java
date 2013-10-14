package com.pyruby.stubserver;

import org.mortbay.jetty.Request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.pyruby.stubserver.Header.header;

class ProxyResponder extends StubMethod {

    private final String baseUrl;
    private Expectation.CannedResponse response;

    ProxyResponder(String baseUrl) {
        super("any", "any");
        this.baseUrl = baseUrl;
    }

    void respond(final HttpServletResponse httpServletResponse) throws IOException {
        response.respond(httpServletResponse);
    }

    boolean matches(HttpServletRequest request) {
        try {
            Request jettyRequest = (Request) request;
            forwardRequest(jettyRequest.getUri().getPathAndParam(), request.getMethod(), this.copyBytes(request.getInputStream()),
                copyTheRequestHeaders(request));
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private void forwardRequest(String path, String method, byte[] requestBody,
                                Map<String, Header> headers) throws IOException {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        if (headers != null && !headers.isEmpty()) {
            for (Header header : headers.values()) {
                for (String headerValue : header.values) {
                    conn.setRequestProperty(header.name, headerValue);
                }
            }
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
        List<Header> headerList = new ArrayList<Header>();
        for (Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
            if (entry.getKey() != null) {
                headerList.add(header(entry.getKey(), entry.getValue().get(0)));
            }
        }
        response = new Expectation.CannedResponse(conn.getResponseCode(), conn.getContentType(), bytes, headerList);
    }

}
