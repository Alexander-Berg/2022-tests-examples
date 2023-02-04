package ru.yandex.qe.http;

import java.io.IOException;

import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author nkey
 * @since 17.03.14
 */
public class HttpClientBuilderIT {

    @Disabled
    @Test
    public void test_trust_google() throws IOException {
        new HttpClientBuilder().build().execute(new HttpGet("https://google.com"));
    }

    @Disabled
    @Test
    public void test_trust_yandex_ca() throws IOException {
        new HttpClientBuilder().build().execute(new HttpGet("https://proxy.yandex-team.ru/api/backends/by-location/"));
    }

}
