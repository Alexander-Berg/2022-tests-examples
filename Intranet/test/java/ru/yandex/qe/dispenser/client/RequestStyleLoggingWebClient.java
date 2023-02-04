package ru.yandex.qe.dispenser.client;

import java.net.URI;
import java.util.StringJoiner;
import java.util.function.Consumer;

import javax.ws.rs.core.MultivaluedMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ru.yandex.qe.dispenser.api.util.SerializationUtils;

public final class RequestStyleLoggingWebClient extends LoggingWebClient {
    public RequestStyleLoggingWebClient(@NotNull final String baseAddress,
                                        @NotNull final Consumer<String> writer,
                                        final boolean fake) {
        super(baseAddress, writer, fake);
    }

    @Override
    public boolean onRequest(@NotNull final String method,
                             @NotNull final MultivaluedMap<String, String> headers,
                             @NotNull final URI uri,
                             @Nullable final Object body) throws Exception {
        final boolean result = super.onRequest(method, headers, uri, body);

        final String pathUrl = uri.getPath() + "?" + uri.getQuery();
        final StringJoiner headersString = new StringJoiner("\r\n");
        headers.forEach((name, values) -> {
            values.forEach(value -> headersString.add(name + ": " + value));
        });
        final String tag = uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);
        final String bodyString = body != null ? SerializationUtils.writeValueAsString(body) : null;

        final String ammo = makeAmmo(method, pathUrl, headersString.toString(), tag, bodyString);

        writer.accept(ammo);
        return result;
    }

    @NotNull
    private String makeAmmo(@NotNull final String method,
                            @NotNull final String url,
                            @NotNull final String headers,
                            @NotNull final String tag,
                            @Nullable final String body) {
        final String req;
        if (body == null) {
            final String template = "%s %s HTTP/1.1\r\n" +
                    "%s\r\n" +
                    "\r\n";
            req = String.format(template, method, url, headers);
        } else {
            final String template = "%s %s HTTP/1.1\r\n" +
                    "%s\r\n" +
                    "Content-Length: %d\r\n" +
                    "\r\n" +
                    "%s\r\n";
            req = String.format(template, method, url, headers, body.getBytes().length, body);
        }
        final String ammoTemplate = "%d %s\n" +
                "%s";
        return String.format(ammoTemplate, req.getBytes().length, tag, req);
    }
}
