package ru.yandex.intranet.d.utils;

import java.util.Optional;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;

import ru.yandex.intranet.d.backend.service.proto.ErrorDetails;

/**
 * Error response utils.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
public final class ErrorsHelper {

    private ErrorsHelper() {
    }

    public static Optional<ErrorDetails> extractErrorDetails(StatusRuntimeException e) {
        Status statusProto = StatusProto
                .fromStatusAndTrailers(e.getStatus(), e.getTrailers());
        if (statusProto.getDetailsCount() == 0) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(statusProto.getDetails(0).unpack(ErrorDetails.class));
        } catch (InvalidProtocolBufferException ex) {
            return Optional.empty();
        }
    }

}
