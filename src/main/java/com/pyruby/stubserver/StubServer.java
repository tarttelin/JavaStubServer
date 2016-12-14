package com.pyruby.stubserver;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.security.SslSocketConnector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import static com.pyruby.stubserver.Header.header;

/**
 * Stub server is intended to give a mockito-esque feel to a stubbed HTTP server.  This allows you to test
 * an application that hits external HTTP interfaces as a proper black box.  Take this example
 * <p>
 * You have a web application that hits a ReST api to retrieve customer information.  In the web app code, you have
 * a gateway that calls the ReST api using a real HTTP request.  In a test environment, you don't want to, or cannot
 * depend on the real ReST api, so you want to have a fake server that provides canned data back.  This is what
 * StubServer is intended to simplify for you.
 * <p>
 * Example test:<br>
 * Deploy your web app with a config file that declares that the ReST api is accessible on http://localhost:21435</li>
 * <pre>
 *   StubServer server = new StubServer(21435); // matching port
 *   server.start();
 *   server.expect(get("/api/customer/Bob")).thenReturn(200, "application/xml","&lt;customer>&lt;name>Bob&lt;/name>&lt;/customer>");
 *   // can have multiple expectations. The url is actually a regex
 *   try {
 *      selenium.open("http://localhost:8080/users/?name=Bob");
 *      ... assertions that Bob has the right values as returned from the external ReST api
 *
 *      server.verify(); // check that all expectations were called
 *   } finally {
 *      server.close();
 *   }
 *   </pre>
 * <p>
 * With this approach, it feels more like a unit test, but it allows you to totally black box the system under test.
 */
public class StubServer {
    private int port;
    private final HttpsSettings httpsSettings;
    private Server server;
    private List<Expectation> expectations = new LinkedList<Expectation>();
    private ProxyResponder proxy;
    private int statusIfUnmatched = 200;

    /**
     * Provides a shortcut for <code>new StubServer(0)</code>, which creates an instance using any free ephemeral port.
     * The actual port chosen can be obtained using {@link #getLocalPort()}.
     */
    public StubServer() {
        this(0, null);
    }

    /**
     * It's usually best to use ports that are above 1024.  If the port is already bound by another process, the server
     * won't start.
     *
     * @param port the port that the stub server should bind to.  This should match the port your system under test is
     *             configured to call. If 0 is specified, an available ephemeral port is chosen automatically.
     */
    public StubServer(int port) {
        this(port, null);
    }

    /**
     * Use this constructor if you want to handle SSL requests. Note that it's http OR https - if you need to handle both
     * just spin up two StubServer instances!
     *
     * @param port          port to bind to. This should match the port that your system under test is configured to call.
     *                      If 0 is specified, an available ephemeral port is chosen automatically.
     * @param httpsSettings The SSL keystore settings to use.
     */
    public StubServer(int port, HttpsSettings httpsSettings) {
        this.port = port;
        this.httpsSettings = httpsSettings;
    }

