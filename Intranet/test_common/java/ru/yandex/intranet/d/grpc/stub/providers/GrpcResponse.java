package ru.yandex.intranet.d.grpc.stub.providers;

import java.util.Objects;
import java.util.Optional;

import io.grpc.StatusRuntimeException;

/**
 * GRPC response, either successful or not.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
public final class GrpcResponse<T> {

    private final T value;
    private final StatusRuntimeException error;

    private GrpcResponse(T value, StatusRuntimeException error) {
        this.value = value;
        this.error = error;
    }

    public static <T> GrpcResponse<T> success(T value) {
        return new GrpcResponse<>(value, null);
    }

    public static <T> GrpcResponse<T> failure(StatusRuntimeException error) {
        return new GrpcResponse<>(null, error);
    }

    public Optional<T> getValue() {
        return Optional.ofNullable(value);
    }

    public Optional<StatusRuntimeException> getError() {
        return Optional.ofNullable(error);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GrpcResponse<?> that = (GrpcResponse<?>) o;
        return Objects.equals(value, that.value) &&
                Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, error);
    }

    @Override
    public String toString() {
        return "GrpcResponse{" +
                "value=" + value +
                ", error=" + error +
                '}';
    }

}
