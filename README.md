Stub server is intended to give a mockito-esque feel to a stubbed HTTP server.  This allows you to test
an application that hits external HTTP interfaces as a proper black box.  Take this example
<p>
You have a web application that hits a ReST api to retrieve customer information.  In the web app code, you have
a gateway that calls the ReST api using a real HTTP request.  In a test environment, you don't want to, or cannot
depend on the real ReST api, so you want to have a fake server that provides canned data back.  This is what
StubServer is intended to simplify for you.
<p>
Example test:<br>
  Deploy your web app with a config file that declares that the ReST api is accessible on http://localhost:21435</li>
  <code>StubServer server = new StubServer(21435); // matching port
  server.start();
  server.expect(get("/api/customer/Bob")).thenReturn(200, "application/xml","&lt;customer>&lt;name>Bob&lt;/name>&lt;/customer>");
  // can have multiple expectations. The url is actually a regex
  try {
     selenium.open("http://localhost:8080/users/?name=Bob");
     ... assertions that Bob has the right values as returned from the external ReST api

     server.verify(); // check that all expectations were called
  } finally {
     server.close();
  }
  </code>
<p>
With this approach, it feels more like a unit test, but it allows you to totally black box the system under test.

## NOTE:

- Given the plethora of cat skinning devices, I have not prescribed a mechanism to build StubServer, or a way of
downloading it's dependencies.  It was built with the following:-

jetty-6.1.26.jar
jetty-util-6.1.26.jar
servlet-api-2.5-20081211.jar
junit-4.7.jar

Later or earlier versions may or may not work.