    /**
     * Starts a web server listening on the port supplied in the constructor
     */
    public void start() {

        createServer();
        server.setHandler(new StubHandler());
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void createServer() {
        if (httpsSettings != null) {
            createSslServer();
        } else {
            createPlainServer();
        }
    }

    private void createPlainServer() {
        server = new Server(port);
    }

    private void createSslServer() {
        server = new Server();
        SslSocketConnector connector = new SslSocketConnector();
        connector.setPort(port);
        connector.setKeystore(httpsSettings.getKeystore());
        connector.setPassword(httpsSettings.getKeystorePassword());
        connector.setKeyPassword(httpsSettings.getKeyPassword());
        connector.setExcludeCipherSuites(httpsSettings.getExcludedCiphers());
        server.addConnector(connector);
    }

    /**
     * Specify the method, i.e. {@link StubMethod#get} including context path you expect your application
     * to call.  The resulting {@link Expectation} is used to allow you to specify what should happen when
     * a subsequent request matches the stubbedMethod.
     *
     * @param stubbedMethod {@link StubMethod}
     * @return {@link Expectation} used to specify what happens when a request matches the stubbedMethod
     */
    public Expectation expect(StubMethod stubbedMethod) {
        Expectation expectation;
        if (proxy != null) {
            expectation = new ProxiableExpectation(stubbedMethod, proxy);
        } else {
            expectation = new Expectation(stubbedMethod);
        }
        expectations.add(expectation);
        return expectation;
    }

    /**
     * Specify the method, i.e. {@link StubMethod#get} including context path you expect your application
     * to call.  The resulting {@link Expectation} is used to allow you to specify what should happen when
     * a subsequent request matches the stubbedMethod.
     * <p>
     * This is different to expect(StubMethod) in that the StubMethod will match against multiple requests
     * to the StubServer and will never fail verification.
     *
     * @param stubbedMethod {@link StubMethod}
     * @return {@link Expectation} used to specify what happens when a request matches the stubbedMethod
     */
    public Expectation stub(StubMethod stubbedMethod) {
        Stub stub = new Stub(stubbedMethod);
        expectations.add(stub);
        return stub;
    }

    /**
     * Stops the {@link StubServer}.  This should be in either a finally block, a unit test tear-down, or a class
     * tear-down. Using a class tear-down will make testing run much more quickly, but you will also need to
     * use {@link #clearExpectations()} in your unit-test tear-down after each test.
     */
    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delegate all unmatched calls to the target server.
     *
     * @param targetServer the base URL of the target server, in the format http://myserver:port
     */
    public void proxy(String targetServer) {
        proxy = new ProxyResponder(targetServer);
    }

    /**
     * Ensure the stub server expectations were all satisfied.  If an expectation was not satisfied, an AssertionError
     * is thrown.
     */
    public void verify() {
        for (Expectation expected : expectations) {
            expected.verify();
        }
    }

    /**
     * Clears all expectations, allowing this instance to be set up for another test.
     */
    public void clearExpectations() {
        expectations.clear();
    }

    /**
     * Returns the assigned port number. This is useful when the server was created with port number 0 so that the actual
     * ephemeral port number can be retrieved.
     */
    public int getLocalPort() {
        return server.getConnectors()[0].getLocalPort();
    }

    public HttpsSettings getHttpsSettings() {
        return httpsSettings;
    }

    /**
     * Gets the status code returned when no expectation is matched. By default this value is 200.
     */
    public int getStatusIfUnmatched() {
        return statusIfUnmatched;
    }

    /**
     * Sets the status code returned when no expectation is matched. By default this value is 200. If you need
     * unmatched requests to appear as errors, you should set this to 404 or any other 4xx/5xx code you need.
     */
    public void setStatusIfUnmatched(int statusIfUnmatched) {
        this.statusIfUnmatched = statusIfUnmatched;
    }

    private class StubHandler extends AbstractHandler {
        public void handle(String target, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, int dispatch) throws IOException, ServletException {
            for (Expectation expected : expectations) {
                if (expected.matches(httpServletRequest)) {
                    expected.respond(httpServletResponse);
                    return;
                }
            }
            if (proxy != null) {
                proxy.matches(httpServletRequest);
                proxy.respond(httpServletResponse);
            } else {
                reportError(httpServletRequest, httpServletResponse);
            }
        }

        private void reportError(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
            StringBuilder message = new StringBuilder("No expectation matched for:\n");
            message.append(httpServletRequest.getMethod()).append(" ").append(httpServletRequest.getRequestURI());
            if (httpServletRequest.getQueryString() != null) {
                message.append('?').append(httpServletRequest.getQueryString());
            }
            message.append("\n");
            Enumeration headerNames = httpServletRequest.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = (String) headerNames.nextElement();
                Enumeration headerValues = httpServletRequest.getHeaders(headerName);
                message.append(header(headerName, headerValues)).append("\n");
            }
            Expectation.CannedResponse nullResponse = new Expectation.CannedResponse(statusIfUnmatched, "text/plain", message.toString(), null);
            nullResponse.respond(httpServletResponse);
        }
    }

}
