package com.pyruby.stubserver;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

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
 *   Deploy your web app with a config file that declares that the ReST api is accessible on http://localhost:21435</li>
 *   <pre>
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
    private Server server;
    private List<Expectation> expectations = new LinkedList<Expectation>();
    private Expectation.CannedResponse nullResponse = new Expectation.CannedResponse(200, "text/html", "No expectation matched", null);

    /**
     * It's usually best to use ports that are above 1024.  If the port is already bound by another process, the server
     * won't start.
     * @param port the port that the stub server should bind to.  This should match the port your system under test is
     * configured to call
     */
    public StubServer(int port) {
        this.port = port;
    }

    /**
     * Starts a web server listening on the port supplied in the constructor
     */
    public void start() {
        server = new Server(port);
        server.setHandler(new StubHandler());
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        Expectation expectation = new Expectation(stubbedMethod);
        expectations.add(expectation);
        return expectation;
    }

    /**
     * Stops the {@link StubServer}.  This should be in either a unit test tearDown, or a finally block.
     */
    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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

    private class StubHandler extends AbstractHandler {
        public void handle(String target, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, int dispatch) throws IOException, ServletException {
            for (Expectation expected : expectations) {
                if (expected.matches(target, httpServletRequest)) {
                    expected.respond(httpServletResponse);
                    return;
                }
            }
            nullResponse.respond(httpServletResponse);
        }
    }

}
