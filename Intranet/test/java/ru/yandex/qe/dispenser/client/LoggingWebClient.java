package ru.yandex.qe.dispenser.client;

import java.net.URI;
import java.util.function.Consumer;

import javax.ws.rs.core.MultivaluedMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ru.yandex.qe.dispenser.client.v1.impl.RequestFilterWebClient;

public abstract class LoggingWebClient extends RequestFilterWebClient {
    @NotNull
    protected final Consumer<String> writer;
    private final boolean fake;

    protected LoggingWebClient(@NotNull final String baseAddress, @NotNull final Consumer<String> writer, final boolean fake) {
        super(baseAddress);
        this.writer = writer;
        this.fake = fake;
    }

    @Override
    public boolean onRequest(@NotNull final String method,
                             @NotNull final MultivaluedMap<String, String> headers,
                             @NotNull final URI uri,
                             @Nullable final Object body) throws Exception {
        headers.putSingle("User-Agent", "tank");
        headers.putSingle("Host", "hostname.com");
        headers.putSingle("Accept", "*/*");
        headers.putSingle("Connection", "Close");
        return !fake;
    }
}
