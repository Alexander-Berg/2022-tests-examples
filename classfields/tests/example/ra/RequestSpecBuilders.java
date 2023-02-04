package ru.auto.tests.example.ra;

import io.restassured.builder.RequestSpecBuilder;

import java.util.function.Consumer;

import static com.google.common.base.Functions.identity;

public class RequestSpecBuilders {

    private RequestSpecBuilders() {
    }

    public static Consumer<RequestSpecBuilder> defaultSpec() {
        return req -> identity();
    }
}
