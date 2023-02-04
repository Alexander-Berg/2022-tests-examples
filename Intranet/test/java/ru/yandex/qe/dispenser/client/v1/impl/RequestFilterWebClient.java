package ru.yandex.qe.dispenser.client.v1.impl;

import java.net.URI;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class RequestFilterWebClient extends RemoteDispenserFactory.PatchedWebClient {
    private static final Log LOG = LogFactory.getLog(RequestFilterWebClient.class);

    protected RequestFilterWebClient(final String baseAddress) {
        super(baseAddress);
    }

    @NotNull
    @Override
    protected final Message createMessage(@Nullable final Object body,
                                          @NotNull final String httpMethod,
                                          @NotNull final MultivaluedMap<String, String> headers,
                                          @NotNull final URI currentURI,
                                          @Nullable final Exchange exchange,
                                          @Nullable final Map<String, Object> invocationContext,
                                          final boolean proxy) {
        boolean doRequest = false;
        try {
            doRequest = onRequest(httpMethod, headers, currentURI, body);
        } catch (Exception e) {
            LOG.error("Can't process request!", e);
        }
        if (!doRequest) {
            throw new SkipRequestInvocationException();
        }
        return super.createMessage(body, httpMethod, headers, currentURI, exchange, invocationContext, proxy);
    }

    protected abstract boolean onRequest(@NotNull final String method,
                                         @NotNull final MultivaluedMap<String, String> headers,
                                         @NotNull final URI uri,
                                         @Nullable final Object body) throws Exception;

    public static class SkipRequestInvocationException extends RuntimeException {
    }
}
