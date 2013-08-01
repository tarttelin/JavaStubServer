package com.pyruby.stubserver;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;

public class HttpsTest extends StubServerTest {

    private static String keystorePath;
    private static final String keystorePasword = "testkeystore";
    private static final String keyPassword = "testkey";

    private static String getKeystorePath() throws IOException {
        if (keystorePath == null) {
            File keystoreFile = File.createTempFile("JavaStubServerHttpsTest", ".jks");
            OutputStream outStream = new FileOutputStream(keystoreFile);
            IOUtils.copy(HttpsTest.class.getResourceAsStream("/testkeystore.jks"), outStream);
            outStream.close();
            keystoreFile.deleteOnExit();
            keystorePath =  keystoreFile.getAbsolutePath();
        }

        return keystorePath;
    }

    @Override
    @Before
    public void setUp() throws IOException {

        System.setProperty("javax.net.ssl.trustStore", getKeystorePath());
        System.setProperty("javax.net.ssl.trustStorePassword", keystorePasword);

        baseUrl = "https://localhost:44804";
        server = new StubServer(44804, new HttpsSettings(getKeystorePath(), keystorePasword, keyPassword));
        server.start();

    }

    @Test
    public void theKeyPasswordDefaultsToTheKeystorePassword() throws IOException {
        HttpsSettings subject = new HttpsSettings("store", "password");

        assertEquals("password", subject.getKeyPassword());
    }


}
