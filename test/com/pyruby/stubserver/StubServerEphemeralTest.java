package com.pyruby.stubserver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.net.*;
import java.io.*;

import static com.pyruby.stubserver.StubMethod.get;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class StubServerEphemeralTest {

    private StubServer server;

    @Before
    public void setUp() {
        server = new StubServer(0);
        server.start();
    }

    @After
    public void tearDown() {
        server.stop();
    }

    @Test
    public void testWorksWithEphemeralPort () throws UnknownHostException, IOException {
        int assignedPort = server.getLocalPort();
        Socket socket = new Socket("127.0.0.1", assignedPort);
        assertTrue(socket.isConnected());
    }
}
