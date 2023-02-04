package ru.auto.tests.cabinet.ra;

import io.restassured.builder.RequestSpecBuilder;

import java.util.function.Consumer;

public class RequestSpecBuilders {

    private RequestSpecBuilders() {
    }

    public static Consumer<RequestSpecBuilder> defaultSpec() {
        return requestSpec -> requestSpec
                .addHeader("X-Autoru-Request-ID", "1");
    }
}
