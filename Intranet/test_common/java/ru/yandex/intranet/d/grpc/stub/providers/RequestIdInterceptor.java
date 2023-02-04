package ru.yandex.intranet.d.grpc.stub.providers;

import java.util.UUID;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

/**
 * GRPC request id setter interceptor.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
public class RequestIdInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> REQUEST_ID_KEY =
            Metadata.Key.of("X-Request-ID", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        return next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void sendHeaders(Metadata responseHeaders) {
                responseHeaders.put(REQUEST_ID_KEY, UUID.randomUUID().toString());
                super.sendHeaders(responseHeaders);
            }

            @Override
            public void close(Status status, Metadata trailers) {
                if (!status.isOk()) {
                    trailers.put(REQUEST_ID_KEY, UUID.randomUUID().toString());
                }
                super.close(status, trailers);
            }
        }, headers);
    }

}
