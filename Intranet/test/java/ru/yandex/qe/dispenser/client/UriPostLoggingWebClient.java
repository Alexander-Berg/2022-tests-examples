package ru.yandex.qe.dispenser.client;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.core.MultivaluedMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ru.yandex.qe.dispenser.api.util.SerializationUtils;

public final class UriPostLoggingWebClient extends LoggingWebClient {
    @NotNull
    private static final AtomicBoolean headersWritten = new AtomicBoolean();

    public UriPostLoggingWebClient(@NotNull final String address, @NotNull final OutputStreamWriter writer, final boolean fake) {
        super(address, s -> {
            try {
                writer.write(s);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, fake);
    }

    @Override
    public boolean onRequest(@NotNull final String method,
                             @NotNull final MultivaluedMap<String, String> headers,
                             @NotNull final URI uri,
                             @Nullable final Object body) throws Exception {
        final boolean result = super.onRequest(method, headers, uri, body);
        final StringJoiner sj = new StringJoiner("\n");
        if (headersWritten.compareAndSet(false, true)) {
            headers.forEach((name, values) -> {
                values.forEach(value -> sj.add("[" + name + ": " + value + "]"));
            });
        }
        final String bodyString = body != null ? SerializationUtils.writeValueAsString(body) : "";
        final String pathUrl = uri.getPath() + "?" + uri.getQuery();
        sj.add(bodyString.length() + " " + pathUrl).add(bodyString);
        writer.accept(sj.toString() + "\n");
        return result;
    }
}
