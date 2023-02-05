package com.yandex.frankenstein.settings;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlComponentsTest {

    private static final String PROTOCOL = "protocol";
    private static final String HOST = "42.100.42.100";
    private static final int PORT = 4242;

    private final UrlComponents mUrlComponents = new UrlComponents(PROTOCOL, HOST, PORT);

    @Test
    public void testProtocol() {
        assertThat(mUrlComponents.protocol).isEqualTo(PROTOCOL);
    }

    @Test
    public void testHost() {
        assertThat(mUrlComponents.host).isEqualTo(HOST);
    }

    @Test
    public void testPort() {
        assertThat(mUrlComponents.port).isEqualTo(PORT);
    }
}
