package com.pyruby.stubserver;

public class HttpsSettings {
    private final String keystore;
    private final String keystorePassword;
    private final String keyPassword;

    public HttpsSettings(String keystore, String keystorePassword) {
        this(keystore, keystorePassword, keystorePassword);
    }

    public HttpsSettings(String keystore, String keystorePassword, String keyPassword) {
        this.keystore = keystore;
        this.keystorePassword = keystorePassword;
        this.keyPassword = keyPassword;
    }

    public String getKeystore() {
        return keystore;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }
}
