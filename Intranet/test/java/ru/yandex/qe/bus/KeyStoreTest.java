package ru.yandex.qe.bus;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Enumeration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author lvovich
 */
public class KeyStoreTest {

    @Disabled
    @Test
    public void test() throws Exception {
        final KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new FileInputStream("/home/lvovich/certs/lvovich2016.pkcs12"), null);
        final Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            System.out.println("Alias: [" + aliases.nextElement() + "]");
        }
    }
}
