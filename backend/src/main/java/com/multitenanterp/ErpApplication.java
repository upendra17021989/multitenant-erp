package com.multitenanterp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Locale;

@SpringBootApplication
public class ErpApplication {
    public static void main(String[] args) {
        useWindowsTrustedRootsWhenAvailable();
        SpringApplication.run(ErpApplication.class, args);
    }

    private static void useWindowsTrustedRootsWhenAvailable() {
        boolean windows = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .startsWith("windows");
        boolean trustStoreNotConfigured = System.getProperty("javax.net.ssl.trustStore") == null
                && System.getProperty("javax.net.ssl.trustStoreType") == null;
        if (windows && trustStoreNotConfigured) {
            System.setProperty("javax.net.ssl.trustStore", "NONE");
            System.setProperty("javax.net.ssl.trustStoreType", "Windows-ROOT");
        }
    }
}