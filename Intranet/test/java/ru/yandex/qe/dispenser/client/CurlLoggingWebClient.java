package ru.yandex.qe.dispenser.client;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.StringJoiner;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.client.ClientState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ru.yandex.qe.dispenser.api.util.SerializationUtils;

public final class CurlLoggingWebClient extends LoggingWebClient {

    private final OutputStreamWriter out;

    public CurlLoggingWebClient(final String baseAddress, final OutputStreamWriter out, final boolean fake) {
        super(baseAddress, s -> {
            try {
                out.write(s);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, fake);
        this.out = out;
    }

    @Override
    public boolean onRequest(@NotNull final String method,
                             @NotNull final MultivaluedMap<String, String> headers,
                             @NotNull final URI uri,
                             @Nullable final Object body) {
        if (body instanceof GenericEntity) {
            System.err.println("unexpected body type");
            System.exit(2);
        }

        final boolean result = onRequest(method, headers, uri, body);
//
        try {
            final StringJoiner sj = new StringJoiner(" ");
            sj.add("curl")
                    .add("-X")
                    .add(method);
            headers.entrySet().forEach(e -> {
                final String headerName = e.getKey();
                e.getValue().forEach(val -> {
                    sj.add("-H").add("\"" + headerName + ":").add(val + "\"");
                });
            });
            if (body != null) {
                sj.add("-d")
                        .add("'" + SerializationUtils.writeValueAsString(body) + "'");
            }
            final ClientState state = this.getState();
            sj.add("'" + state.getCurrentBuilder().build().toASCIIString() + "'");

            writer.accept(sj.toString() + "\n");
        } catch (Exception ignore) {
        }

        return result;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (out != null) {
            out.close();
        }
    }
}
