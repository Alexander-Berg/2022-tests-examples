package ru.yandex.qe.http.certificates;

import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nkey
 * @since 17.03.14
 */
public class TrustManagersProviderTest {

    @Test
    public void test_provide() {
        assertNotNull(TrustManagersProvider.getInstance().getTrustManagers());
        assertTrue(TrustManagersProvider.getInstance().getTrustManagers().length > 0);
        assertTrue(Iterables
                .filter(Arrays.asList(TrustManagersProvider.getInstance().getTrustManagers()), X509TrustManager.class)
                .iterator().hasNext());
    }

    @Test
    public void test_ssl_context() {
        SSLContext sslContext = TrustManagersProvider.getInstance().createSSLContext();
        assertNotNull(sslContext);
    }
}
