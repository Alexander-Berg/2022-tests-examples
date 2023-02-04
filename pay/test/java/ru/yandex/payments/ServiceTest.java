package ru.yandex.payments;

import javax.inject.Inject;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@MicronautTest
public class ServiceTest {
    @Inject
    @Client("/")
    HttpClient client;

    @Test
    @DisplayName("Verify that server response to ping")
    void testPing() {
        val response = client.toBlocking()
                .retrieve(HttpRequest.GET("/ping"));

        assertThat(response).isEqualTo("pong");
    }

    @Test
    @DisplayName("Verify that server responds to unistat")
    void testUnistat() {
        assertThatCode(() ->
                assertThatJson(client.toBlocking()
                        .retrieve(HttpRequest.GET("/unistat")))
                        .isArray()
        ).doesNotThrowAnyException();
    }
}
