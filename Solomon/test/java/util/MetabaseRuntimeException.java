package ru.yandex.metabase.client.util;

import ru.yandex.solomon.metabase.api.protobuf.EMetabaseStatusCode;

/**
 * @author Vladimir Gordiychuk
 */
public class MetabaseRuntimeException extends RuntimeException {
    private final EMetabaseStatusCode statusCode;

    public MetabaseRuntimeException(String message, EMetabaseStatusCode statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public EMetabaseStatusCode getStatusCode() {
        return statusCode;
    }
}
