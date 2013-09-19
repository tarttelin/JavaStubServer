package com.pyruby.stubserver;

public class HttpsSettings {
    private final String keystore;
    private final String keystorePassword;
    private final String keyPassword;
    private final String[] excludedCiphers;

    public HttpsSettings(String keystore, String keystorePassword) {
        this(keystore, keystorePassword, keystorePassword);
    }

    public HttpsSettings(String keystore, String keystorePassword, String keyPassword) {
        this(keystore, keystorePassword, keyPassword, determineDefaultExcludedCiphers());
    }

    public HttpsSettings(String keystore, String keystorePassword, String keyPassword, String... excludedCiphers) {
        this.keystore = keystore;
        this.keystorePassword = keystorePassword;
        this.keyPassword = keyPassword;
        this.excludedCiphers = excludedCiphers;
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

    public String[] getExcludedCiphers() {
        return excludedCiphers;
    }

    private static String[] determineDefaultExcludedCiphers() {
        if (System.getProperty("java.version").startsWith("1.7")) {
            return new String[]{
                    "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
                    "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                    "SSL_DHE_DSS_WITH_DES_CBC_SHA"
            };
        }
        return new String[0];
    }
}
