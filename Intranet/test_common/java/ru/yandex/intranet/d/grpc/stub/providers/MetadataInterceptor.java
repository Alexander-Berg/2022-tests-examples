package ru.yandex.intranet.d.grpc.stub.providers;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * GRPC metadata reader interceptor.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
public class MetadataInterceptor implements ServerInterceptor {

    public static final Context.Key<Metadata> METADATA_KEY = Context.key("TestMetadata");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        Context context = Context.current().withValue(METADATA_KEY, headers);
        return Contexts.interceptCall(context, call, headers, next);
    }

}
