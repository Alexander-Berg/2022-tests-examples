package ru.yandex.intranet.d.grpc;

import java.util.concurrent.Executor;

import io.grpc.CallCredentials;
import io.grpc.Metadata;

/**
 * Adds user headers for stub GRPC auth.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
public class MockGrpcUser extends CallCredentials {

    private final String uid;
    private final Long tvmClientId;

    private MockGrpcUser(String uid, Long tvmClientId) {
        this.uid = uid;
        this.tvmClientId = tvmClientId;
    }

    public static MockGrpcUser uid(String uid) {
        return new MockGrpcUser(uid, null);
    }

    public static MockGrpcUser uidTvm(String uid, long tvmClientId) {
        return new MockGrpcUser(uid, tvmClientId);
    }

    public static MockGrpcUser tvm(long tvmClientId) {
        return new MockGrpcUser(null, tvmClientId);
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
        Metadata authHeaders = new Metadata();
        if (uid != null) {
            authHeaders.put(Metadata.Key.of("X-Ya-Uid",
                    Metadata.ASCII_STRING_MARSHALLER), uid);
        }
        if (tvmClientId != null) {
            authHeaders.put(Metadata.Key.of("X-Ya-Service-Id",
                    Metadata.ASCII_STRING_MARSHALLER), tvmClientId.toString());
        }
        applier.apply(authHeaders);
    }

    @Override
    public void thisUsesUnstableApi() {
    }

}
